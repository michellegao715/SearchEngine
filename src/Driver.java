import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

// /home/public/cs212/test/check jgao11 3  
// /home/public/cs212/test/benchmark jgao11

/**
 * This class demonstrates main method and accepts command-line arguments -d
 * <directory> to indicate which directory(contains files) to build inverted
 * index, -q <queryFile> which contains all the queries, -i <filename> to
 * indicate which file to save the output(invertedindex-index.txt by default),
 * -r<resultFile> is the output of search results(searchresults-index-simple.txt
 * by default).
 * 
 * @author Jie Gao
 */
public class Driver {
	public static void main(String[] args) {

		ArgumentParser argumentParser = new ArgumentParser(args);
		InvertedIndex mapOfIndex = new InvertedIndex();
		String directory = "";
		String url = "";
		String queryFile = "";
		// -i<outPutFile> .
		String outPutFile = "";
		// -r<searchResultFile>.
		String searchResultFile = "";
		int numOfThreads = 5;
		/**
		 * -d <directory> is the directory to parse, and -i <filename> is the
		 * inverted index output file.
		 */
		if (!argumentParser.hasFlag("-u")) {
			return;
		}

		try {
			if (!argumentParser.hasFlag("-d")
					&& (!argumentParser.hasFlag("-u"))) {
				System.err
						.println("Please enter the right directory or right webpage.");
			} else {
				if (argumentParser.hasFlag("-d")) {
					directory = argumentParser.getValue("-d");
				} else if (argumentParser.hasFlag("-u")) {
					url = argumentParser.getValue("-u");
				}

				if (argumentParser.hasFlag("-i")) {
					if (argumentParser.getValue("-i") == "")
						outPutFile = "invertedindex.txt";
					else
						outPutFile = argumentParser.getValue("-i");
				}
			}

			/**
			 * In general, if the flags -i or -r are not provided, then not
			 * produce any output files.
			 */
			if (!argumentParser.hasFlag("-q")
					&& ((argumentParser.hasFlag("-d")) || (argumentParser
							.hasFlag("-u")))) {
				/**
				 * the -r argument is not valid without -q, so no output of
				 * searchresult file. No q, but has d, just output the inverted
				 * index.
				 */
				System.err
						.println("There is no output of searchresult file because no query files provided.");
				mapOfIndex.saveToFile("Invertedindex.txt");
			} else if (!argumentParser.hasFlag("-q")) {
				System.err.println("Please enter the filename of query file.");
			} else
				queryFile = argumentParser.getValue("-q");

			if (argumentParser.hasFlag("-r")) {
				if (argumentParser.getValue("-r") == "") {
					searchResultFile = "searchresults.txt";
				} else
					searchResultFile = argumentParser.getValue("-r");
			}

			if ((argumentParser.hasFlag("-d") || (argumentParser.hasFlag("-u")))
					&& (!argumentParser.hasFlag("-q"))
					&& (!argumentParser.hasFlag("-i"))
					&& (!argumentParser.hasFlag("-r"))) {
				outPutFile = "invertedindex.txt";
			}
		} catch (NullPointerException e) {
			System.err.println("There is no value.");
		}

		// -t indicates how many worker threads should be used by the work
		// queue.Default number of worker threads is 5.
		if (argumentParser.hasFlag("-t")
				&& argumentParser.getValue("-t") != null) {
			try {
				String s = argumentParser.getValue("-t");
				numOfThreads = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				numOfThreads = 5;
			}
			if (numOfThreads <= 0) {
				numOfThreads = 5;
			}
		}

		/**
		 * Traverse directory and save all text files in ArrayList<Path> files.
		 * Then Build a mapOfIndex to save each word parsed from files, and save
		 * the filename and location of each word,then write into outPutFile.
		 * 
		 * Invoke multithreadedDirectoryTraverser to traverse directory and put
		 * files with specific extension into Set<Path>.
		 * 
		 */
		if (argumentParser.hasFlag("-d")) {
			MultithreadedDirectoryTraverser d = new MultithreadedDirectoryTraverser(
					numOfThreads);
			Path dir = Paths.get(directory);
			String ext = "txt";
			d.traverseDirectory(dir, ext);
			Set<Path> files = d.getPaths();
			d.shutdown();

			MultiInvertedIndexBuilder m = new MultiInvertedIndexBuilder(
					numOfThreads, mapOfIndex);
			m.parseWordsFromFiles(files);
			m.shutdown();
		}

		else if (argumentParser.hasFlag("-u")) {
			MultiWebCrawler w = new MultiWebCrawler(numOfThreads, mapOfIndex);
			w.parseSeed(url);
			w.shutdown();
		}

		if (outPutFile != "")
			mapOfIndex.saveToFile(outPutFile);

		/**
		 * Parse words of queryFile and then search each word in outPutFile(from
		 * directory -d<directory>)and sort the searching result using
		 * multithreads, then build a comparator, output the results to
		 * searchResultFile.
		 * */
		MultiPartialSearcher s = new MultiPartialSearcher(numOfThreads);
		s.partialSearch(queryFile, mapOfIndex);
		s.saveToFile(searchResultFile);
		s.shutdown();

	}
}