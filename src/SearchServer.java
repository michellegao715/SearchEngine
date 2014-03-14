import java.sql.Connection;
import java.sql.Statement;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class SearchServer {

	public static final int PORT = 8080;
	private static final String TRUNCATE = "TRUNCATE page_snippet";

	public static void main(String[] args) throws Exception {
		ArgumentParser argumentParser = new ArgumentParser(args);
		InvertedIndex mapOfIndex = new InvertedIndex();
		DatabaseConnector connector = new DatabaseConnector();
		int PORT = 0;
		String url;
		int numOfThreads;

		if (!argumentParser.hasFlag("-u")) {
			System.err.println("Please enter the right seed URL.");
			return;
		}
		url = argumentParser.getValue("-u");

		// Indicate the port the web server should use to accept socket
		// connections.
		if (argumentParser.hasFlag("-p")
				&& argumentParser.getValue("-p") != null) {
			try {
				String s = argumentParser.getValue("-p");
				PORT = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				System.err.println(e.toString());
			}
		} else {
			System.err.println("Please enter the right port.");
			return;
		}

		// Indicate the number of threads use in the work queue.
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

			// Everytime before crawling a table,clear the snippet page table in
			// db.
			try (Connection db = connector.getConnection();
					Statement statement = db.createStatement();) {
				// check if table exists in database
				statement.executeUpdate(TRUNCATE);
			}

			MultiWebCrawler w = new MultiWebCrawler(numOfThreads, mapOfIndex,
					connector);
			// String url = "http://www.usfca.edu/";
			w.parseSeed(url);
			w.shutdown();

			Server server = new Server(PORT);
			ServletHandler handler = new ServletHandler();

			SearchServlet search = new SearchServlet(mapOfIndex, connector);
			SearchHistoryServlet history = new SearchHistoryServlet(connector);
			ClearHistoryServlet clearhistory = new ClearHistoryServlet(
					connector);

			handler.addServletWithMapping(LoginUserServlet.class, "/login");
			handler.addServletWithMapping(LoginRegisterServlet.class,
					"/register");
			handler.addServletWithMapping(LoginWelcomeServlet.class, "/welcome");
			handler.addServletWithMapping(LogoutServlet.class, "/logout");

			handler.addServletWithMapping(new ServletHolder(search), "/search");
			handler.addServletWithMapping(new ServletHolder(history),
					"/searchhistory");
			handler.addServletWithMapping(new ServletHolder(clearhistory),
					"/clearhistory");
			// handler.addServletWithMapping(LoginRedirectServlet.class, "/*");

			// Change password.
			handler.addServletWithMapping(ChangePasswordServlet.class,
					"/changepassword");

			// Go to visited page.(table:visited_page(user,url))
			VisitedPageServlet visitedpage = new VisitedPageServlet(connector);
			handler.addServletWithMapping(new ServletHolder(visitedpage),
					"/visitedpage");

			
//			// TODO crawl new url
//			CrawlNewUrlServlet newSearch = new CrawlNewUrlServlet(mapOfIndex, connector);
//			handler.addServletWithMapping(new ServletHolder(newSearch), "/search");
			
			server.setHandler(handler);
			server.start();
			server.join();
		}
	}
}
