import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class MultiInvertedIndexBuilder demonstrates reading words line-by-line from file, and
 * add each word and file and location of that word to the index at the same
 * time.
 * 
 * @author Jie Gao
 * */

public class MultiInvertedIndexBuilder {

	private static final Logger logger = LogManager.getLogger(Driver.class);
	private final WorkQueue workers;
	private int pending;
	private final InvertedIndex index;

	public MultiInvertedIndexBuilder(int numOfThreads, InvertedIndex index) {
		workers = new WorkQueue(numOfThreads);
		this.index = index;
	}

	/**
	 * Use ArrayList<Path> files from DirectoryTraverse as parseWordsFromFiles's
	 * parameter. Parse words from each file in files, and save the file and
	 * location in mapOfIndex.
	 * 
	 * @param links
	 *            Saving all files that have been traversed from directory.
	 * @param index
	 *            InvertedIndex to keep the word, path and location.
	 */
	public void parseWordsFromFiles(Set<Path> files) {
		for (Path path : files) {
			workers.execute(new FileParserWorker(path));
		}
		finish();	
	}
	
//	public void parseWordsFromFiles(Set<Path> files) {
//		for (Path path : files) {
//			workers.execute(new FileParserWorker(path));
//		}
//		finish();	
//	}

	/**
	 * Each FileParserWorker should parse a single text file.
	 */
	private class FileParserWorker implements Runnable {

		private final Charset UTF8 = Charset.forName("UTF-8");
		private final Path path;
		private String line;
		private int location = 0;

		public FileParserWorker(Path path) {
			logger.debug("Worker created for {}", path.toString());
			this.path = path;
			incrementPending();
		}

		@Override
		public void run() {
			InvertedIndex tempIndex = new InvertedIndex();
			try (BufferedReader reader = Files.newBufferedReader(path, UTF8)) {
				location = 0;
				while ((line = reader.readLine()) != null) {
					String[] words = line.split("\\s");
					for (String word : words) {
						// Remove special characters and dash "-".
						word = word.replace("_", "").replaceAll("\\W", "")
								.trim().toLowerCase();
						if (!word.isEmpty()) {
							location++;
							tempIndex.add(word, path, location);
						}
					}
				}
				index.addAll(tempIndex);
				
			} catch (NoSuchFileException e) {
				System.err.println("Couldn't find file " + path.toString());
			} catch (IOException e) {
				System.err.println("Could not read file " + path.toString());
			}
			decrementPending(); 
		}
		
	}

	private synchronized void decrementPending() { 
		pending--;
		logger.debug("Pending in drecrement is " + pending);
		if (pending <= 0) {
			System.out.printf("pending = %d", pending);
			this.notifyAll();
		}
	}

	private synchronized void incrementPending() {
		pending++;
		logger.debug("Pendingin increment is " + pending);
	}

	public void shutdown() {
		logger.debug("Shutting down");
		finish();
		workers.shutdown();
	}

	public void reset() throws UnsupportedOperationException {
		finish();
		index.clear();
		logger.debug("Clear all files.");
	}

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

}