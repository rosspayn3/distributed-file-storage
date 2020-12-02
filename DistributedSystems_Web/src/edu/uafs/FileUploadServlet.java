package edu.uafs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
	
	private static final File tempdir = new File("C:\\upload");

	
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
		
		// use this for built-in browser in Eclipse
	    // return headers.toString().substring(headers.toString().lastIndexOf('\\') + 1, headers.toString().length()-2);
		
		// use this for chrome
	    return Paths.get(part.getSubmittedFileName()).getFileName().toString();
	    
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
				
				// check if file size over X
				if(part.getSize() > (long) 1024 * 1024 * 100 /* 100 MB */) {
					return false;
				}
				
				// get file name and replace spaces with '-'
				String filename = getFileName(part).replace(" ", "-") + ".temp";
				String username = (String) request.getSession().getAttribute("username");
				
				// number of bytes to send
				int size = (int) part.getSize();
				
				//                                              v----  remove .tmp extension  ----v
				client.sendMessage( String.format("add %s %d", filename.substring(0, filename.length()-5), part.getSize()) );
				
				try{
					
					// save part as temporary file
					// will need to update tempdir once this is running on Linux
					File tempFile = new File(tempdir, filename);
					
					try {
						// check if temporary file already exists from a previous failed upload
						if(!tempFile.exists()) {
							Files.copy(part.getInputStream(), tempFile.toPath());
						}
						
					} catch (Exception e) {
						System.err.println("FILEUPLOADSERVLET: Exception when writing temp file.");
						e.printStackTrace();
					}
					
					
					int pageSize = 1024;
					byte[] buffer = new byte[pageSize];
					
					DataOutputStream out = new DataOutputStream(client.getSocket().getOutputStream());
					System.out.println("FILEUPLOADSERVLET: BufferedInputStream opening on temp file :  " + tempdir + File.separator + filename);
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tempdir + File.separator + filename));
					
					// buffered output stream for saving file locally for testing
					// BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("C:\\upload\\received\\"+"\\"+username+"\\"+filename.substring(0,filename.length()-5)));
					
					int bytesRead = 0;
					int bytesLeft = (int) part.getSize();
					
					while( (bytesRead = bis.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0 ) {
						
						// print bytes in buffer to console for testing
						// for(byte c : buffer) {
						// 	System.out.print((char) c);
						// }
						
						// write file locally
						// bos.write(buffer, 0 , bytesRead);
						
						// send file over socket
						out.write(buffer, 0 , bytesRead);
						
						bytesLeft -= bytesRead;
					}
					
					System.out.println("FILEUPLOADSERVLET: Done sending file to UAServer.");
					
					// ******************************************************************************
					// this method says it reads all bytes from an input stream and transfers them to 
					// the output stream. possibly use this instead of the while loop above?
					// ******************************************************************************
					//bis.transferTo(out);

					
					// ************************************************************************************
					// can't close output stream without getting a 'socket closed' exception on next upload
					// ************************************************************************************
					// out.close();
					// bos.close();
					bis.close();
					
					// delete temporary file
					tempFile.delete();
					System.out.println("FILEUPLOADSERVLET: Deleted temp file.");

					return true;
				}
				catch(Exception e){
					e.printStackTrace();
					return false;
				}
		    }
			
			return true;
		} catch (IllegalStateException | IOException | ServletException e) {
			// IllegalStateException is thrown when file is >100MB (the value that is set above)
			e.printStackTrace();
			return false;
		}
		
	}

}
