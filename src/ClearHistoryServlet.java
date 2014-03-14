import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


@SuppressWarnings("serial")
public class ClearHistoryServlet extends LoginBaseServlet {
private static final String DELETE = "DELETE FROM search_history WHERE username =";
	
	private static Logger log = Log.getRootLogger();

	protected final DatabaseConnector connector;
	

	public ClearHistoryServlet(DatabaseConnector connector ) {
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
		
		// Make sure client log in, if not redirect to log in page.
		String user = getUsername(request);
		if(user == null) {
			  response.sendRedirect("/login");
		}
		if (user != null) {
			prepareResponse("Clear history", response); 
		}
		try {
				Connection db = connector.getConnection();
				Statement statement = db.createStatement();
				statement.executeUpdate(DELETE + "'" + user + "'" + ";");
				
			// The text used in results.getString(String) must match the
			// column names in the SELECT statement.
		} catch (SQLException e) {
			log.info(e.toString());
		}
		response.sendRedirect(response.encodeRedirectURL("/search"));
		response.setStatus(HttpServletResponse.SC_OK);

	}
}

