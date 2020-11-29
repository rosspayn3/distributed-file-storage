package edu.uafs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

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
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String username = String.valueOf(request.getParameter("username")).trim();
		String password = String.valueOf(request.getParameter("password")).trim();
		String confirmpassword = String.valueOf(request.getParameter("confirmpassword")).trim();
		
		WebClient client = (WebClient) request.getSession(false).getAttribute("client");
		
		if(password.equals(confirmpassword)) {
			client.sendMessage(String.format("register %s %s", username, password));
			System.out.printf("Sent 'register %s %s' to main server\n", username, password);
			response.sendRedirect("login.jsp");
		} else {
			request.setAttribute("errormsg", "Passwords do not match.");
			request.setAttribute("username", username);
			request.getRequestDispatcher("register.jsp").forward(request, response);
		}
		
		
	}

}
