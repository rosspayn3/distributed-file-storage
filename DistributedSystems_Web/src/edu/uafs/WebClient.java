/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

public class WebClient {
	
	Socket socket;
	PrintWriter printWriter;
	OutputStream outputStream;
	
	public WebClient(HttpSession session, JspWriter out) {
		
		try {
			this.socket = new Socket("127.0.0.1", 54320);
			this.printWriter = new PrintWriter(socket.getOutputStream(), true);
			this.outputStream = socket.getOutputStream();
			printWriter.println("client connected");
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
		return printWriter;
	}

	public void setPrintWriter(PrintWriter writer) {
		this.printWriter = writer;
	}
	
	public boolean register(String username, String password) {
		
		try {
			printWriter.println(String.format("register %s %s", username, password));
			return true;
		} catch (Exception e) {
			System.err.println("Exception in WebClient register method.");
			e.printStackTrace();
			return false;
		}
		
	}
	
	public boolean login(String username, String password) {
		try {
			printWriter.println(String.format("login %s %s", username, password));
			return true;
		} catch (Exception e) {
			System.err.println("Exception in WebClient login method.");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean sendMessage(String msg) {
		// We may or may not need this method. Just in case.
		
		try {
			printWriter.println(msg);
			return true;
		} catch (Exception e) {
			System.err.println("Exception in WebClient sendMessage method.");
			e.printStackTrace();
			return false;
		}
		
	}
	
	public boolean addFile(String file) {
		
		try {
			// send file using output stream
			printWriter.println("add " + file);
			
			return true;
		} catch (Exception e) {
			// something bad happened
			
			e.printStackTrace();
			return false;
		}
	}
	
}
