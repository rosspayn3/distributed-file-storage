/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
*	
*/
public class UAServer {

	private static ArrayList<SimpleEntry<Socket, PrintWriter>> fileServers = new ArrayList<>();
	private static HashMap<String, PrintWriter> clients = new HashMap<>();
	private static LinkedList<String> messageQueue = new LinkedList<>();
  	private static HashMap<String, String> users = new HashMap<>();
  								   			// v---  filename`server1`server2
	private static HashMap<String, ArrayList<String>> userFiles = new HashMap<>();


    private static int fileCount = 0;
    private static FileDistributor distributor = new FileDistributor();

	public static void main (String[] args) throws Exception {
		// args: portnumber and number of threads


		if (args.length < 2) {
			System.out.println("Invalid syntax:    java UAServer port nthreads");
			System.exit(10);
		}

		int port = Integer.parseInt(args[0]);
		int nthreads = Integer.parseInt(args[1]);

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss.SS");
		System.out.println(dateFormatter.format(LocalDateTime.now()) +" :  Server is now running on port " + port);
		ServerSocket server = new ServerSocket(port);

		// create a thread pool of size nthreads
		ExecutorService threadPool = Executors.newFixedThreadPool(nthreads);

		DispatcherService dispatcher = new DispatcherService();
		dispatcher.start();

        distributor.start();

		while (true) {
			Socket socket = server.accept();
			UAClientThread t = new UAClientThread(socket);
			threadPool.execute(t);
		}
    }

	public static class UAClientThread implements Runnable {

		private Socket socket;
		private String currentUser = "none";

		public UAClientThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
            log("New connection established: " + socket.toString());
            BufferedReader serverIn = null;
            PrintWriter serverOut = null;
			try {
				serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				serverOut = new PrintWriter(socket.getOutputStream(), true);

				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss.SS");
				serverOut.println("Connected to main server on " + dateFormatter.format(LocalDateTime.now()));

				String line;
				while ((line = serverIn.readLine()) != null) {
					
					if (line.contains("connected")) {
						handleConnectionEvent(line, serverOut);
					} else if (line.contains("distribute~")) {
						startFileDistribution(line, socket.getPort());
                    } else {
						parseAndExecuteCommand(line, socket, serverIn, serverOut);
					}
				}

				log(socket.toString() + " Client connection closed.");
				closeConnection(socket, serverIn, serverOut);

			} catch (SocketException ex) {
				log(socket.toString() + " " + ex.getMessage());
				handleSocketException(ex, this.socket, serverIn, serverOut);
            } catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		private void handleConnectionEvent(String eventString, PrintWriter serverOut) {
			if (eventString.equals("file server connected")) {
				log("A file server has connected");
				fileServers.add(new SimpleEntry<>(socket, serverOut));
			} else if (eventString.equals("client connected")) {
				log("A client has connected.");
				clients.put(socket.toString(), serverOut);
			} else {
				log(String.format("Unhandled connection string detected: \"%s\" - cannot process.", eventString));
			}
		}

		private void startFileDistribution(String cmdText, int port) {
			String filename = cmdText.split("~")[1];

			synchronized (distributor.serverResponses) {
				distributor.serverResponses.put(port, filename);
			}
		}

		private void parseAndExecuteCommand(String cmdText, Socket socket, BufferedReader serverIn, PrintWriter serverOut) {
			if (cmdText.contains("F|")) {
				// queue message from file server to client
				synchronized(messageQueue) {
					messageQueue.offer(cmdText);
				}
			} else {
				// handle response from client

				log(String.format("%s :  %s", socket.toString(), cmdText));
				String[] cmdArgs = cmdText.split(" ");

				String command = cmdArgs[0].toLowerCase().trim();
				String parameter = cmdArgs[1].trim();

				switch (command) {
					case "add":
						log("Add command received. Distributing file to servers...");
						// add syntax: add {filename} {file size in bytes}
						distributeFile(socket, this.currentUser, parameter, cmdArgs[2]);
						log("Add command successfully executed!");
						break;
					case "remove":
						removeFile(parameter, cmdArgs[2], serverOut);
						break;
					case "list":
						if (parameter.equalsIgnoreCase("all")) {
							listAllFiles(command, parameter, socket, serverOut);
						} else {
							listUserFiles(cmdArgs[1], serverOut);
						}
						break;
					case "register":
						registerUser(cmdArgs, serverOut);
						break;
					case "login":
						executeLoginCommand(cmdArgs, serverOut);
						break;
					case "whoami":
						serverOut.println("Current user: " + currentUser);
						break;
					default:
						serverOut.println("Invalid command.");
						break;
				}
			}
		}

