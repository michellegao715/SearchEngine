import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

// TODO Combine HTTP and HTML fetcher into a single class.
// TODO Update the @author tag!

/**
 * An abstract class designed to make fetching the results of different HTTP
 * operations easier.
 *
 * @author Sophie Engle
 * @author CS 212 Software Development
 * @author University of San Francisco
 *
 * @see HTTPFetcher
 * @see HTMLFetcher
 * @see HeaderFetcher
 */
public abstract class HTTPFetcher {
	/** Port used by socket. For web servers, should be port 80. */
	private static final int PORT = 80;

	/** The URL to fetch from a web server. */
	private final URL url;

	/**
	 * Initializes this fetcher. Must call {@link #fetch()} to actually start
	 * the process.
	 *
	 * @param url - the link to fetch from the webserver
	 * @throws MalformedURLException if unable to parse URL
	 */
	public HTTPFetcher(String url) throws MalformedURLException {
		this.url = new URL(url);
	}

	/**
	 * Returns the port being used to fetch URLs.
	 *
	 * @return port number
	 */
	public int getPort() {
		return PORT;
	}

	/**
	 * Returns the URL being used by this fetcher.
	 *
	 * @return URL
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * Crafts the HTTP request from the URL. Must be overridden.
	 *
	 * @return HTTP request
	 */
	protected abstract String craftRequest();

	/**
	 * Handles each line fetched from the web server. May be overridden.
	 *
	 * @param line - text retrieved from web server
	 */
	protected void processLine(String line) {
		//System.out.println(line);
	}

	/**
	 * Will connect to the web server and fetch the URL using the HTTP request
	 * from {@link #craftRequest()}, and then call {@link #processLine(String)}
	 * on each of the returned lines.
	 */
	public String fetch() {
			StringBuilder result = new StringBuilder();
			
			try (Socket socket = new Socket(url.getHost(), PORT);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					PrintWriter writer = new PrintWriter(socket.getOutputStream());) {
				String request = craftRequest();

				writer.println(request);
				writer.flush();
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
						result.append(line+"\n");
						line = reader.readLine();
					}
				}
				

				 catch (Exception ex) {
				ex.printStackTrace(); // TODO Stack trace, formatting
			}
			return result.toString();
		}
}