import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * In this class,create an add method so that anything can add a word to the
 * index with their file name and location. Call this add() method from
 * FileParser.
 * 
 * 
 * @author Jie Gao
 */
public class InvertedIndex {
	private static final Logger logger = LogManager.getLogger(Driver.class);

	// Build a private mapOfIndex which is a TreeMap data structure.
	private final TreeMap<String, TreeMap<String, ArrayList<Integer>>> mapOfIndex;
	private final MultiReaderLock lock;

	/**
	 * Create an empty inverted index and initialize it in constructor.
	 * Construct a new tree map using natural order of its keys. to replace: new
	 * TreeMap<String, TreeMap<String, ArrayList<Integer>>>();
	 * */
	public InvertedIndex() {
		mapOfIndex = new TreeMap<>();
		lock = new MultiReaderLock();
	}

	public void clear() {
		lock.lockWrite();
		mapOfIndex.clear();
		lock.unlockWrite();
	}

	/**
	 * In this add method, checks like if(!MapOfIndex.containsKey(word)), so
	 * every time something calls add() this check is done. There are three
	 * circumstances after checking the word: Firstly, if mapOfIndex does not
	 * contain the key(word), then create a new TreeMap for that word. Secondly,
	 * if mapOfIndex contains the key(word), there are two situations: the word
	 * is in the previous text file or in a new text file in third circumstance.
	 * 
	 * @param word
	 *            Parsed words from file.
	 * @param file
	 *            File to parse.
	 * @param location
	 *            The location of each word in file.
	 */

	public void add(String word, Path file, int location) {

		String path = file.toAbsolutePath().toString();
		// Make this data structure thread safe by add lock in add method(check the "notes").
		lock.lockWrite();
		// if not contain key(word), then create a new TreeMap for the word
		if (!mapOfIndex.containsKey(word)) {
			ArrayList<Integer> arrayList = new ArrayList<Integer>();
			arrayList.add(location);
			TreeMap<String, ArrayList<Integer>> tm = new TreeMap<String, ArrayList<Integer>>();
			tm.put(path, arrayList);
			mapOfIndex.put(word, tm);
		} else if (mapOfIndex.containsKey(word)) {
			/**
			 * Two situations of containsKey: in the previous textfile, or in a
			 * new textfile. if the word is in the previous textfile,then add
			 * new founded location of word to existed arrayList.
			 */
			if (mapOfIndex.get(word).containsKey(path)) {
				ArrayList<Integer> arrayList = mapOfIndex.get(word).get(path);
				arrayList.add(location);
			} else {
				// If the word is in a new textfile: create a new arrayList to
				// keep location.
				ArrayList<Integer> arrayList = new ArrayList<>();
				arrayList.add(location);
				mapOfIndex.get(word).put(path, arrayList);
			}
		}
		lock.unlockWrite();
	}

	public StringBuffer toStringBuffer() {

		boolean isFirst = true;
		StringBuffer output = new StringBuffer();
		lock.lockRead();
		for (String word : mapOfIndex.keySet()) {
			// check if the word is the first, if so, write a new line in
			// the file.
			if (!isFirst)
				output.append("\n");
			isFirst = false;
			System.out.println(word);
			// For each word, write all text files information(each text
			// file followed by a location(ArrayList<Integer>) into file.
			for (String textFile : mapOfIndex.get(word).keySet()) {
				output.append("\"").append(textFile).append("\"");
				for (int num : mapOfIndex.get(word).get(textFile)) {
					output.append(", ").append(num);
				}
				if (mapOfIndex.get(word).keySet().iterator().hasNext()) {
					output.append("\n");
				}
			}
		}
		lock.unlockRead();
		return output;
	}

