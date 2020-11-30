package edu.uafs;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet implementation class FileUploadServlet
 */
@WebServlet("/fileupload")
@MultipartConfig(
		
		// 20MB max file size (servlet breaks if a file is larger than this. need to troubleshoot.)
		maxFileSize = 1024 * 1024 * 20,
		// 110MB max request size, should allow 5 files at once (plus some overhead)
		maxRequestSize = 1024 * 1024 * 110
)
public class FileUploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			
			boolean success = save(request);
			
			if( success ) {
				request.setAttribute("successmsg", "File(s) uploaded successfully!");
			} else {
				request.setAttribute("errormsg", "Something went wrong when uploading file(s).");
			}
			
		} catch (Exception e) {
			// something bad happened
			System.err.println("Something bad happened while saving file(s).");
			e.printStackTrace();
			request.setAttribute("errormsg", "Exception when uploading file.");
		}
		
		// forward to upload.jsp with error/success message
		request.getRequestDispatcher("upload.jsp").forward(request, response);
		
	}
	
	private String getFileName(Part part) {
		
		Collection<String> headers = part.getHeaders("content-disposition");
		
	    return headers.toString().substring(headers.toString().lastIndexOf('\\') + 1, headers.toString().length()-2);
		
	}
	
	private boolean save(HttpServletRequest request) {
		
		try {
			String username = (String) request.getSession().getAttribute("username");
			
			for (Part part : request.getParts()) {
				if(part.getSize() > (long) 1024 * 1024 * 20) {
					return false;
				}
				String filename = getFileName(part);
				// will need to change directory if running on Linux
				part.write("C:\\upload\\" + username + "-" + filename);
				System.out.printf("File '%s' saved to C:\\upload\\\n", filename);
		    }
			
			return true;
		} catch (IllegalStateException | IOException | ServletException e) {
			// IllegalStateException is thrown when file is >20MB (the value that is set above)
			e.printStackTrace();
			return false;
		}
		
	}

}
