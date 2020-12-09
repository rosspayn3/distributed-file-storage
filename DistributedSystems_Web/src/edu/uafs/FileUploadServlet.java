package edu.uafs;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

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

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * <p>
	 * Reads one {@link Part} from a multipart form on {@code files.jsp} and sends that {@link Part}'s bytes
	 * over a {@link WebClient}'s {@link DataOutputStream} to an instance of {@link UAServer}. A success/error
	 * message is assigned before forwarding the request.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			
			WebClient client = (WebClient) request.getSession(false).getAttribute("client");
			
//			boolean success = save(request);
			
			boolean success = upload(request, response, client);
			
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
	
	
	
	/**
	 * Reads one {@link Part} from a multipart form on {@code files.jsp} and sends that {@link Part}'s bytes
	 * over a {@link WebClient}'s {@link DataOutputStream} to an instance of {@link UAServer}.
	 * <p>
	 * This method sets the success/error message for the {@link HttpServletRequest} before returning.
	 * 
	 * @param request The {@link HttpServletRequest} object from {@link FileUploadServlet#doPost(HttpServletRequest, HttpServletResponse)}.
	 * @param response The {@link HttpServletResponse} object from {@link FileUploadServlet#doPost(HttpServletRequest, HttpServletResponse)}.
	 * @param client	The {@link WebClient} stored in the web browser's session.
	 * @return	A boolean value representing the success of a {@link Part}'s transfer to {@link UAServer}.
	 */
	private boolean upload(HttpServletRequest request, HttpServletResponse response, WebClient client) {
		
		try {

			for (Part part : request.getParts()) {
				
				// check if file size over limit we set
				if(part.getSize() > (long) 1024 * 1024 * 200 /* 200 MB */) {
					request.setAttribute("errordetails", "File is larger than 200MB.");
					return false;
				}
				
				// get file name, replace spaces with '-', add .tmp extension in case we need it
				String filename = getFileName(part).replace(" ", "-") + ".temp";
				
				//                                              v----  remove .tmp extension  ----v
				boolean proceed = client.sendAddFileCommand(filename.substring(0, filename.length()-5), part.getSize());
				
				if(!proceed) {
					request.setAttribute("errordetails", "No available file servers.");
					return false;
				}
				
				try{
					
					// number of bytes to send
					int fileSize = (int) part.getSize();
					int bytesLeft = fileSize;
					int pageSize = 4096;
					byte[] buffer = new byte[pageSize];
					int bytesRead = 0;
					
					DataOutputStream dataOut = new DataOutputStream(client.getSocket().getOutputStream());
					BufferedInputStream fileIn = new BufferedInputStream(part.getInputStream());
					
					Instant start = Instant.now();
					
					while( (bytesRead = fileIn.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0 ) {					
						dataOut.write(buffer, 0, Math.min(pageSize, bytesLeft));
						bytesLeft -= bytesRead;
					}
					
					Instant end = Instant.now();
					
					// ******************************************************************************
					// this method says it reads all bytes from an input stream and transfers them to 
					// the output stream. possibly use this instead of the while loop above?
					// ******************************************************************************
//					fileIn.transferTo(dataOut);
					
					Duration timeBetween = Duration.between(start, end);
					
					
					Logger.log("FILEUPLOADSERVLET", String.format("File '%s' of size %d KB sent to UAServer after %sm %ss %sms.",
														filename, NumberFormat.getNumberInstance(Locale.US).format( ((double) fileSize / 1000.0) ),
														timeBetween.toMinutesPart(), timeBetween.toSecondsPart(),
														timeBetween.toMillisPart()
					));

					fileIn.close();

					return true;
				}
				catch(Exception e){
					Logger.log("FILEUPLOADSERVLET", "Exception when sending file to UAServer.");
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
	
	/**
	 * Returns the given file name for a {@link Part}.
	 * 
	 * @param part	The {@link Part} to get the file name from.
	 * @return	The file name for the given {@link Part}.
	 */
	private String getFileName(Part part) {
		
		// use this for built-in browser in Eclipse
		// Collection<String> headers = part.getHeaders("content-disposition");
	    // return headers.toString().substring(headers.toString().lastIndexOf('\\') + 1, headers.toString().length()-2);
		
		// use this for chrome (chromium-based) browsers
	    return Paths.get(part.getSubmittedFileName()).getFileName().toString();
	    
	}

}
