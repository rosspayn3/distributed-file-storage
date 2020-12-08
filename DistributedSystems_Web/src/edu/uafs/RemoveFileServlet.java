package edu.uafs;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class RemoveFileServlet
 */
@WebServlet("/removefile")
public class RemoveFileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		WebClient client = (WebClient) request.getSession(false).getAttribute("client");
		String input = request.getParameter("removefile");
		String fileToRemove = input.substring(input.indexOf("| ") + 2);
		
		boolean success = client.sendRemoveFileCommand(fileToRemove);
		
		System.out.printf("REMOVE FILE SERVLET: Sent 'remove %s %s' to main server\n", client.getUsername(), fileToRemove);
		
		if(success) {
			request.setAttribute("successmsg", "<strong>File removed.</strong>");
			request.getRequestDispatcher("files.jsp").forward(request, response);
		} else {
			request.setAttribute("errormsg", "<strong>Something went wrong.</strong>");
			request.getRequestDispatcher("files.jsp").forward(request, response);
		}
		
	}

}
