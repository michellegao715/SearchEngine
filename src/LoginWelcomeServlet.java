import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class LoginWelcomeServlet extends LoginBaseServlet {

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                String user = getUsername(request);

                if (user != null) {
                        prepareResponse("Welcome", response);

                        PrintWriter out = response.getWriter();
                        out.println("<p>Hello " + user + "!</p>");
                        out.println("<p><a href=\"/login?logout\">(logout)</a></p>");
                        response.sendRedirect("/login");
                        finishResponse(request, response);
                }
                else {
                        response.sendRedirect("/login");
                }
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                doGet(request, response);
        }
}