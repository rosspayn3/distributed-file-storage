/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

public class WebClient {
	
	Socket socket;
	PrintWriter clientOut;
	BufferedReader clientIn;
	
	public WebClient(HttpSession session, JspWriter out) {
		
		try {
			this.socket = new Socket("127.0.0.1", 54320);
			this.clientOut = new PrintWriter(socket.getOutputStream(), true);
			this.clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			clientOut.println("client connected");
			getServerResponse();
		} catch(Exception e) {
			try {
				out.print("<h1 class=\"text-danger text-center\"><strong>Exception</strong></h1>"
						+ "<p class=\"text-warning text-center mb-5\">Failed to establish socket connection to main server.</p>");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
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
	
	public boolean register(String username, String password) {
		
		try {
			clientOut.println(String.format("register %s %s", username, password));
			if(getServerResponse().contains("success")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			System.err.println("WEBCLIENT: Exception in WebClient register method.");
			e.printStackTrace();
			return false;
		}
		
	}
	
	public boolean login(String username, String password) {
		try {
			clientOut.println(String.format("login %s %s", username, password));
			if(getServerResponse().contains("success")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			System.err.println("WEBCLIENT: Exception in WebClient login method.");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean sendMessage(String msg) {
		// We may or may not need this method. Just in case.
		
		try {
			clientOut.println(msg);
			return true;
		} catch (Exception e) {
			System.err.println("WEBCLIENT: Exception in WebClient sendMessage method.");
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
			System.err.println("Exception in WebClient getServerResponse method.");
			e.printStackTrace();
			return null;
		}
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
