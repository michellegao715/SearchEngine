import java.util.HashMap;

/**
 * Class that handles parsing an array of arguments into flag/value pairs. A
 * flag is considered to be a non-null String that starts with a "-" dash
 * symbol. A value optionally follows a flag, and must not start with a "-" dash
 * symbol.
 * 
 * @author Jie Gao
 */
public class ArgumentParser {

	/** Stores flag and value in each pair of arguments. */
	private final HashMap<String, String> argumentMap;

	/** Parse arguments and save the pairs into argumentMap. */
	public ArgumentParser(String[] args) {
		argumentMap = new HashMap<String, String>();
		if (!(args == null)) {
			parseArgs(args);
		}
	}

	/**
	 * Parses the provided array of arguments into flag/value pairs, storing the
	 * results in an internal map.
	 * 
	 * @param arguments
	 *            Input arguments as flag's value.
	 */
	private void parseArgs(String[] arguments) {
		String key = "";
		String value = "";
		boolean haskey = false;
		boolean hasvalue = false;
		for (String word : arguments) {
			if (word.startsWith("-")) {
				key = word;
				haskey = true;

			} else {
				value = word;
				hasvalue = true;
			}
			if ((haskey == true) && (hasvalue == true)) {
				argumentMap.put(key, value);
				haskey = false;
				hasvalue = false;
			} else if ((haskey == true) && (hasvalue == false)) {
				argumentMap.put(key, "");
			}
		}
	}

	/**
	 * This method tests whether the provided String is a flag, i.e. whether the
	 * String is non-null, starts with a "-" dash, and has at least one
	 * character following the dash.
	 * 
	 * @param text
	 *            to test
	 * @return <code>true</code> if the text is non-null, start with the "-"
	 *         dash symbol, and has a flag name of at least one character
	 */
	public static boolean isFlag(String text) {
		if (text == null) {
			return false;
		} else if (text.startsWith("-")) {
			if (text == "-") {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method tests whether the provided String is a value, i.e. whether
	 * the String is non-null, non-empty, and does NOT start with a "-" dash.
	 * 
	 * @param text
	 *            to test
	 * @return <code>true</code> if the text is non-null, non-empty, and does
	 *         NOT start with the "-" dash symbol
	 */
	public static boolean isValue(String text) {
		// text should start with "-" and not be null.
		return (text != null && text.startsWith("-"));
	}

	/**
	 * This method returns the number of flags stored in the argument map.
	 */
	public int numFlags() {
		return argumentMap.size();

	}

	/**
	 * Checks whether the provided flag exists in the argument map.
	 * 
	 * @param flag
	 *            to check for
	 * @return <code>true</code> if the flag exists
	 */
	public boolean hasFlag(String flag) {
		return argumentMap.containsKey(flag);
	}

	/**
	 * Checks whether the provided flag has an associated non-null value.
	 * Returns <code>false</code> if there is no value for the flag, or if the
	 * flag does not exist.
	 * 
	 * @param flag
	 *            to check for value
	 * @return <code>true</code> if the flag has a non-null value, or
	 *         <code>false</code> if the value or flag does not exist
	 */
	public boolean hasValue(String flag) {
		return (argumentMap.containsValue(flag));
	}

	/**
	 * Returns the value associated with a flag, or <code>null</code> if the
	 * flag does not exist, or the flag does not have an associated value.
	 * 
	 * @param flag
	 *            to fetch associated value
	 * @return value of the flag if it exists, or <code>null</code>
	 */
	public String getValue(String flag) {
		return argumentMap.get(flag);
	}
}