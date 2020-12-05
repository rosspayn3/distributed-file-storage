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
		
		boolean success = client.login(username, password);
		System.out.printf("LOGIN SERVLET: Sent 'login %s %s' to main server\n", username, password);
		
		if(success) {
			request.getSession().setAttribute("username", username);
			request.getSession().setAttribute("loggedin", "true");
			request.getRequestDispatcher("fileupload.jsp").forward(request, response);
		} else {
			request.setAttribute("errormsg", "<strong>Username or password is incorrect.</strong>");
			request.setAttribute("username", username);
			request.getRequestDispatcher("login.jsp").forward(request, response);
		}
		
	}

}
