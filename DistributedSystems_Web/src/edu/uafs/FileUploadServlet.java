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
	
	private static File tempdir = new File("tempfiles");

	
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
	
	
	
	/*
	 *  Sends a file to the UAServer
	 */
	private boolean upload(HttpServletRequest request, WebClient client) {
		
		try {
			
			if(!tempdir.exists()) {
				tempdir.mkdirs();
			}
			
			for (Part part : request.getParts()) {
				
				// check if file size over limit we set
				if(part.getSize() > (long) 1024 * 1024 * 200 /* 200 MB */) {
					request.setAttribute("errordetails", "File is larger than 200MB.");
					return false;
				}
				
				// get file name and replace spaces with '-'
				String filename = getFileName(part).replace(" ", "-") + ".temp";
				
				// send 'add' command to server with filename and size
				//                                              v----  remove .tmp extension  ----v
				client.sendMessage( String.format("add %s %d", filename.substring(0, filename.length()-5), part.getSize()) );
				
				try{
					
					// save part as temporary file
					// will need to update tempdir once this is running on Linux
					File tempFile = new File(tempdir + File.separator + filename);
					
					try {
						// check if temporary file already exists from a previous failed upload
						if(!tempFile.exists()) {
							Files.copy(part.getInputStream(), tempFile.toPath());
						}
						
					} catch (Exception e) {
						System.err.println("FILEUPLOADSERVLET: Temporary file already exists for attempted upload.");
						e.printStackTrace();
					}
					
					// number of bytes to send
					int bytesLeft = (int) tempFile.length();
					int pageSize = 4096;
					byte[] buffer = new byte[pageSize];
					int bytesRead = 0;
					
					DataOutputStream dataOut = new DataOutputStream(client.getSocket().getOutputStream());
					System.out.println("FILEUPLOADSERVLET: BufferedInputStream opening on temp file :  " + tempdir + File.separator + filename);
					BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(tempdir + File.separator + filename));
					
					// buffered output stream for saving file locally for testing
					// String username = (String) request.getSession().getAttribute("username");
					// BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("C:\\upload\\received\\"+"\\"+username+"\\"+filename.substring(0,filename.length()-5)));
					
					while( (bytesRead = fileIn.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0 ) {
						
						// write file locally for testing
						// bos.write(buffer, 0 , bytesRead);
						
						// send file over socket
						dataOut.write(buffer, 0, Math.min(pageSize, bytesLeft));
						
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
					fileIn.close();
					
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
			// IllegalStateException is thrown when file is larger than max we set
			e.printStackTrace();
			return false;
		}
		
	}
	
	private String getFileName(Part part) {
		
		Collection<String> headers = part.getHeaders("content-disposition");
		
		// use this for built-in browser in Eclipse
	    return headers.toString().substring(headers.toString().lastIndexOf('\\') + 1, headers.toString().length()-2);
		
		// use this for chrome
	    // return Paths.get(part.getSubmittedFileName()).getFileName().toString();
	    
	}

}
