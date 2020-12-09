/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

/**
 * A client class to be used in {@code .jsp} pages for communication with an instance of {@link UAServer}.
 * <p> 
 * An instance of this object will be maintained in a web browser session and passed to servlets to facilitate 
 * communication.
 *
 */
public class WebClient {
	
	private Socket socket;
	private PrintWriter clientOut;
	private BufferedReader clientIn;
	private ArrayList<String> filenames;
	private String username;
	
	/**
	 * Default constructor for {@link WebClient}. {@link WebClient#connect(JspWriter)} is called 
	 * inside the constructor to initialize a {@link Socket} connection with an instance of {@link UAServer}.
	 * <p>
	 * This method prints a success/error message to the {@code .jsp} page that called this method. 
	 * 
	 * @param session	The current browser {@link HttpSession} object.
	 * @param out	The {@link JspWriter} of the {@code .jsp} page that called this method.
	 */
	public WebClient(HttpSession session, JspWriter out) {
		
		try {
			
			connect(out);
			
		} catch(Exception e) {
			try {
				out.print("<h1 class=\"text-danger text-center mt-3\"><strong>Exception</strong></h1>"
						+ "<p class=\"text-warning text-center\">Failed to establish socket connection to server."
						+ "<br>Refresh the page, or close the browser window and open this page again to attempt to reconnect.</p>");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Initializes a new {@link Socket} connection with an instance of {@link UAServer}.
	 * <p>
	 * This method prints a success/error message to the {@code .jsp} page that called this method. 
	 * 
	 * @param out	The {@link JspWriter} of the {@code .jsp} page that called either 
	 * 				{@link WebClient#WebClient(HttpSession, JspWriter)}	or this method.
	 * 
	 * @return	A boolean value representing the success of the attempted {@link Socket} connection.
	 */
	public boolean connect(JspWriter out) {
		
		try {
			this.socket = new Socket("127.0.0.1", 32122);
			this.clientOut = new PrintWriter(socket.getOutputStream(), true);
			this.clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.filenames = new ArrayList<String>();
			clientOut.println("client connected");
			return true;
		} catch (Exception e) {
			
			try {
				out.print("<h1 class=\"text-danger text-center mt-3\"><strong>Exception</strong></h1>"
						+ "<p class=\"text-warning text-center\">Failed attempt to reconnect to server."
						+ "<br>Refresh the page, or close the browser window and open this page again to attempt to reconnect.</p>");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			e.printStackTrace();
			return false;
		}
		
	}

	/**
	 * Sends a register command to an instance of {@link UAServer} via the {@link Socket} stored in this object.
	 * <p>
	 * This method assigns a success/error message to the {@link HttpServletRequest}.
	 * 
	 * @param username	The username
	 * @param password	The password
	 * @param request	The {@link HttpServletRequest} from the servlet calling this method.
	 * 
	 * @return	A boolean value representing the success of the operation.
	 */
	public boolean register(String username, String password, HttpServletRequest request) {
		
		boolean success = false;
		try {
			clientOut.println(String.format("register %s %s", username, password));
			String response = getServerResponse();
			
			if(response.contains("success")) {
				success = true;
			} else if (response.contains("taken")){
				request.setAttribute("errordetails", "Username is taken. Try another username.");
				success = false;
			} else if (response.contains("failed")){
				request.setAttribute("errordetails", "Exception in server. Complain about it to someone!");
				success = false;
			}
		} catch (Exception e) {
			System.out.println("WEBCLIENT: Exception in WebClient register method.");
			e.printStackTrace();
			return false;
		}
		
		return success;
	}
	
	/**
	 * Sends a login command to an instance of {@link UAServer} for validation via the {@link Socket} 
	 * stored in this object. If the {@link UAServer} responds with a success message, the given username 
	 * is stored in the current instance of {@link WebClient}.
	 * 
	 * @param username	The username.
	 * @param password	The password.
	 * 
	 * @return	A boolean value representing the success of the login operation.
	 */
	public boolean login(String username, String password) {
		try {
			clientOut.println(String.format("login %s %s", username, password));
			if(getServerResponse().contains("success")) {
				this.username = username;
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			System.out.println("!! WEBCLIENT: Exception in WebClient login method.\n");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Sends a message to an instance of {@link UAServer} via the {@link Socket} stored in this object.
	 * 
	 * @param msg The message to be sent.
	 * 
	 * @return	A boolean value representing the success of the send operation.
	 */
	public boolean sendMessage(String msg) {
		
		try {
			clientOut.println(msg);
			System.out.println("WEBCLIENT: Sent '" + msg + "' to server.");
			return true;
		} catch (Exception e) {
			System.out.println("!! WEBCLIENT: Exception in WebClient sendMessage method.\n");
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Reads one line from the {@link InputStream} of the {@link Socket} stored in this object.
	 *  
	 * @return	The line that was read.
	 */
	public String getServerResponse() {
		try {
			String response = clientIn.readLine();
			System.out.println("WEBCLIENT: Response from server: " + response);
			return response;
		} catch (Exception e) {
			System.out.println("!! WEBCLIENT: Exception in WebClient getServerResponse method.\n");
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Sends an add file command to an instance of {@link UAServer} via the {@link Socket} stored in this object. 
	 * This method should be called before transferring bytes of a file over the {@link Socket} to tell the 
	 * {@link UAServer} how many bytes should be read for the file.
	 * <p>
	 * One or more {@link FileServer} data nodes must be connected to the {@link UAServer} for the command 
	 * to be accepted.
	 * 
	 * @param filename	The name of the file to be sent.
	 * @param size	The size of the file, in bytes.
	 * 
	 * @return	A boolean value representing whether the {@link UAServer} has accepted the request for file transfer.
	 */
	public boolean sendAddFileCommand(String filename, long size) {
		
		boolean success = false;
		
		try {
			
			sendMessage( String.format("add %s %d", filename, size) );
			String response = getServerResponse();
			if(response.contains("accepted")) {
				success = true;
			} else if (response.contains("no available file servers")) {
				success = false;
			}
			
		} catch (Exception e) {
			System.out.println("!! WEBCLIENT: Exception in WebClient sendAddFileCommand method.\n");
		}
		
		return success;
		
	}
	
	/**
	 * Sends a remove file command to an instance of {@link UAServer} via the {@link Socket} stored in this object.
	 * <p>
	 * One or more {@link FileServer} data nodes must be connected to the {@link UAServer} for the command 
	 * to be accepted.
	 * 
	 * @param filename	The name of the file to be removed.
	 * 
	 * @return	A boolean value representing whether the {@link UAServer} has accepted the request, AND found the file 
	 * 			to be removed.
	 */
	public boolean sendRemoveFileCommand(String filename) {
		
		boolean success = false;
		
		try {
			
			sendMessage( String.format("remove %s %s", this.username, filename) );
			String response = getServerResponse();
			if(response.contains("removing file")) {
				success = true;
			} else if (response.contains("Could not find") || response.contains("available file servers")) {
				success = false;
			}
			
		} catch (Exception e) {
			System.out.println("!! WEBCLIENT: Exception in WebClient sendRemoveFileCommand method.\n");
		}
		
		return success;
		
	}
	
	/**
	 * Sends a request to list files for the current user stored in the instance of {@link WebClient} to an instance of 
	 * {@link UAServer} via the {@link Socket} stored in this object.
	 * 
	 * @param username The user files should be listed for.
	 * @return	An {@link ArrayList} of file names that belong to the user. The list will contain one element with a value 
	 * 			of "No files." if no files were found, or an error message.
	 */
	public ArrayList<String> listUserFiles(String username) {
		
		filenames.clear();
		
		try {
			clientOut.printf("list %s\n", username);
			
			String response = getServerResponse();
			
			if(response.contains("listing filenames")) {
				String filename;
				while( !(filename = clientIn.readLine()).equals("done")) {
					System.out.println("File name received: " + filename);
					filenames.add(filename);
				}
			} else if (response.contains("Could not find any files") || response.contains("No available file servers")){
				filenames.add("No files.");
			} else {
				filenames.add("Something went wrong.");
			}
		} catch (Exception e) {
			System.out.println("!! WEBCLIENT: Exception in WebClient listUserFiles method.\n");
			e.printStackTrace();
		}
		
		return filenames;
	}
	

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * @param socket the socket to set
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	/**
	 * @return the clientOut
	 */
	public PrintWriter getClientOut() {
		return clientOut;
	}

	/**
	 * @param clientOut the clientOut to set
	 */
	public void setClientOut(PrintWriter clientOut) {
		this.clientOut = clientOut;
	}

	/**
	 * @return the clientIn
	 */
	public BufferedReader getClientIn() {
		return clientIn;
	}

	/**
	 * @param clientIn the clientIn to set
	 */
	public void setClientIn(BufferedReader clientIn) {
		this.clientIn = clientIn;
	}

	/**
	 * @return the filenames
	 */
	public ArrayList<String> getFilenames() {
		return filenames;
	}

	/**
	 * @param filenames the filenames to set
	 */
	public void setFilenames(ArrayList<String> filenames) {
		this.filenames = filenames;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
}
