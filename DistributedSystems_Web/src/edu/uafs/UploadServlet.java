/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class UploadServlet
 */
@WebServlet( name= "UploadServlet", urlPatterns = {"/upload"} )
public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public UploadServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		WebClient client = (WebClient) request.getSession(false).getAttribute("client");
		String text = request.getParameter("text");
		
		try {
			// transfer file to main server
			client.addFile(text);
			System.out.printf("UPLOAD SERVLET: Sent 'add %s' to main server\n", text);
			request.setAttribute("successmsg", "File uploaded successfully!");
		} catch (Exception e) {
			// something bad happened
			
			request.setAttribute("errormsg", "Exception when uploading file... Wups.");
		}
		
		
		// forward to upload.jsp with error/success message
		request.getRequestDispatcher("upload.jsp").forward(request, response);
	
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("text/html");
		out.println("GET request served at UploadServlet");
		
	}
}
