import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This method use multiple workers to traverse directory, and save files in
 * TreeSet<Path>. Use MultiReaderLock to allow multiple concurrent read
 * operations, but non-concurrent write and write/read operations.
 * 
 * @author Jie Gao
 */
public class MultithreadedDirectoryTraverser {
	private static final Logger logger = LogManager.getLogger(Driver.class);

	private final WorkQueue workers;
	private final TreeSet<Path> paths;
	private final MultiReaderLock lock;
	private int pending;

	/**
	 * @param numOfThreads
	 *            Specify the number of threads.
	 */
	public MultithreadedDirectoryTraverser(int numOfThreads) {

		workers = new WorkQueue(numOfThreads);
		paths = new TreeSet<Path>();
		lock = new MultiReaderLock();
	}

	/**
	 * @param dir
	 *            Traverse the directory of Path.
	 * @param ext
	 *            Get files of specific extension.
	 */
	public void traverseDirectory(Path dir, String ext) {
		workers.execute(new DirectoryWorkers(dir, ext));
		finish();	
	}

	/**
	 * Handles per-directory parsing. If a subdirectory is encountered, a new
	 * DirectoryWorkers is created to handle that subdirectory.
	 */
	private class DirectoryWorkers implements Runnable {
		// not static because : need the work queue instance member of the
		// multithreading object.
		private final Path directory;
		private final String ext;

		public DirectoryWorkers(Path directory, String ext) {

			logger.debug("Worker created for {}", directory);
			this.directory = directory;
			this.ext = ext;

			// Indicate we now have "pending" work to do. This is necessary
			// so we know when our threads are "done", since we can no longer
			// call the join() method on them.
			incrementPending();
		}

		@Override
		public void run() {

			TreeSet<Path> txtFiles = new TreeSet<Path>();
			
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
				for (Path path : stream) {
					if (Files.isDirectory(path)) {
						// Note that we now create a new runnable object and add it
						// to the work queue.for parsing subdirectory.
						workers.execute(new DirectoryWorkers(path, ext));
					} else {
						// Note that we are adding to LOCAL variables, so we
						// only lock ONCE when we are done.
						if (path.toString().toLowerCase().endsWith(ext))
							txtFiles.add(path);
					}
				}
				lock.lockWrite();
				paths.addAll(txtFiles);
				lock.unlockWrite();
				// Indicate that we no longer have "pending" work to do.
			} catch (IOException e) {
				logger.warn("Unable to parse {}", directory);
				logger.catching(Level.DEBUG, e);
				decrementPending();
			}
			decrementPending(); 
			logger.debug("Minion finished {}", directory);
		}
	}

	/**
	 * Returns all files with extension(eg."txt") found since the last reset.
	 * Make this method synchronized in the multithreaded version.
	 * 
	 * @return Set<Path>. Get the paths of all files.
	 */
	public Set<Path> getPaths() { 
		
		lock.lockRead();
		Set<Path> s = Collections.unmodifiableSet(paths);
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
	 * Reset the counter for reusing.
	 * 
	 * @throws UnsupportedOperationException
	 */
	public void reset() throws UnsupportedOperationException { 
		
		finish();
		// delete any files in Set<Path> txtFiles.
		lock.lockWrite();
		paths.clear();
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
	 * Everytime create a new worker thread, increment pending.
	 */
	private synchronized void incrementPending() {

		pending++;
		logger.debug("Pendingin increment is " + pending);
	}
}
