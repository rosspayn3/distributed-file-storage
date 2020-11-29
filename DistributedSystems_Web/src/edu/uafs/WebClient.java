package edu.uafs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

public class WebClient {
	
	Socket socket;
	PrintWriter printWriter;
	
	public WebClient(HttpSession session, JspWriter out) {
		
		try {
			this.socket = new Socket("127.0.0.1", 54320);
			this.printWriter = new PrintWriter(socket.getOutputStream(), true);
			printWriter.println("client connected");
		} catch(Exception e) {
			try {
				out.print("<h1 class=\"text-danger text-center\"><strong>Exception</strong></h1><p class=\"text-warning text-center mb-5\">Failed to establish socket connection to main server.</p>");
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
	
	public boolean sendMessage(String msg) {
		
		try {
			printWriter.println(msg);
			return true;
		} catch (Exception e) {
			System.err.println("Exception in WebClient sendMessage method.");
			e.printStackTrace();
			return false;
		}
		
	}
	
}
