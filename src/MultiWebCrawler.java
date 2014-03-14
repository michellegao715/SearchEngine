import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Processed the first 50 webpages(saved in a master set) parsed from seed url,
 * for each webpage, add a new job to a work queue to crawl that webpage.
 * 
 * @author Jie Gao
 */
public class MultiWebCrawler {
	private static final Logger logger = LogManager.getLogger(Driver.class);

	private final WorkQueue workers;
	private final HashSet<String> links;
	private final MultiReaderLock lock;
	private int pending;
	private final InvertedIndex index;
	// for
	protected final DatabaseConnector connector;
	
	private static final String INSERT =
			"INSERT INTO page_snippet (url,snippet)"+ "VALUES ";

	/**
	 * @param numOfThreads
	 *            Specify the number of threads.
	 */
	public MultiWebCrawler(int numOfThreads, InvertedIndex index) {
		connector = null;
		workers = new WorkQueue(numOfThreads);
		links = new HashSet<String>();
		lock = new MultiReaderLock();
		this.index = index;
	}

	// Add page_snippet to database.
	public MultiWebCrawler(int numOfThreads, InvertedIndex index,
			DatabaseConnector connector) {
		this.connector = connector;
		workers = new WorkQueue(numOfThreads);
		links = new HashSet<String>();
		lock = new MultiReaderLock();
		this.index = index;
		

	}

	/**
	 * @param dir
	 *            Traverse the directory of Path.
	 * @param ext
	 *            Get files of specific extension.
	 */
	public void parseSeed(String url) {
		lock.lockWrite();
		links.add(url);
		lock.unlockWrite();
		workers.execute(new LinkParser(url));
		// Driver is only thing that calls this, and not other threads
		finish();
	}

	private class LinkParser implements Runnable {

		private final String url;
		private final int numOfLinksToParse = 10;

		public LinkParser(String url) {
			this.url = url;
			logger.debug("LinkParser created for {}", url);

			// Indicate we now have "pending" work to do. This is necessary
			// so we know when our threads are "done", since we can no longer
			// call the join() method on them.
			incrementPending();
		}

		@Override
		public void run() {
			System.out.println("New worker working on url: " + url);
			logger.debug("New worker working on url: " + url);
			HTMLFetcher htmlFetcher = null;
			ArrayList<String> parsedLinks = null;

			URL u = null;
			try {
				u = new URL(url);
			} catch (MalformedURLException e1) {
				logger.debug("Invalid url" + url);
				decrementPending();

				return;
			}

			htmlFetcher = new HTMLFetcher();
			
			try {

				String fetchedHTML = HTMLFetcher.fetch(u);
				String cleanedHtml = HTMLCleaner.cleanHTML(fetchedHTML);
				Connection db = connector.getConnection();
				Statement statement = db.createStatement();
				
//				String snippet0 = cleanedHtml.trim();
				// change to prepared statement,
				statement.executeUpdate(INSERT + "(" + "'" + url+"'" + ", " + "'"+cleanedHtml.replace("\\W", " ").substring(0,500)+"'" + ")");
				
				
				// Grab the links before clean fetchedHTML
				parsedLinks = HTMLLinkParser.listLinks(fetchedHTML);
				// Then clean html.
				
				InvertedIndex tempIndex = new InvertedIndex();
				int location = 0;

				// Parse links before adding to index, then it create new
				// workers sooner.
				String[] words = cleanedHtml.split("\\s");

				for (String word : words) {
					// Remove special characters and dash "-".
					word = word.replace("_", "").replaceAll("\\W", "").trim()
							.toLowerCase();
					if (!word.isEmpty()) {
						location++;
						tempIndex.add(word, url, location);
					}
				}
				index.addAll(tempIndex);

				// Loop through parsed links and then if have 50 links then
				// break, else convert to absolute URL without
				// fragment. Check if contained in master link set, if not add
				// to master links set and create LinkParser worker.
				// Lock for read/write of link set.
				lock.lockWrite();
				for (String s : parsedLinks) {
					if (links.size() >= numOfLinksToParse) {
						break;
					}
					// Use the URL Costructor to turn relative links to absolute
					URL base = new URL(url);
					URL absolute = new URL(base, s);
					s = absolute.toString();

					Pattern p = Pattern.compile("([^#]+)(#.*?)");
					Matcher m = p.matcher(s);
					while (m.find()) {
						s = m.group(1);
					}
					if (!links.contains(s)) {
						links.add(s);
						workers.execute(new LinkParser(s));
					}
				}
				lock.unlockWrite();

			} catch (Exception e) {
				logger.debug("Unable to fetch html");
			}
			decrementPending();
		}
	}

	/**
	 * 
	 * @return Set<String>. Get all links
	 */
	public Set<String> getLinks() {
		lock.lockRead();
		Set<String> s = Collections.unmodifiableSet(links);
		lock.unlockRead();
		logger.debug("Getting paths");
		// call "finish", make sure there is no pending work before you return
		// the variable
		return s;
	}


	/**
	 * Will shutdown the work queue after all the current pending work is
	 * finished. Necessary to prevent our code from running forever in the
	 * background.
	 */
	public void shutdown() {
		logger.debug("Shutting down");
		finish();
		workers.shutdown();
	}

	/**
	 * Reset the counter for reusing. Delete any files in Set<Path> txtFiles.
	 * 
	 */

	public void reset() {

		finish();

		lock.lockWrite();
		links.clear();
		lock.lockWrite();
		logger.debug("Clear all files.");
	}

	/**
	 * Helper method, that helps a thread wait until all of the current work is
	 * done. This is useful for resetting the counters or shutting down the work
	 * queue.
	 */
	public synchronized void finish() {

		try {
			while (pending > 0) {
				logger.debug("Waiting until finished");
				this.wait();
				// synchronize this object (can not call wait with out
				// synchronization.)
			}
		} catch (InterruptedException e) {
			logger.debug("Finish interrupted", e);
		}
	}

	/**
	 * Indicates that we now have one less "pending" work, and will notify any
	 * waiting threads if we no longer have any more pending work left.
	 */
	private synchronized void decrementPending() {

		pending--;
		logger.debug("Pending in drecrement is " + pending);
		System.out.println("Pending in drecrement is " + pending);
		// System.out.printf("pending = %d\n", pending);

		if (pending == 0) {
			logger.debug("Pending in drecrement is " + pending);
			System.out.println("Pending in drecrement is " + pending);
			this.notifyAll();
		}
	}

	/**
	 * Everytime create a new worker thread, increment pending.
	 */
	private synchronized void incrementPending() {
		pending++;
		// logger.debug("Pendingin increment is " + pending);
		System.out.println("Pendingin increment is " + pending);

	}
}
