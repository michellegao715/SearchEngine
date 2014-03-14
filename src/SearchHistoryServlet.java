import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


@SuppressWarnings("serial")
public class SearchHistoryServlet extends LoginBaseServlet {
	protected static final String SELECT = "SELECT query FROM search_history WHERE username =";
	
	private static Logger log = Log.getRootLogger();

	protected final DatabaseConnector connector;
	

	public SearchHistoryServlet(DatabaseConnector connector ) {
		super();
		this.connector = connector;

	}

	@Override
	protected void doGet(
			HttpServletRequest request,
			HttpServletResponse response)
					throws ServletException, IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		log.info("MessageServlet ID " + this.hashCode() + " handling GET request.");

		Cookie[] cookies = request.getCookies();
		// Make sure client log in, if not redirect to log in page.
		String user = getUsername(request);
		if(user == null) {
			  response.sendRedirect("/login");
		}
		
		PrintWriter out = response.getWriter();
		if (user != null) {
			prepareResponse("Welcome", response); 
			out.println("<p>Hello " + user + "," +"These are your search history:"+"</p>");

		}
		
//		String cellFormat = "\t<td><b>%s</b></td>%n";
		String cellFormat = "\t<b>%s</b>%n";
		try (
				Connection db = connector.getConnection();
				Statement statement = db.createStatement();
				ResultSet results = statement.executeQuery(SELECT + "'"+user+"'" +";");
				) {
			// The text used in results.getString(String) must match the
			// column names in the SELECT statement.
			while (results.next()) {
				out.printf("<tr>%n");
				out.printf(cellFormat, results.getString("query"));
				out.printf("</tr>%n");
			}
		} catch (SQLException e) {
			log.info(e.toString());
		}
		
		response.setStatus(HttpServletResponse.SC_OK);

	}
}