/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class FileServer {

	private static HashMap<String, String> files = new HashMap<>();
	private static File fileDirectory;
	
	public static void main(String[] args) throws IOException {
		
		// get starting directory from command arguments
		// final String path = args[0];
		
		// hard-coded starting directory for now
		final String path = "C:\\files";
		fileDirectory = new File(path);
		
		try {
			if(!fileDirectory.exists()) {
				// will create directory along with parent directories if necessary
				fileDirectory.mkdirs();
			}
		} catch (Exception e) {
			log("Exception when attempting to create starting directory. Exiting...");
			System.exit(1);
		}
		

		// IP address of main server
		String host = "127.0.0.1";
		// port main server is listening on
		int port = 54320;
		
		Socket socket = new Socket(host, port);
		BufferedReader fileServerIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter fileServerOut = new PrintWriter(socket.getOutputStream(), true);
		
		fileServerOut.println("file server connected");
		
		// consume the connection message sent by the master server
		log(fileServerIn.readLine());
		
		String line;
		while ((line = fileServerIn.readLine()) != null) {
			if (!line.equalsIgnoreCase("invalid command.") && !line.equals("connection test")) {
				
                System.out.format("Message received: %s%n", line);
                
                if (line.equals("distribute")) {
                	
                    StringBuilder sb = new StringBuilder();
                    sb.append("distribute~");
                    
                    for (String file : files.keySet()) {
                        sb.append(String.format("%s|", file));
                    }
                    
                    sb.replace(sb.lastIndexOf("|"), sb.length(), "");
                    log("Files being sent: " + sb.toString());
                    fileServerOut.println(sb.toString());
                    files.clear();
                    
                } else {
                	
                    String[] cmdArgs = line.split("~");
                    String socketString = cmdArgs[0];
                    String command = cmdArgs[1];
                    String parameter = cmdArgs[2];
                    
                    switch(command) {
                    
	                    case "add":
	                    	
	                    	// add syntax: add {username} {filename} {size}
	                        if (!files.containsKey(parameter)) {
	                        	
	                            files.put(parameter, parameter);
	                            //receiveFile();
	                            
	                            if (socketString.equals("distributor")) {
	                            	log(String.format("%s successfully re-inserted.", parameter));
	                            } else {
	                                fileServerOut.format("F|%s~%s successfully added to filesystem.%n", socketString, parameter);
	                            }
	                            
	                        } else {
	                        	
	                            if (socketString.equals("distributor")) {
	                                log(String.format("%s already exists in this server.", parameter));
	                            } else {
	                                fileServerOut.format("F|%s~ERROR: %s already exists in filesystem.%n", socketString, parameter);
	                            }
	                            
	                        }
	                        
	                        break;
	                        
	                    case "remove":
	                    	
	                        if (files.containsKey(parameter)) {
	                            files.remove(parameter);
	                            fileServerOut.format("F|%s~%s successfully removed from filesystem.%n", socketString, parameter);
	                        } else {
	                            fileServerOut.format("F|%s~ERROR: %s not found in filesystem; nothing to remove.%n", socketString, parameter);
	                        }
	                        
	                        break;
	                        
	                    case "list":
	                    	
	                        StringBuilder sb = new StringBuilder();
	                        for (String file : files.keySet()) {
	                            sb.append(String.format("F|%s~%s%n", socketString, file));
	                        }
	                        
	                        fileServerOut.println(sb.toString());
	                        
	                        break;
	                        
	                    default:
	                    	
	                        fileServerOut.format("F|%s~Command [%s] not recognized.%n", socketString, command);
	                        
	                        break;
	                        
                    }
                }
			}
		}
	}
	
	private static void log(String message) {

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss.SS");
		System.out.println(dateFormatter.format(LocalDateTime.now()) + " FILE SERVER :  " + message);

	}
	
	private static boolean receiveFile(Socket socket, String user, String filename, String size) {
		
		try {
			
			File path = new File(fileDirectory + File.separator + user);
			
			try {
				if(!path.exists()) {
					path.mkdirs();
				}
			} catch (Exception e) {
				log("Exception when attempting to create directory to receive file.");
			}
			
			int fileSize = Integer.parseInt(size);
			int pageSize = 4096;
			byte[] buffer = new byte[pageSize];
			int bytesRead = 0;
			int bytesLeft = fileSize;
			log("Size of file about to receive: " + bytesLeft);

			DataInputStream dataIn = new DataInputStream(socket.getInputStream());
			BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(path + File.separator + filename) );
			
			log("Writing file to disk...");
			
			while( (bytesRead = dataIn.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0){
				log("Should have read " + Math.min(pageSize, bytesLeft) + " bytes from DataInputStream.");
				log("Read " + bytesRead + " bytes.");
				
				bos.write(buffer, 0, Math.min(pageSize, bytesLeft));
				log("Wrote " + Math.min(pageSize, bytesLeft) + " to disk.");

				bytesLeft -= bytesRead;
				log(bytesLeft + " bytes left to read.");
			}
			
			log("Successfully wrote " + NumberFormat.getNumberInstance(Locale.US).format(fileSize) + " bytes to disk.");

			// don't close input stream here or socket exceptions happen when attempting to use it again.
			bos.close();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	
}
