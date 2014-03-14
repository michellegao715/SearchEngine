import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

/**
 * A class designed to make fetching the results of different HTTP operations
 * easier. This particular class handles the GET operation. Demonstrates how to
 * use sockets to send and receive HTTP requests (to fetch headers and html).
 * 
 * @author Jie Gao
 * @author CS 212 Software Development
 * @author University of San Francisco
 * 
 * @see HTTPFetcher
 * @see HTMLFetcher
 * @see HeaderFetcher
 */
public class HTMLFetcher {
//	private final boolean head;
	/** The URL to fetch from a web server. */
	private static final int PORT = 80;

	/**
	 * Initializes this fetcher. Must call {@link #fetch()} to actually start
	 * the process.
	 * 
	 * @param url
	 *            - the link to fetch from the webserver
	 * @throws MalformedURLException
	 *             if unable to parse URL
	 */

	
	public static String craftRequest(URL url) {
		String host = url.getHost();
		String resource = url.getFile().isEmpty() ? "/" : url.getFile();
		// Why StringBuffer is much faster than String?
		StringBuffer output = new StringBuffer();
		output.append("GET " + resource + " HTTP/1.1\n");
		output.append("Host: " + host + "\n");
		output.append("Connection: close\n");
		output.append("\r\n");

		return output.toString();
	}
	/**
	 * Will connect to the web server and fetch the URL using the HTTP request
	 * from {@link #craftRequest()}, and then call {@link #processLine(String)}
	 * on each of the returned lines.
	 */

	public static String fetch(URL url) {
		StringBuilder result = new StringBuilder();

		try (Socket socket = new Socket(url.getHost(), PORT);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				PrintWriter writer = new PrintWriter(socket.getOutputStream());) {
			String request = craftRequest(url);

			writer.println(request);
			writer.flush();
			/** Used to determine if headers have been read. */
			boolean head = true;
			String line = reader.readLine();
			while (head) {
				// Check if we hit the blank line separating headers and HTML
				if (line.trim().isEmpty()) {
					head = false;
				}
				line = reader.readLine();
			}
			while (line != null) {
				result.append(line + "\n");
				line = reader.readLine();
			}
		}

		catch (Exception ex) {
			System.err.println(ex.toString());
		}
		return result.toString();
	}
}