	/**
	 * This saveToFile method demonstrates writing InvertedIndex into output
	 * file.
	 * 
	 * @param filename
	 *            The file to save index into file.
	 */
	public void saveToFile(String filename) {
		Path path = Paths.get(filename);
		Charset UTF8 = Charset.forName("UTF-8");

		lock.lockRead();
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF8)) {
			boolean isFirst = true;
			for (String word : mapOfIndex.keySet()) {
				// check if the word is the first, if so, write a new line.
				if (!isFirst)
					writer.newLine();
				isFirst = false;
				writer.write(word);
				writer.newLine();
				// For each word, write all text files information(each text
				// file followed by a location(ArrayList<Integer>) into file.
				for (String textFile : mapOfIndex.get(word).keySet()) {
					writer.write("\"" + textFile + "\"");
					// writer.write("\""+"home/public/cs212/input/"+textFile.substring(50)+"\""+", ");
					for (int num : mapOfIndex.get(word).get(textFile)) {
						writer.write(", " + num);
					}
					if (mapOfIndex.get(word).keySet().iterator().hasNext())
						writer.newLine();
				}
			}
			writer.newLine();
		}

		catch (IOException e) {
			// System.err.println("Unable to write the file"+path.toString());
			logger.warn("Unable to write the file" + path.toString());
			logger.catching(Level.DEBUG, e);
		}
		lock.unlockRead();
	}

	/**
	 * @param queryWords
	 *            Split a query into queryWords.
	 * @param resultMap
	 *            Keeps a query and its searching result into HashMap.
	 * @return Sorting information(filename,frequency,location)
	 */
	public ArrayList<SearchResult> partialSearch(String[] queryWords) {
		HashMap<String, SearchResult> resultMap = new HashMap<String, SearchResult>();
		SortedMap<String, TreeMap<String, ArrayList<Integer>>> tailMap = null;
		lock.lockRead();
		for (String word : queryWords) {
			for (String key : mapOfIndex.tailMap(word).keySet()) {
				if (!key.startsWith(word)) {
					break;
				} else if (key.startsWith(word)) {
					TreeMap<String, ArrayList<Integer>> queryLocation = mapOfIndex
							.get(key);
					for (String file : queryLocation.keySet()) {
						if (!resultMap.containsKey(file)) {
							int frequency = queryLocation.get(file).size();
							int location = queryLocation.get(file).get(0);
							SearchResult result = new SearchResult(file,
									frequency, location);
							resultMap.put(file, result);
						} else {
							int frequency = queryLocation.get(file).size();
							int location = queryLocation.get(file).get(0);
							resultMap.get(file).update(frequency, location);
						}
					}
				}
			}
		}
		lock.unlockRead();

		ArrayList<SearchResult> resultList = new ArrayList<>();
		resultList.addAll(resultMap.values());
		Collections.sort(resultList);
		return resultList;
	}


	/**
	 *  Make this InvertedIndex a thread safe data structure. Add writer lock before editing the mapOfIndex
	 * @param tempIndex    Add tempIndex to original InvertedIndex(this.mapOfIndex).
	 */
	public void addAll(InvertedIndex tempIndex) {
		lock.lockWrite();

		TreeMap<String, TreeMap<String, ArrayList<Integer>>> map = tempIndex.mapOfIndex;
		for (String word : map.keySet()) {
			for (String path : map.get(word).keySet()) {
				if (mapOfIndex.containsKey(word)) {
					if (mapOfIndex.get(word).containsKey(path)) {
						ArrayList<Integer> arrayList = map.get(word).get(path);
						mapOfIndex.get(word).get(path).addAll(arrayList);
						Collections.sort(mapOfIndex.get(word).get(path));
					} else {
						this.mapOfIndex.get(word).put(path,
								map.get(word).get(path));
					}
				} else if (!mapOfIndex.containsKey(word)) {
					mapOfIndex.put(word, map.get(word));
				}
			}
		}
		lock.unlockWrite();
	}

	/** 
	 * 
	 * @param word   
	 * @param url    add the url(webpage) to inverted index.
	 * @param location
	 */
	public void add(String word, String url, int location) {
		
		// Make this data structure thread safe by add lock in add method(check the "notes").
		lock.lockWrite();
		// if not contain key(word), then create a new TreeMap for the word
		if (!mapOfIndex.containsKey(word)) {
			ArrayList<Integer> arrayList = new ArrayList<Integer>();
			arrayList.add(location);
			TreeMap<String, ArrayList<Integer>> tm = new TreeMap<String, ArrayList<Integer>>();
			tm.put(url, arrayList);
			mapOfIndex.put(word, tm);
		} else if (mapOfIndex.containsKey(word)) {
			/**
			 * Two situations of containsKey: in the previous textfile, or in a
			 * new textfile. if the word is in the previous textfile,then add
			 * new founded location of word to existed arrayList.
			 */
			if (mapOfIndex.get(word).containsKey(url)) {
				ArrayList<Integer> arrayList = mapOfIndex.get(word).get(url);
				arrayList.add(location);
			} else {
				// If the word is in a new textfile: create a new arrayList to
				// keep location.
				ArrayList<Integer> arrayList = new ArrayList<>();
				arrayList.add(location);
				mapOfIndex.get(word).put(url, arrayList);
			}
		}
		lock.unlockWrite();
	}
		
	}
