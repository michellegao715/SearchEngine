import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class LogoutServlet extends LoginBaseServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		

		clearCookies(request, response);
		response.sendRedirect(response.encodeRedirectURL("/login"));

		finishResponse(request,response);
		
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		doGet(request, response);
	}
	
//	 private void printForm(PrintWriter out) {
//         out.println("<form action=\"/register\" method=\"post\">");
//         out.println("<table border=\"0\">");
//         out.println("\t<tr>");
//         out.println("\t\t<td>Usename:</td>");
//         out.println("\t\t<td><input type=\"text\" name=\"user\" size=\"30\"></td>");
//         out.println("\t</tr>");
//         out.println("\t<tr>");
//         out.println("\t\t<td>Password:</td>");
//         out.println("\t\t<td><input type=\"password\" name=\"pass\" size=\"30\"></td>");
//         out.println("</tr>");
//         out.println("</table>");
//         out.println("<p><input type=\"submit\" value=\"Register\"></p>");
//         out.println("</form>");
// }
	
	
}