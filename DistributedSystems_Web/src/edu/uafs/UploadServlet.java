package edu.uafs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
		
		Socket socket;
		// this doesn't work, but nice try :)
		if(request.getAttribute("socket") == null) {
			 socket = new Socket("127.0.0.1", 54320);
		} else {
			socket = (Socket) request.getAttribute("socket");
		}
		request.setAttribute("socket", socket);
		
		PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		// get form data from index.jsp input with name="text"
		String text = request.getParameter("text");
		
		// send text from input to server
		toServer.println(text);
		
		// consume initial 'connected to main server' message
		fromServer.readLine();
		
		// save response from server
		request.setAttribute("response", fromServer.readLine());
		
		
		// forward to index.jsp with response from server
		request.getRequestDispatcher("index.jsp").forward(request, response);
	
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("text/html");
		out.println("GET request served at UploadServlet");
		
	}
}
