import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class VisitedPageServlet extends LoginBaseServlet {

	protected final DatabaseConnector connector;
	private static final String INSERT =
			"INSERT INTO visited_page (user,url) " +
					"VALUES ";
	public VisitedPageServlet(DatabaseConnector connector ) {
		super();
		this.connector = connector;
	}
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		
		// Make sure the user has logged in.
		String user = getUsername(request);
		if(user == null) {
			  response.sendRedirect("/login");
		}

		String url = request.getParameter("url");
		
		try {
			Connection db = connector.getConnection();
			Statement statement = db.createStatement();
			statement.executeUpdate(INSERT + "(" + "'" +user+"'" + ", " + "'"+url+"'" + ")"+ ";");
		}

		catch (SQLException e) {
			System.err.printf(e.getMessage());
		}
		// Visit the url page.
		response.sendRedirect(response.encodeRedirectURL(url));
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		
		}
	}