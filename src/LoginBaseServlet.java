import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("serial")
public class LoginBaseServlet extends HttpServlet {

        protected static final Logger log = LogManager.getLogger();
        protected static final LoginDatabaseHandler db = LoginDatabaseHandler.getInstance();

        /**
         * Prepares the HTTP response by setting the content type and adding header
         * HTML code.
         *
         * @param title - web page title
         * @param response - HTTP response
         * @throws IOException
         * @see #finishResponse(HttpServletRequest, HttpServletResponse)
         */
        public static void prepareResponse(String title,
                        HttpServletResponse response) throws IOException {

                response.setContentType("text/html");
                PrintWriter out = response.getWriter();

                out.printf("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
                out.printf("\"http://www.w3.org/TR/html4/strict.dtd\">%n%n");
                out.printf("<html>%n%n");
                out.printf("<head>%n");
                out.printf("\t<title>%s</title>%n", title);
                out.printf("\t<meta http-equiv=\"Content-Type\" ");
                out.printf("content=\"text/html;charset=utf-8\">%n");
                out.printf("</head>%n%n");
                out.printf("<body>%n%n");
        }

        /**
         * Finishes the HTTP response by adding footer HTML code and setting the
         * response code.
         *
         * @param request - HTTP request
         * @param response - HTTP response
         * @throws IOException
         * @see #prepareResponse(String, HttpServletResponse)
         */
        public static void finishResponse(
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {

                PrintWriter out = response.getWriter();

                out.printf("%n");
                out.printf("<p style=\"font-size: 10pt; font-style: italic; text-align: center;");
                out.printf("border-top: 1px solid #eeeeee; margin-bottom: 1ex;\">");

                out.printf("Page <a href=\"%s\">%s</a> generated on %s. ",
                                request.getRequestURL(), request.getRequestURL(),
                                StringUtilities.getDate("yyyy-MM-dd hh:mm a"));

                out.printf("</p>%n%n");
                out.printf("</body>%n");
                out.printf("</html>%n");

                out.flush();

                response.setStatus(HttpServletResponse.SC_OK);
                response.flushBuffer();
        }

        /**
         * Gets the cookies form the HTTP request, and maps the cookie key to the
         * cookie value.
         *
         * @param request - HTTP request from web server
         * @return map from cookie key to cookie value
         */
        public static Map<String, String> getCookieMap(HttpServletRequest request) {
                HashMap<String, String> map = new HashMap<>();
                Cookie[] cookies = request.getCookies();

                if (cookies != null) {
                        for (Cookie cookie : cookies) {
                                map.put(cookie.getName(), cookie.getValue());
                        }
                }

                return map;
        }

        /**
         * Clears all of the cookies included in the HTTP request.
         *
         * @param request - HTTP request
         * @param response - HTTP response
         */
        public static void clearCookies(
                        HttpServletRequest request,
                        HttpServletResponse response) {

                Cookie[] cookies = request.getCookies();

                if (cookies != null) {
                        for (Cookie cookie : cookies) {
                                cookie.setValue(null);
                                cookie.setMaxAge(0);
                                response.addCookie(cookie);
                        }
                }
        }

        /**
         * Gets the username stored in cookies, or returns null if the user is not
         * logged in.
         *
         * @param request
         * @return username
         */
        public static String getUsername(HttpServletRequest request) {
                Map<String, String> cookies = getCookieMap(request);

                String login = cookies.get("login");
                String user  = cookies.get("name");

                if ((login != null) && login.equals("true") && (user != null)) {
                		
                        // this is not safe!
                        return user;
                }

                return null;
        }
}