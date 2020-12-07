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

public class WebClient {
	
	Socket socket;
	PrintWriter clientOut;
	BufferedReader clientIn;
	ArrayList<String> filenames;
	String username;
	
	
	public WebClient(HttpSession session, JspWriter out) {
		
		try {
			// this attempts to connect to the wrong port to simulate connection failure.
			// code in .jsp pages should call the connect() method with the correct port on page refresh.
			
//			this.socket = new Socket("127.0.0.1", 54320);
//			this.clientOut = new PrintWriter(socket.getOutputStream(), true);
//			this.clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//			clientOut.println("client connected");
//			getServerResponse();
			
			// use this method once testing is complete
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
	
	public boolean connect(JspWriter out) {
		
		try {
			this.socket = new Socket("127.0.0.1", 32122);
			this.clientOut = new PrintWriter(socket.getOutputStream(), true);
			this.clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.filenames = new ArrayList<String>();
			clientOut.println("client connected");
			getServerResponse();
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

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public PrintWriter getPrintWriter() {
		return clientOut;
	}

	public void setPrintWriter(PrintWriter writer) {
		this.clientOut = writer;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setFileNames(ArrayList<String> filenames) {
		this.filenames = filenames;
	}
	
	public ArrayList<String> getFileNames(){
		return this.filenames;
	}
	
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
			System.err.println("WEBCLIENT: Exception in WebClient register method.");
			e.printStackTrace();
			return false;
		}
		
		return success;
	}
	
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
			System.err.println("!! WEBCLIENT: Exception in WebClient login method.\n");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean sendMessage(String msg) {
		
		try {
			clientOut.println(msg);
			System.out.println("WEBCLIENT: Sent '" + msg + "' to server.");
			return true;
		} catch (Exception e) {
			System.err.println("!! WEBCLIENT: Exception in WebClient sendMessage method.\n");
			e.printStackTrace();
			return false;
		}
		
	}
	
	public String getServerResponse() {
		try {
			String response = clientIn.readLine();
			System.out.println("WEBCLIENT: Response from server: " + response);
			return response;
		} catch (Exception e) {
			System.err.println("!! WEBCLIENT: Exception in WebClient getServerResponse method.\n");
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean sendAddFileCommand(String filename, long size) {
		
		boolean success = false;
		
		try {
			
			sendMessage( String.format("add %s %d", filename, size) );
			String response = getServerResponse();
			if(response.contains("accepted add command")) {
				success = true;
			} else if (response.contains("no available file servers")) {
				success = false;
			}
			
		} catch (Exception e) {
			System.err.println("!! WEBCLIENT: Exception in WebClient sendAddFileCommand method.\n");
		}
		
		return success;
		
	}
	
	public boolean sendRemoveFileCommand(String filename) {
		
		boolean success = false;
		
		try {
			
			sendMessage( String.format("remove %s %s", this.username, filename) );
			String response = getServerResponse();
			if(response.contains("removing file")) {
				success = true;
			} else if (response.contains("Could not find") || response.contains("no available file servers")) {
				success = false;
			}
			
		} catch (Exception e) {
			System.err.println("!! WEBCLIENT: Exception in WebClient sendRemoveFileCommand method.\n");
		}
		
		return success;
		
	}
	
	public ArrayList<String> listUserFiles(String username) {
		
		filenames.clear();
		
		try {
			clientOut.printf("list %s\n", username);
			
			String response = getServerResponse();
			
			if(response.contains("listing filenames")) {
				String filename;
				while( !(filename = clientIn.readLine()).equals("done")) {
					filenames.add(filename);
				}
			} else if (response.contains("Could not find any files") || response.contains("No available file servers")){
				filenames.add("No files.");
			} else {
				filenames.add("Something went wrong.");
			}
		} catch (Exception e) {
			System.err.println("!! WEBCLIENT: Exception in WebClient listUserFiles method.\n");
			e.printStackTrace();
		}
		
		return filenames;
	}
	
	public boolean sendFile(String file) {
		
		try {
			
			// send 'add' command (or just start writing bytes to output stream)
			
			// maybe wait for 'ready' message from file server before transferring?
			
			// wait for success message after transfer is complete
			
			// return true/false
			
			
			
			// this works with the String storage system we have now
			clientOut.println("add " + file);
			String response = getServerResponse();
			// consume extra success message from second file server 
			getServerResponse();
			if(response.contains("success")) {
				return true;
			} else {
				return false;
			}
			
		} catch (Exception e) {
			// something bad happened
			
			e.printStackTrace();
			return false;
		}
	}
	
}
