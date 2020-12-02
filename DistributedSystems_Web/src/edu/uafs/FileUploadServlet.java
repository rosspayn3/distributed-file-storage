package edu.uafs;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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
@MultipartConfig()
public class FileUploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final File tempDir = new File("C:\\upload");

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			
			WebClient client = (WebClient) request.getSession(false).getAttribute("client");
			
//			boolean success = save(request);
			
			boolean success = upload(request, client);
			
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
		request.getRequestDispatcher("fileupload.jsp").forward(request, response);
		
	}
	
	private String getFileName(Part part) {
		
		Collection<String> headers = part.getHeaders("content-disposition");
		
	    return headers.toString().substring(headers.toString().lastIndexOf('\\') + 1, headers.toString().length()-2);
		
	}
	
	/*
	 *  Saves a file locally
	 */
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
	
	/*
	 *  Sends a file to the UAServer
	 */
	private boolean upload(HttpServletRequest request, WebClient client) {
		
			
		
		try {
			
			for (Part part : request.getParts()) {
				
				if(part.getSize() > (long) 1024 * 1024 * 100) {
					return false;
				}
				
//				String filename = getFileName(part);
				String filename = Paths.get(part.getSubmittedFileName()).getFileName().toString().replace(" ", "-");
				int size = (int) part.getSize();
				
				client.sendMessage( String.format("add %s %d", filename, part.getSize()) );
				
				try{
					
					// save temporary file
					
					File tempFile = new File(tempDir, filename);
					
					try {
						Files.copy(part.getInputStream(), tempFile.toPath());
					} catch (Exception e) {
						// TODO: handle exception
					}
					
					
					// send over socket
					
					DataOutputStream out = new DataOutputStream(client.getSocket().getOutputStream());
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tempFile));
					
					
					int fourKBpage = 4096;
					byte[] b = new byte[fourKBpage];
					
					
					while( bis.read() > -1) {
						out.write(b);
					}
					
					
					// delete temporary file????

					
					// close streams
					out.close();
					bis.close();

					return true;
				}
				catch(Exception e){
					e.printStackTrace();
					return false;
				}
		    }
			
			return true;
		} catch (IllegalStateException | IOException | ServletException e) {
			// IllegalStateException is thrown when file is >20MB (the value that is set above)
			e.printStackTrace();
			return false;
		}
		
	}

}
