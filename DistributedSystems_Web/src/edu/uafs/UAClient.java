package edu.uafs;
/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Lab 14 - File Server Communication
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.JOptionPane;

public class UAClient {
	public static void main(String[] args) {
		String host = "127.0.0.1";
		int port = 54320;
		
		try {
			System.out.format("Client connecting to %s on %d%n", host, port);
			
			Socket socket = new Socket(host, port);
			
			PrintWriter clientOut = new PrintWriter(socket.getOutputStream(), true);
			MessageReader clientIn = new MessageReader(socket);
			Thread thread = new Thread(clientIn);
			thread.start();
			clientOut.println("client connected");
			
			// get message from server
			String line;
			String message = "";
			while (true) {
				
				message = JOptionPane.showInputDialog("Enter command");
				
				if (message == null)
					break;
				
				if (message != null && (message.equalsIgnoreCase("exit") || message.equalsIgnoreCase("logout")) ) {
					socket.close();
					System.exit(0);
				}
				
				if (message != null && message.trim().length() > 0) {
					clientOut.printf("%s%n", message);
				}
			}
			
			System.out.println("CLIENT: Terminated connection.");
			thread.interrupt();
			socket.close();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static class MessageReader implements Runnable {

		private Socket socket;
		private BufferedReader in;
		
		private MessageReader(Socket socket) {
			this.socket = socket;
			try{
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			while (true) {
				
				if (socket.isClosed()) {
					try{
						in.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					break;
				}
				
				try {
					String message = in.readLine();
					if (message != null && message.length() > 0 && !message.equals("connection test")) {
						System.out.println(message);
					}
				} catch (SocketException ex) {
					if (!ex.getMessage().equals("Socket closed")) {
						ex.printStackTrace();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
