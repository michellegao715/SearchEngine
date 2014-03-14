


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Jie Gao
 * 
 */
public class MultiReaderLock {
	private static final Logger logger = LogManager.getLogger();
	private int readers;
	private int writers;

	public MultiReaderLock() {
		readers = 0;
		writers = 0;
	}

	public synchronized void lockRead() {
		while (writers > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Interrupted from read lock!");
			}
		}
		readers++;
	}

	public synchronized void unlockRead() {
		readers--;
		if (readers <= 0) {
			notifyAll();
		}
	}

	public synchronized void lockWrite() {
		while (readers > 0 || writers > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Interrupted from read-write lock!");
			}
		}
		writers++;
	}

	public synchronized void unlockWrite() {
		writers--;
		if (writers <= 0) {
			notifyAll();
		}
	}

	public synchronized int numReaders() {
		return readers;
	}

	public synchronized int numWriters() {
		return writers;
	}
}