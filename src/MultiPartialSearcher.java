import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is to save and write multi-query searchResult. Keep query and
 * searchResult in one class(for each query which may contain several words).
 * Parse the query file, and parse each query line into words, then
 * partialSearch the queryWords and get one query's searchResult. Save the query
 * and searchResult into LinkedHashMap.
 * 
 * @author Jie Gao
 */

public class MultiPartialSearcher {

	private static final Logger logger = LogManager.getLogger(Driver.class);

	private final LinkedHashMap<String, ArrayList<SearchResult>> sortedSearchResults; 
	private final WorkQueue workers;
	private final MultiReaderLock lock;
	private int pending;

	/**
	 * @param numOfThreads
	 *            Specify the number of threads used in the WorkQueue.
	 */
	public MultiPartialSearcher(int numOfThreads) {
		workers = new WorkQueue(numOfThreads);
		lock = new MultiReaderLock();
		sortedSearchResults = new LinkedHashMap<String, ArrayList<SearchResult>>();
	}

	/**
	 * @param queryFile
	 * @param index
	 * */
	public void partialSearch(String queryFile, InvertedIndex index) {
		String queryLine = null;
		Path path = Paths.get(queryFile);
		
		try (BufferedReader reader = Files.newBufferedReader(path,
				Charset.forName("UTF-8"))) {
			while ((queryLine = reader.readLine()) != null) {
				
				lock.lockWrite();
				sortedSearchResults.put(queryLine, null); 
				lock.unlockWrite();
				workers.execute(new PartialSearchWorkers(queryLine,index));
			}				
		} catch (IOException e) {
			logger.warn("Unable to parse {}", queryFile);
			logger.catching(Level.DEBUG, e);		
		}
		
	}

	// If there is only one query to search.
	public void partialSearchQuery(String query, InvertedIndex index) {
		sortedSearchResults.put(query, null); 
		try {
			String[] words = query.toLowerCase().replaceAll("\\W", " ")
					.replace("_", "").split("\\s");
			ArrayList<SearchResult> searchResult = index
					.partialSearch(words);
			
			sortedSearchResults.put(query, searchResult);
			
		} catch (Exception e) {
			logger.warn("Unable to read query" + query.toString());
			logger.catching(Level.DEBUG, e);
			// what is the out put of this?
		}
		
	}
	
	/**
	 * Create a PartialSearchWorker for each queryLine. And save each queryLine
	 * and its searching result in LinkedHashMap of tempSearchResults.
	 */
	private class PartialSearchWorkers implements Runnable {
		private final String queryLine;
		private final InvertedIndex index;

		public PartialSearchWorkers(String queryLine, InvertedIndex index) {
			logger.debug("Worker created for {}", queryLine);

			this.queryLine = queryLine;
			this.index = index;
			incrementPending();
		}

		@Override
		public void run() {
			try {
				String[] words = queryLine.toLowerCase().replaceAll("\\W", " ")
						.replace("_", "").split("\\s");
				ArrayList<SearchResult> searchResult = index
						.partialSearch(words);
				lock.lockWrite();
				sortedSearchResults.put(queryLine, searchResult);
				lock.unlockWrite();
			} catch (Exception e) {
				logger.warn("Unable to read query" + queryLine.toString());
				logger.catching(Level.DEBUG, e);
				// what is the out put of this?
			}
			decrementPending();
			
			// what is this {} output ?
			logger.debug("Workers finished {}" + queryLine);
		}
	}

	/**
	 * Helper method, that helps a thread wait until all of the current work is
	 * done. This is useful for resetting the counters or shutting down the work
	 * queue. Call finish when retrieve the data.(getFiles, getBytes,reset,)
	 */
	public synchronized void finish() {
		try {
			while (pending > 0) {
				logger.debug("Waiting until finished");
				// logger.info("Waiting until finished");
				this.wait();
				// synchronize this object (can not call wait with out
				// synchronization.)
			}
		} catch (InterruptedException e) {
			logger.debug("Finish interrupted", e);
		}
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
	 * Indicates that we now have one less "pending" work, and will notify any
	 * waiting threads if we no longer have any more pending work left.
	 */
	private synchronized void decrementPending() {
		pending--;
		logger.debug("Pending in drecrement is " + pending);
		if (pending <= 0) {
			System.out.printf("pending = %d", pending);
			this.notifyAll();
		}
	}

	/**
	 * Indicates that we now have additional "pending" work to wait for. We need
	 * this since we can no longer call join() on the threads. (The threads keep
	 * running forever in the background.)
	 * 
	 * We made this a synchronized method in the outer class, since locking on
	 * the "this" object within an inner class does not work.
	 */
	private synchronized void incrementPending() {
		pending++;
		logger.debug("Pendingin increment is " + pending);
	}


	/**
	 * Iterate through sortedSearchResults and write to file.
	 * 
	 * @param searchResultFile
	 */
	
	/**
	 * 
	 * @return Get the searchresult of MultiPartialSearcher.
	 */
	public LinkedHashMap<String, ArrayList<SearchResult>> getSortedSearchResult() {
		return sortedSearchResults;
	}
	
	public void saveToFile(String searchResultFile) {
		finish();
		Path path = Paths.get(searchResultFile);
		Charset UTF8 = Charset.forName("UTF-8");
         lock.lockRead();
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF8)) {
			for (String query : sortedSearchResults.keySet()) {
				writer.write(query);
				writer.newLine();
				for (SearchResult sr : sortedSearchResults.get(query)) {
					writer.write(sr.toString());
					writer.newLine();
				}
				writer.newLine();
			}
		} catch (IOException e) {
			System.err.println("Unable to write the file " + path.toString());
		}
		lock.unlockRead();
	}
	}

	