		private void removeFile(String username, String filename, PrintWriter serverOut) {

			if (fileServers.size() == 0) {
				log("Attempted \"remove\" file operation with no available file servers.");
				serverOut.println("No available file servers.");
				return;
			}

			ArrayList<String> userFileList = getUserFilenames(username);
			String fileInfo = null;

			if (userFileList == null || userFileList.size() == 0) {
				serverOut.printf("Could not find any files belonging to user %s.%n", username);
				return;
			}

			for (int i = 0; i < userFileList.size() && fileInfo == null; i++) {
				String s = userFileList.get(i);
				int tick = s.indexOf('`');
				if (tick != -1 && s.substring(0, tick).equals(filename)) {
					fileInfo = s;
				}
			}

			if (fileInfo == null) {
				serverOut.printf("Could not find a file with name \"%s\" belonging to user %s.%n", filename, username);
				return;
			}

			String[] tokens = fileInfo.split("`");
			int serverId = Integer.parseInt(tokens[1]);
			int backupServerId = Integer.parseInt(tokens[2]);
			var server1 = fileServers.get(serverId);
			var server2 = fileServers.get(backupServerId);

			if (server1 != null) {
				try {
					var fsout = server1.getValue();
					fsout.printf("remove~%s~%s%n", username, filename);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				log(String.format("Attempted to remove file \"%s\" from primary file server %d, " +
						"but server's entry in file server list was null.", filename, serverId));
			}

			if (server2 != null) {
				try {
					var fsout = server2.getValue();
					fsout.printf("remove~%s~%s%n", username, filename);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				log(String.format("Attempted to remove file [%s] from backup file server %d, " +
						"but server's entry in file server list was null.", filename, backupServerId));
			}
		}

		private void listUserFiles(String username, PrintWriter serverOut) {

			if (fileServers.size() == 0) {
				log("Attempted \"list\" file operation with no available file servers.");
				serverOut.println("No available file servers.");
				return;
			}

			ArrayList<String> userFileList = getUserFilenames(username);

			if (userFileList == null || userFileList.size() == 0) {
				serverOut.printf("Could not find any files belonging to user %s.%n", username);
				return;
			}

			for (String fileInfo : userFileList) {
				String[] tokens = fileInfo.split("`");
				int serverId = Integer.parseInt(tokens[1]);
				var server = fileServers.get(serverId);
				if (server != null) {
					try {
						var fsout = server.getValue();
						fsout.printf("list~%s%n", username);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					log(String.format("Attempted to list files for user %s from file server %d, " +
							"but server's entry in file server list was null.", username, serverId));
				}
			}
		}

		private void listAllFiles(String command, String parameter, Socket socket, PrintWriter serverOut) {

			if (fileServers.size() == 0) {
				log("Attempted '" + command + "' file operation with no available file servers.");
				serverOut.println("No available file servers.");
				return;
			}

			for (SimpleEntry<Socket, PrintWriter> entry : fileServers) {
				PrintWriter p = entry.getValue();
				try {
					p.format("%s~%s~%s%n", socket.toString(), command, parameter);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		private void executeLoginCommand(String[] cmdArgs, PrintWriter serverOut) {
			// login syntax: login {username} {password}
			String username = cmdArgs[1];
			String password = cmdArgs[2];

			if (username == null || !users.containsKey(username)) {
				serverOut.println("Invalid username.");
				return;
			}

			log(String.format("Logging in user '%s'...", username));

			if (!validateLogin(username, password)) {
				serverOut.println("Login failed.");
				log("Login failed.");
				return;
			}

			currentUser = username;
			serverOut.println("Login successful.");
			log("Login successful.");
		}

		private void handleSocketException(SocketException ex, Socket socket, BufferedReader serverIn, PrintWriter serverOut) {

            if (ex.getMessage().equalsIgnoreCase("connection reset")) {
                try {
					closeConnection(socket, serverIn, serverOut);
				} catch (IOException e) {
					e.printStackTrace();
				}
                synchronized(fileServers) {
                	for(int i = 0; i < fileServers.size(); i++) {
                    	if(fileServers.get(i).getKey() == socket) {
                    		redistribute(socket);
                    	}
                    }
                }
            }
		}

		private void closeConnection(Socket socket, BufferedReader serverIn, PrintWriter serverOut) throws IOException {

			try {
                if (serverOut != null)
                    serverOut.close();
                if (serverIn != null)
                    serverIn.close();
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

		}

	}

	private static void redistribute(Socket socket) {

		int i = 0;
        while (i < fileServers.size()) {
            SimpleEntry<Socket, PrintWriter> entry = fileServers.get(i);
            if (entry.getKey().equals(socket)) {
                log("Found disconnected server.");
                synchronized(fileServers) {
                	 fileServers.remove(i);
                }
            }
            i++;
        }
        for(int j = 0; j < fileServers.size(); j++) {
        	fileServers.get(j).getValue().println("distribute");
        }

	}

	private static void log(String message) {

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss.SS");
		System.out.println(dateFormatter.format(LocalDateTime.now()) + " :  " + message);

	}



	private static void registerUser(String[] cmdArgs, PrintWriter serverOut) {

		String username = cmdArgs[1];
		String password = cmdArgs[2];

		log(String.format("Registering new user '%s'...", username));

		try {
			users.put(username, password);
			userFiles.put(username, new ArrayList<String>());
			serverOut.println("Register successful.");
			log("Register successful.");
		} catch(Exception e) {
			e.printStackTrace();
			serverOut.println("Register failed.");
			log("Register failed.");
		}

	}

	private static boolean validateLogin(String username, String password) {

		if(users.get(username).equals(password)) {
			return true;
		} else {
			return false;
		}

	}
	
	private static ArrayList<String> getUserFilenames(String username){
		return userFiles.get(username);
	}

	private static byte[] receiveFile(Socket s){

		try {
			int fourKBpage = 4096;
			byte[] b = new byte[fourKBpage];

			DataInputStream in = new DataInputStream(s.getInputStream());
			
			int bytesRead = 0;
			int offset = 0;
			int fileSizeToReceive = 50000;

			offset = fileSizeToReceive;

			while( (bytesRead = in.read(b, 0, Math.min(fourKBpage, offset))) > 0){
				
				// write bytes to file in memory

				offset -= bytesRead;
			}

			return b;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private static boolean distributeFile(Socket clientSocket, String user, String filename, String size){

		log("============= Distributing file to file servers =============");
		
		try {
			
			int pageSize = 4096;
			byte[] buffer = new byte[pageSize];

			DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());

			int bytesRead = 0;
			int bytesLeft = Integer.parseInt(size);
			log("Size of file about to receive: " + bytesLeft);

			int[] servers = getServerIndices(filename);
			DataOutputStream server1 = new DataOutputStream(fileServers.get(servers[0]).getKey().getOutputStream());
			DataOutputStream server2 = new DataOutputStream(fileServers.get(servers[1]).getKey().getOutputStream());
			
			// buffered output stream for saving file locally for testing
			// directory needs to exist locally before this will work.
			// need to write code for creating a directory if it doesn't exist.
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("C:\\upload\\received\\"+"\\"+user+"\\"+filename));
			
			log("Sending bytes...");
			
			//**********************************************************************************************
			// files are getting padded with null bytes and corrupted when written to disk here. not as
			// bad with a smaller page size. probably at the end of the buffer. doesn't happen when they're 
			// written to disk in  FileUploadServlet, so something is weird with this loop even though 
			// they're the exact same. could be something to do with reading from DataInputStream instead of 
			// BufferedInputStream?
			//
			// also something weird is happening with very small files. with a 2104 byte file, dataIn.read()
			// should return 2104 to bytesRead at line 405. it only returns 1024 bytes, but then writes 2104
			// to file. Math.min(pageSize, bytesLeft) is giving two different values at line 405 and 406.
			// then it loops through again and reads the remaining 1080 bytes and writes 1080 bytes to file
			// like it should. the first loop through is acting weird.
			//**********************************************************************************************
			while( (bytesRead = dataIn.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0){
				log("Should have read " + Math.min(pageSize, bytesLeft) + " bytes.");
				log("Read " + bytesRead + " bytes.");
				
				// write file to local disk for testing until we get file servers to accept file transfers
				bos.write(buffer, 0, Math.min(pageSize, bytesLeft));
				log("Wrote " + Math.min(pageSize, bytesLeft) + " to file.");
				
				//*********************************************************
				// file servers explode when being sent unexpected things.
				// their sockets also close when an exception is thrown.
				// probably need to send them the 'add' command so they
				// can start reading their input stream for files like this
				// server does. get all bytes from input stream in their
				// main thread, then start a new thread that writes those 
				// bytes to disk.
				//*********************************************************
				// send file to file server DataOutputStreams
//				server1.write(b);
//				server2.write(b);
				
				bytesLeft -= bytesRead;
				log(bytesLeft + " bytes left to read.");
			}
			
			log("Successfully sent " + size + " bytes to each file server.");
			
			fileCount++;
			userFiles.get(user).add(filename + "`" + servers[0] + "`" + servers[1]);

			//*************************************************************************
			// don't think we can close this as it's the client thread's input stream.
			// probably can't close the file server output streams either.
			//*************************************************************************
//			dataIn.close();
			
			// close output stream that writes to local disk
			bos.close();
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}



	}

	private static boolean sendFile(Socket s, UAFile file){
		try{
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());

			int fourKBpage = 4096;
			byte[] b = new byte[fourKBpage];

			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.getFile()));

			while(  bis.read(b) > 0){
				dos.write(b);
			}

			dos.close();
			bis.close();

			return true;
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}

	}

    private static int[] getServerIndices(String filename) {
		
		var reversedFilename = new StringBuilder();
		
		for (int i = filename.length() - 1; i >= 0; i--) {
            reversedFilename.append(filename.charAt(i));
        }
		
		int i1 = Math.abs(filename.hashCode() % fileServers.size());
        int i2 = Math.abs(reversedFilename.toString().hashCode() % fileServers.size());
		
		if (i1 == i2) {
            i2++;
            if (i2 >= fileServers.size()) {
                i2 = 0;
            }
        }
		
		return new int[] {i1, i2};
    }

	private static class DispatcherService extends Thread {

		@Override
		public void run() {
			while (true) {

				while(messageQueue.size() > 0) {
					String message = messageQueue.poll();
					if (message != null && message.contains("F|")) {
                        // handle message from file server
                        if (message.contains("already exists in filesystem")
                            || message.contains("successfully removed from filesystem")) {
                            fileCount--;
                        }
						String content = message.substring(2);
						log("Dispatcher Service: " + content);
						String[] tokens = content.split("~");
						String socketString = tokens[0];
						String transmission = tokens[1];
						PrintWriter clientOut = clients.get(socketString);
						if (clientOut != null) {
							clientOut.println(transmission);
						}
					} else {
						log(message);
					}
				}

				try {
					sleep(500);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}

    }

    private static class FileDistributor extends Thread {
        private HashMap<Integer, String> serverResponses = new HashMap<>();

        @Override
        public void run() {
            while (true) {
                if (serverResponses.size() == fileServers.size()) {
                    HashMap<String, String> files = new HashMap<>();
                    for (String entry : serverResponses.values()) {
                        String[] filenames = entry.split("[|]");
                        for (String name : filenames) {
                            System.out.println(name);
                        }
                        for (String name : filenames) {
                            if (!files.containsKey(name)) {
                                files.put(name, name);
                            }
                        }
                    }
                    for (String file : files.values()) {
                        int[] indices = getServerIndices(file);
                        var dest1 = fileServers.get(indices[0]);
                        var dest2 = fileServers.get(indices[1]);
                        dest1.getValue().printf("distributor~add~%s%n", file);
                        dest2.getValue().printf("distributor~add~%s%n", file);
                    }
                    serverResponses.clear();
                } else {
                    try {
                        sleep(500);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        }
    }


}
