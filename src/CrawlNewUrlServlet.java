import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



// More XSS Prevention:
// https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet

// Apache Comments:
// http://commons.apache.org/proper/commons-lang/download_lang.cgi

//Sophie's explanation:
// 1. User request /pie  (GET)
//    need to return form
// 2. on form 
//    < form method = "post" action= "/pie" >  
//    when user clicks button, browser sends a POST to /pie
// 3. server will call doPost(): process form data
// 4. server need to return html to user
//-either return in Post
//-or redirect request to doGet() : response.sendRedirect("/pie");

@SuppressWarnings("serial")
public class CrawlNewUrlServlet extends LoginBaseServlet {
	private static final String TITLE = "Messages";
	
	protected final DatabaseConnector connector;
	private static final String INSERT = "INSERT INTO search_history (username,query) "
			+ "VALUES ";

	// Delete seach_history query from (username, query)
	private static final String DELETE = "DELETE query FROM search_history (username,query) ";

	private static final String SELECT = "SELECT snippet FROM page_snippet WHERE url = ";

	private static final String SELECT_VISITED = "SELECT * FROM visited_page WHERE user = '%s' and url = '%s';";

	private final InvertedIndex mapOfIndex;

	public CrawlNewUrlServlet(InvertedIndex mapOfIndex, DatabaseConnector connector) {
		super();
		this.mapOfIndex = mapOfIndex;
		this.connector = connector;

	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		log.info("MessageServlet ID " + this.hashCode()
				+ " handling GET request.");
		// Make sure client log in, if not redirect to log in page.
		String user = getUsername(request);
		if (user == null) {
			System.out.println("search servelet: user = null!");
			response.sendRedirect("/login");
		}

		String searchquery = request.getParameter("searchquery");

		PrintWriter out = response.getWriter();
		out.printf("<html>%n");
		out.printf("<head><title>%s</title></head>%n", TITLE);
		out.printf("<body>%n");
		out.printf("<h1>Search Engine</h1>%n%n");

		printForm(request, response);
		out.printf("<p>%s</p>", searchquery);
		if (searchquery != "" && searchquery != null) {
			String[] arrayOfQuery = searchquery.split(" ");
			ArrayList<SearchResult> searchResult = this.mapOfIndex
					.partialSearch(arrayOfQuery);

			ArrayList<String> snippet = new ArrayList<String>();
			for (SearchResult s : searchResult) {
				Connection db;
				try {
					db = connector.getConnection();
					Statement statement = db.createStatement();
					ResultSet results = statement.executeQuery(SELECT + "'"
							+ s.getFilename() + "'" + ";");
					while (results.next()) {
						snippet.add(results.getString("snippet"));
					}
				} catch (SQLException e) {
					log.info(e.toString());
				}
			}

			ArrayList<Boolean> visited = new ArrayList<Boolean>();
			for (SearchResult s : searchResult) {
				Connection db;
				try {
					db = connector.getConnection();
					Statement statement = db.createStatement();

					ResultSet results = statement.executeQuery(String.format(
							SELECT_VISITED, user, s.getFilename()));

					Boolean visit = false;
					while (results.next()) {
						visit = true;
					}
					visited.add(visit);
				} catch (SQLException e) {
					log.info(e.toString());
				}
			}

			printSearchResult(request, response, searchResult, snippet, visited);

		}

		out.printf("</body>%n");
		out.printf("</html>%n");

		response.setStatus(HttpServletResponse.SC_OK);

	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		log.info("MessageServlet ID " + this.hashCode()
				+ " handling POST request.");

		// Click "Search" then do search, Click SearchHistory then print out
		// query history.
		String searchquery = request.getParameter("searchquery");

		Cookie[] cookies = request.getCookies();
		String name = null;
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("name")) {
					name = cookie.getValue();
				}
			}
		}

		try {
			Connection db = connector.getConnection();
			Statement statement = db.createStatement();

			statement.executeUpdate(INSERT + "(" + "'" + name + "'" + ", "
					+ "'" + searchquery + "'" + ")" + ";");
		}

		catch (SQLException e) {
			System.err.printf(e.getMessage());
		}
		// Avoid XSS attacks using Apache Commons StringUtils
		// Comment out if you don't have this library installed
		// username = StringEscapeUtils.escapeHtml4(username);
		// message = StringEscapeUtils.escapeHtml4(message);

		response.setStatus(HttpServletResponse.SC_OK);
		if (searchquery.equals("")) {
			response.sendRedirect(request.getServletPath());
		} else if (searchquery != "") {
			response.sendRedirect(request.getServletPath() + "?searchquery="
					+ searchquery);
		}
	}

	private static void printForm(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		PrintWriter out = response.getWriter();
		// this is a doGet and action= means to send the form to
		// request.getServletPath()(such as /pie)
		// Then when user click button, browser sends a post to /pie.
		out.printf("<form method=\"post\" action=\"%s\">%n",
				request.getServletPath());
		out.printf("<table cellspacing=\"0\" cellpadding=\"2\"%n");
		out.printf("<tr>%n");

		out.printf("\t<td nowrap>Query:</td>%n");
		out.printf("\t<td>%n");
		out.printf("\t\t<input type=\"text\" name=\"searchquery\" maxlength=\"50\" size=\"20\">%n");
		out.printf("\t</td>%n");
		out.printf("</tr>%n");
		out.printf("<tr>%n");
		out.printf("\t<td>%n");
		out.printf("</tr>%n");
		out.printf("</table>%n");
		// Do Search.
		out.printf("<p><input type=\"submit\" value=\"Search\">\n%n");
		// Show the search history.
		out.printf("</form>\n%n");

		out.printf("<form action = \"searchhistory\" method = \"get\">");
		out.printf("<button type = \"submit\"> search history </button>");
		out.printf("</form>");
		// Delete search history.
		out.printf("<form action = \"clearhistory\" method = \"get\">");
		out.printf("<button type = \"submit\"> clear history </button>");
		out.printf("</form>");
		// Do log out.
		out.printf("<form action = \"logout\" method = \"get\">");
		out.printf("<button type = \"submit\"> log out </button>");
		out.printf("</form>");
//		out.printf("\t<td nowrap>Enter a new url to crawl:</td>%n");
//		out.printf("\t\t<input type=\"text\" name=\"newcrawl\" maxlength=\"50\" size=\"20\">%n");
//		out.printf("<p><input type=\"submit\" value=\"Crawl new url\">\n%n");
//		out.printf("</form>");

	}

	private static void printSearchResult(HttpServletRequest request,
			HttpServletResponse response, ArrayList<SearchResult> searchResult,
			ArrayList<String> snippets, ArrayList<Boolean> visited)
			throws IOException {
		PrintWriter out = response.getWriter();
		out.printf("<form method=\"post\" action=\"%s\">%n",
				request.getServletPath());
		int len = searchResult.size();
		// print out linked url, frequency, and location.
		for (int i = 0; i < len; ++i) {
			SearchResult sr = searchResult.get(i);
			String snippet = snippets.get(i);
			String visit = visited.get(i) ? "[visited]" : "";
			int frequency = sr.getFrequency();
			int location = sr.getLocation();
			String url = sr.getFilename();
			out.printf("<p>" + visit + "<a href = \"visitedpage?url=" + url
					+ "\">" + "<bid>" + url + "</bid>" + "</a>, " + "<br>"
					+ "Frequency: " + frequency + ", " + "Location: "
					+ location + "</br>" + "</p>");
			
			out.printf("<p>" + "<small>" + snippet + "</small>" + "</p>");
		}
	}
}

//When trigger the logout button.
//if (request.getParameter("logout") != null) {
//	clearCookies(request, response);
//	response.sendRedirect(response.encodeRedirectURL("/login"));
//}