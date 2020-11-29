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
import java.util.HashMap;

public class FileServer {

	
	public static void main(String[] args) throws IOException {
		HashMap<String, String> files = new HashMap<>();
		String host = "127.0.0.1";
		int port = 54320;
		Socket socket = new Socket(host, port);
		BufferedReader fileServerIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter fileServerOut = new PrintWriter(socket.getOutputStream(), true);
		fileServerOut.println("file server connected");
		// consume the connection message sent by the master server
		System.out.println(fileServerIn.readLine());
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
                    System.out.println("Files being sent: " + sb.toString());
                    fileServerOut.println(sb.toString());
                    files.clear();
                } else {
                    String[] cmdArgs = line.split("~");
                    String socketString = cmdArgs[0];
                    String command = cmdArgs[1];
                    String parameter = cmdArgs[2];
                    switch(command) {
                    case "add":
                        if (!files.containsKey(parameter)) {
                            files.put(parameter, parameter);
                            if (socketString.equals("distributor")) {
                                System.out.printf("%s successfully re-inserted.", parameter);
                            } else {
                                fileServerOut.format("F|%s~%s successfully added to filesystem.%n", socketString, parameter);
                            }
                        } else {
                            if (socketString.equals("distributor")) {
                                System.out.printf("%s already exists in this server.", parameter);
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
}
