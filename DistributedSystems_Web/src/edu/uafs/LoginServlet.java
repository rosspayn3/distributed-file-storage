package edu.uafs;

import java.io.IOException;
import java.net.Socket;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String username = String.valueOf(request.getParameter("username")).trim();
		String password = String.valueOf(request.getParameter("password")).trim();

		WebClient client = (WebClient) request.getSession(false).getAttribute("client");
		
		client.sendMessage(String.format("login %s %s", username, password));
		System.out.printf("Sent 'login %s %s' to main server\n", username, password);
		
		request.getSession().setAttribute("username", username);
		request.getSession().setAttribute("password", password);
		request.getRequestDispatcher("welcome.jsp").forward(request, response);
	}

}
