import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class ChangePasswordServlet extends LoginBaseServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		prepareResponse("Change password", response);
		PrintWriter out = response.getWriter();

		printForm(out);
		finishResponse(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		prepareResponse("Change password", response);

		String user = request.getParameter("user");
		String newpass = request.getParameter("newpassword");
		String oldpass = request.getParameter("oldpassword");
		System.out.println("before updatePassword");
		Status status = db.updatePassword(user, oldpass, newpass);
		System.out.println("after updatePassword");
		try {
			// Add update in LoginDatabaseHandler, to update password for the
			// user.
			// if user fill in the right username and password, then redirect
			// him to change password page.
			if (status == Status.OK) {
				System.out.println("before updatePassword, status = OK");
				response.sendRedirect(response.encodeRedirectURL("/login"));
			} else {
				PrintWriter out = response.getWriter();

				out.println("<p style = \"color: red;\">"
						+ "You give the invalid username or password, please try again."
						+ "</p>");

				// response.sendRedirect(response.encodeRedirectURL("/login?error="
				// + status.ordinal()));
			}
		} catch (Exception ex) {
			log.error("Unable to process login form.", ex);
		}
	}

	private void printForm(PrintWriter out) {
		out.println("<form action=\"/changepassword\" method=\"post\">");
		out.println("<table border=\"0\">");
		out.println("\t<tr>");
		out.println("\t\t<td>Usename:</td>");
		out.println("\t\t<td><input type=\"text\" name=\"user\" size=\"30\"></td>");
		out.println("\t</tr>");
		out.println("\t<tr>");
		out.println("\t\t<td>OldPassword:</td>");
		out.println("\t\t<td><input type=\"password\" name=\"oldpassword\" size=\"30\"></td>");
		out.println("\t\t<td>NewPassword:</td>");
		out.println("\t\t<td><input type=\"password\" name=\"newpassword\" size=\"30\"></td>");
		out.println("</tr>");
		out.println("</table>");
		out.println("<p><input type=\"submit\" value=\"ChangePassword\"></p>");
		out.println("</form>");
	}
}