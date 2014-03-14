/**
 * Implementation of Comparator in SearchResult, so the ArrayList<SearchResult>
 * is "automatically" sorted and ready to save to File. For reference:
 * https://github.com/cs212/demos/blob/master/Inheritance/SortDemo.java
 * 
 * @author Jie Gao
 */
public class SearchResult implements Comparable<SearchResult> {
	private int frequency;
	/** keep the initial location */
	private int location;
	/** The absolute path */
	private final String filename;

	/**
	 * SearchResult keep the frequency, initial location and filename for a
	 * query word.
	 */
	public SearchResult(String filename, int frequency, int location) {

		this.frequency = frequency;
		this.location = location;
		this.filename = filename;
	}

	/**
	 * @return Get the frequency of query.
	 */
	public int getFrequency() {
		return this.frequency;
	}

	/**
	 * @return Get the location of query.
	 */
	public int getLocation() {
		return location;
	}

	/**
	 * @return Get the filename of query.
	 */
	public String getFilename() {
		return this.filename;
	}

	/**
	 * @param newFrequency
	 *            The newFrequency is sum of old frequency plus current
	 *            frequency.
	 * @param newLocation
	 *            Update location to the smaller one.
	 */
	public void update(int newFrequency, int newLocation) {

		if (newLocation < location) {
			location = newLocation;
		}
		frequency += newFrequency;
	}

	@Override
	public String toString() {

		String formattedString = ("\"" + filename + "\"" + ", " + frequency
				+ ", " + location);
		return formattedString;
	}

	/**
	 * Define the implementation of Comparable<SearchResult> and compare by
	 * frequency by default.
	 */
	@Override
	public int compareTo(SearchResult other) {
		if (Integer.compare(other.frequency, this.frequency) == 0) {
			// compare by initial location if frequency is the same.
			if (Integer.compare(this.location, other.location) == 0)
				return String.CASE_INSENSITIVE_ORDER.compare(this.filename,
						other.filename);
			else
				return Integer.compare(this.location, other.location);
		} else
			return Integer.compare(other.frequency, this.frequency);
	}
}
