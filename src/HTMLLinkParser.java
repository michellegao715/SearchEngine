import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For this homework assignment, you must create a regular expression that
 * is able to parse links from HTML. Your code may assume the HTML is valid,
 * and all attributes are properly quoted and URL encoded.
 *
 * <p>
 * See the following link for details on the HTML Anchor tag:
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/a">
 * https://developer.mozilla.org/en-US/docs/Web/HTML/Element/a
 * </a>
 *
 * @author Jie Gao
 * @see HTMLLinkTester
 */
public class HTMLLinkParser {
	
	// TODO Clean up comments, formatting
	
//	 public static final String REGEX = "<[Aa]\\n*\\s*\\w[=\"\\w\"\\s]*[HhRrEeFf]\\n*\\s*=\\n*\\s*\"(.*?)\"\\n*.*?>";
	
	
	// Need dotall, global, ignore case
//	private static final String anchorTag = "(?!s)(<[ ]*a[\\w\\W]+?href[ =\\n]+?\")";
//	private static final String url = "([^\"]+)\"";
//			
//	public static final String REGEX = anchorTag + url;
//		
//		public static final int GROUP = 3;
	
	public static final String REGEX = "(?is)(<a.*?href.*?=.*?\")(.*?\\..*?/?)(\\.*?\")(.*?>)";
	// (?i) is for case insensitive, and (?s) is for change \n to single line.
    //  
// group 2 is for (www.usfca.edu)
	//group 3 if for (..\.." )	, \\ is for \,
//	group 4 is for (...>..);
	
			
	

	
	public static final int GROUP = 2; 
		
		/**
		 * Parses the provided text for HTML links. You should not need to modify
		 * this method.
		 *
		 * @param text - valid HTML code, with quoted attributes and URL encoded links
		 * @return list of links found in HTML code
		 */
		public static ArrayList<String> listLinks(String text) {
			// list to store links
			ArrayList<String> links = new ArrayList<String>();

			// compile string into regular expression
			Pattern p = Pattern.compile(REGEX);

			// match provided text against regular expression
			Matcher m = p.matcher(text);
			
//			int start = 0;
			// loop through every match found in text
			while(m.find()) {
				// add the appropriate group from regular expression to list
				links.add(m.group(GROUP));
//				start = m.end();
			}
			return links;
		}
		/**For testing if the regex works. */
		public static void main(String[] args) {
			HTMLLinkParser h = new HTMLLinkParser();
			String test = "<p><a href=\"http://www.usfca.edu\">USFCA</a> is in San Francisco.</p>";
			//System.out.println(h.listLinks(test));
		}
	
}
/**
 * The regular expression used to parse the HTML for links.
 */


