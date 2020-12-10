/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class RegistrationServlet
 */
@WebServlet("/register")
public class RegistrationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RegistrationServlet() {
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
	 * <p>
	 * Accepts a username and password from {@code register.jsp}. The credentials are then sent to an 
	 * instance of {@link UAServer} using the browser session's {@link WebClient} socket connection. 
	 * The credentials are then stored for future use.
	 * <p>
	 * This method assigns a success/error message before forwarding the request.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String username = String.valueOf(request.getParameter("username")).trim();
		String password = String.valueOf(request.getParameter("password")).trim();
		String confirmpassword = String.valueOf(request.getParameter("confirmpassword")).trim();
		
		WebClient client = (WebClient) request.getSession(false).getAttribute("client");
		
		if(password.equals(confirmpassword)) {
			boolean success = client.register(username, password, request);
			if(success) {
				System.out.printf("REGISTRATION SERVLET: Sent 'register %s %s' to main server\n", username, password);
				response.sendRedirect("login.jsp");
			} else {
				request.setAttribute("errormsg", "A problem occurred when registering.");
				request.setAttribute("username", username);
				request.getRequestDispatcher("register.jsp").forward(request, response);
			}
			
		} else {
			request.setAttribute("errormsg", "Passwords do not match.");
			request.setAttribute("username", username);
			request.getRequestDispatcher("register.jsp").forward(request, response);
		}
		
		
	}

}
