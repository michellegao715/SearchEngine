import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

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
public class OldLogInServlet extends HttpServlet {
	private static final String TITLE = "LogIn";
	private static Logger log = Log.getRootLogger();


	public OldLogInServlet() {
		super();	
	}

	@Override
	protected void doGet(
			HttpServletRequest request,
			HttpServletResponse response)
					throws ServletException, IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		log.info("MessageServlet ID " + this.hashCode() + " handling GET request.");

		PrintWriter out = response.getWriter();
		out.printf("<html>%n");
		out.printf("<head><title>%s</title></head>%n", TITLE);
		out.printf("<body>%n");

		out.printf("<h1>Log In</h1>%n%n");
		printForm(request, response);
		
		//		out.printf("<p>This request was handled by thread %s.</p>%n",
		//				Thread.currentThread().getName());
		//		
		out.printf("</body>%n");
		out.printf("</html>%n");

		response.setStatus(HttpServletResponse.SC_OK);
	}


	@Override
	protected void doPost(
			HttpServletRequest request,
			HttpServletResponse response)
					throws ServletException, IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		log.info("MessageServlet ID " + this.hashCode() + " handling POST request.");

		String searchquery = request.getParameter("searchquery");

		// Avoid XSS attacks using Apache Commons StringUtils
		// Comment out if you don't have this library installed
		//                username = StringEscapeUtils.escapeHtml4(username);
		//                message = StringEscapeUtils.escapeHtml4(message);

		response.setStatus(HttpServletResponse.SC_OK);
		if(searchquery.equals("")) {
			response.sendRedirect(request.getServletPath());
		}
		else if (searchquery != "") {
			response.sendRedirect(request.getServletPath() +"?searchquery="+searchquery);
		}
	}

	// TODO the searchQuery is the method to search and get the html. 
	//	public void searchQuery(HttpServletRequest request,
	//			HttpServletResponse response, String q)

	private static void printForm(
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		PrintWriter out = response.getWriter();
		//this is a doGet and action= means to send the form to request.getServletPath()(such as /pie)
		//Then when user click button, browser sends a post to /pie.
		out.printf("<table cellspacing=\"0\" cellpadding=\"2\"%n");
		out.printf("<tr>%n");

		out.printf("\t<td nowrap>Username:</td>%n");
		out.printf("\t<td>%n");
		out.printf("\t\t<input type=\"text\" name=\"username\" maxlength=\"70\" size=\"30\">%n");
		out.printf("<tr>%n");
		out.printf("\t<td>%n");
		out.printf("</tr>%n");
		out.printf("\t<td nowrap>Password:</td>%n");
		out.printf("\t<td>%n");
		out.printf("\t\t<input type=\"text\" name=\"password\" maxlength=\"70\" size=\"30\">%n");
		out.printf("\t<td>%n");
		out.printf("</tr>%n");
		out.printf("<tr>%n");
		out.printf("\t<td>%n");
		out.printf("</tr>%n");
		out.printf("</table>%n");
//		out.printf("<p><input type=\"submit\" value=\"LogIn\"></p>\n%n");
//		out.printf("<form method = \"get\" action=\"%s\">%n", request.getServletPath()+"/");
//		out.printf("<button type = \"submit\"> Log In </button>");
		
		
		out.printf("<p>Not a member?</p>");
		// If "Not a member" click "Sign up" and then go to /sighup page.
		out.printf("<form method = \"get\" action=\"%s\">%n","/signup");
		out.printf("<button type = \"submit\"> Sign up </button>");
	}
}