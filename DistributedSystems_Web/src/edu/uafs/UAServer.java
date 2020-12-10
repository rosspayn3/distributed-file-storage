/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.NumberFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 
 *  The main server of the LionDB distributed file storage system.
 *  <p>
 *  When started from a command line, syntax is {@code java UAServer [listen port] [# threads]}.
 *
 */
public class UAServer {

	/**
	 * An {@link ArrayList} of currently connected {@link FileServer} nodes.
	 */
	private static ArrayList<Server> fileServers = new ArrayList<>();
	
	/**
	 * 
	 * 
	 */
	private static HashMap<Integer, Server> serverMap = new HashMap<>();
	
	/**
	 * A {@link HashMap} containing currently connected clients and their output streams.
	 */
	private static HashMap<String, DataOutputStream> clients = new HashMap<>();
	
	/**
	 * 
	 * 
	 */
	private static LinkedList<String> messageQueue = new LinkedList<>();
	
	/**
	 * A {@link HashMap} containing usernames and passwords for login.
	 */
  	private static HashMap<String, String> userLogins = new HashMap<>();
  	
  	/**
  	 * A {@link HashMap} containing file metadata such as file name and which {@link FileServer} the file 
  	 * is located on.
  	 */
	private static HashMap<String, ArrayList<String>> userFiles = new HashMap<>();

	/**
	 * An integer that represents the total number of files added since the start of an 
	 * instance of {@link UAServer}. This count is not decremented when a file is removed,
	 * so it functions much like a primary key in a traditional DBMS.
	 */
    private static int fileCount = 0;

	public static void main (String[] args) throws Exception {

		if (args.length < 2) {
			System.out.println("Invalid syntax:    java UAServer [listen port] [# threads]");
			System.exit(10);
		}

		int port = Integer.parseInt(args[0]);
		int nthreads = Integer.parseInt(args[1]);

		Logger.print("MAIN SERVER", "Server is now running on port " + port);
		ServerSocket server = new ServerSocket(port);

		// create a thread pool of size nthreads
		ExecutorService threadPool = Executors.newFixedThreadPool(nthreads);

		DispatcherService dispatcher = new DispatcherService();
		dispatcher.start();

		while (true) {
			Socket socket = server.accept();
			UAClientThread t = new UAClientThread(socket);
			threadPool.execute(t);
		}
    }
	
	
	/**
	 * 
	 * A thread that handles client interaction with UAServer. A {@link Socket} connection
	 * is maintained with the client, along with a username to aid in storing, listing, 
	 * and removing files for a specific user.
	 *
	 */
	public static class UAClientThread implements Runnable {

		/**
		 * The socket connection between a UAClientThread and a client application.
		 */
		private Socket socket;
		
		/**
		 * The current user of the client. The username is given by the client when it connects.
		 */
		private String currentUser = "none";

		/**
		 * Default constructor for {@link UAClientThread}.
		 * 
		 * @param socket	The {@link Socket} of the incoming client.
		 */
		public UAClientThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
            Logger.log("MAIN SERVER", "New connection established: " + socket.toString());
            BufferedReader commandReader = null;
            BufferedInputStream serverIn = null;
            DataOutputStream serverOut = null;
			try {
				commandReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				serverIn = new BufferedInputStream(socket.getInputStream());
				serverOut = new DataOutputStream(socket.getOutputStream());

				String line;
				while ((line = commandReader.readLine()) != null) {
					
					if (line.contains("connected")) {
						handleConnectionEvent(line, serverIn, serverOut);
					} else if (line.contains("backup")) {
						backupFile(commandReader, serverIn);
                    } else {
						parseAndExecuteCommand(line, socket, serverIn, serverOut);
					}
				}

				Logger.log("MAIN SERVER", socket.toString() + " Client connection closed.");
				closeConnection(socket, serverIn, serverOut);

			} catch (SocketException ex) {
				Logger.log("MAIN SERVER", socket.toString() + " " + ex.getMessage());
				handleSocketException(ex, this.socket, serverIn, serverOut);
            } catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/**
		 * Handles incoming connections from a {@link WebClient} or a {@link FileServer}.
		 * 
		 * @param eventString	Message that identifies a client or server that is connecting.
		 * @param serverIn	The {@link BufferedInputStream} of the incoming {@link Socket} connection. 
		 * @param serverOut	The {@link DataOutputStream} of the incoming {@link Socket} connection. 
		 */
		private void handleConnectionEvent(String eventString, BufferedInputStream serverIn, DataOutputStream serverOut) {
			if (eventString.equals("file server connected")) {
				Logger.log("MAIN SERVER", "A file server has connected");
				Server s = new Server(socket, serverIn, serverOut);
				fileServers.add(s);
				serverMap.put(s.id, s);
			} else if (eventString.equals("client connected")) {
				Logger.log("MAIN SERVER", "A client has connected.");
				clients.put(socket.toString(), serverOut);
			} else {
				Logger.log("MAIN SERVER", String.format("Unhandled connection string detected: \"%s\" - cannot process.", eventString));
			}
		}
		
		/**
		 * 
		 * 
		 * 
		 */
		private void backupFile(BufferedReader r, BufferedInputStream in) throws Exception {
			int destination = Integer.parseInt(r.readLine());
			int fileSize = Integer.parseInt(r.readLine());
			String filename = r.readLine();
			String username = r.readLine();
			Server server = fileServers.get(destination);
			Exception exception = null;
			
			try {
				server.lock();
				server.sendCommand("add");
				server.sendCommand(String.format("%s %s %d", username, filename, fileSize));
				int pageSize = 4096;
				byte[] buffer = new byte[pageSize];
				int bytesRead = 0;
				int bytesLeft = fileSize;
				while (bytesLeft > 0 && (bytesRead = in.read(buffer)) > 0) {
					server.sendBytes(buffer, 0, Math.min(pageSize, bytesLeft));
					bytesLeft -= bytesRead;
				}
				Logger.log("MAIN SERVER", String.format("Successfully backed up file [%s] to server #%d.", filename, destination));
			} catch (Exception ex) {
				exception = ex;
			} finally {
				server.unlock();
			}
			
			if (exception != null) {
				throw exception;
			}
		}

		/**
		 * Receives and executes a command from a {@link WebClient} or {@link FileServer}.
		 * 
		 * @param cmdText	The String that details the command and its parameters. 
		 * @param socket	The {@link Socket} the command was received on.
		 * @param serverIn	The {@link BufferedInputStream} of the {@link UAClientThread} that received the command.
		 * @param serverOut	The {@link DataOutputStream} of the {@link UAClientThread} that received the command.
		 */
		private void parseAndExecuteCommand(String cmdText, Socket socket, BufferedInputStream serverIn, DataOutputStream serverOut) {
			if (cmdText.contains("F|")) {
				// queue message from file server to client
				synchronized(messageQueue) {
					messageQueue.offer(cmdText);
				}
			} else {
				// handle response from client

				Logger.log("MAIN SERVER", String.format("%s :  %s", socket.toString(), cmdText));
				String[] cmdArgs = cmdText.split(" ");

				String command = cmdArgs[0].toLowerCase().trim();
				String parameter = cmdArgs[1].trim();

				switch (command) {
					case "add":
						Logger.log("MAIN SERVER", "Add command received. Distributing file to servers...");
						// add syntax: add {filename} {file size in bytes}
						try {
							if(fileServers.size() == 0) {
								serverOut.writeBytes("no available file servers.\n");
								serverOut.flush();
								break;
							} else {
								serverOut.writeBytes("accepted.\n");
								serverOut.flush();
								distributeFile(serverIn, this.currentUser, parameter, cmdArgs[2]);
							}
						} catch (Exception ex) {
							Logger.log("MAIN SERVER", "Exception thrown while distributing file.");
						}
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
						try {
							serverOut.writeBytes(String.format("Current user: %s%n", currentUser));
							serverOut.flush();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
						break;
					default:
						try {
							serverOut.writeBytes("Invalid command.");
							serverOut.flush();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
						break;
				}
			}
		}

		/**
		 * Sends a command to remove a file to each {@link FileServer} node it is located on, then removes the file 
		 * from the metadata stored in {@link UAServer}.
		 * <p>
		 * This method first sends a confirmation or error message to the {@link WebClient} in response to the command.
		 * 
		 * @param username	The user that is removing the file.
		 * @param filename	The name of the file to be removed.
		 * @param serverOut	The {@link DataOutputStream} of the {@link UAClientThread} that received the command.
		 */
		private void removeFile(String username, String filename, DataOutputStream serverOut) {

			if (fileServers.size() == 0) {
				Logger.log("MAIN SERVER", "Attempted \"remove\" file operation with no available file servers.");
				try {
					serverOut.writeBytes("No available file servers.\n");
					serverOut.flush();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				return;
			}

			ArrayList<String> userFileList = getUserFilenames(username);
			String fileInfo = null;

			if (userFileList == null || userFileList.size() == 0) {
				try {
					serverOut.writeBytes(String.format("Could not find any files belonging to user %s.%n", username));
					serverOut.flush();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				return;
			}
			
			int fileIndex = -1;
			
			for (int i = 0; i < userFileList.size() && fileInfo == null; i++) {
				String s = userFileList.get(i);
				int tick = s.indexOf('`');
				if (tick != -1 && s.substring(0, tick).equals(filename)) {
					fileInfo = s;
					fileIndex = i;
				}
			}

			if (fileInfo == null) {
				try {
					serverOut.writeBytes(String.format("Could not find a file with name \"%s\" belonging to user %s.%n", filename, username));
					serverOut.flush();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				return;
			}
			
			// send response to client
			try {
				serverOut.writeBytes("removing file\n");
				serverOut.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			String[] tokens = fileInfo.split("`");
			int serverId = Integer.parseInt(tokens[1]);
			int backupServerId = Integer.parseInt(tokens[2]);
			var server1 = fileServers.get(serverId);
			var server2 = fileServers.get(backupServerId);

			if (server1 != null) {
				try {
					server1.lock();
					server1.sendCommand("remove");
					Thread.sleep(200);
					server1.sendCommand(String.format("%s:%s\n", username, filename));
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					server1.unlock();
				}
			} else {
				Logger.log("MAIN SERVER", String.format("Attempted to remove file \"%s\" from primary file server %d, " +
						"but server's entry in file server list was null.", filename, serverId));
			}

			if (server2 != null) {
				try {
					server2.lock();
					server2.sendCommand("remove");
					Thread.sleep(200);
					server2.sendCommand(String.format("%s:%s\n", username, filename));
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					server2.unlock();
				}
			} else {
				Logger.log("MAIN SERVER", String.format("Attempted to remove file [%s] from backup file server %d, " +
						"but server's entry in file server list was null.", filename, backupServerId));
			}
			
			userFiles.get(username).remove(fileIndex);
			
		}

		/**
		 * Sends a list of the user's files to the {@link WebClient} that sent the command. File names are sent one
		 * line at a time. 
		 * <p>
		 * This method first sends a confirmation or error message to the {@link WebClient} in response to the command.
		 * 
		 * @param username
		 * @param serverOut
		 */
		private void listUserFiles(String username, DataOutputStream serverOut) {

			if (fileServers.size() == 0) {
				Logger.log("MAIN SERVER", "Attempted \"list\" file operation with no available file servers.");
				try {
					serverOut.writeBytes("No available file servers.\n");
					serverOut.flush();
				}  catch (IOException ex) {
					ex.printStackTrace();
				}
				return;
			}

			var userFileList = getUserFilenames(username);

			if (userFileList == null || userFileList.size() == 0) {
				try {
					serverOut.writeBytes(String.format("Could not find any files belonging to user %s.%n", username));
					serverOut.flush();
				}  catch (IOException ex) {
					ex.printStackTrace();
				}
				return;
			}
			
			// send response to client
			
			try {
				Logger.log("MAIN SERVER", "Listing filenames for user " + username + "...");
				serverOut.writeBytes("listing filenames\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			for (String fileInfo : userFileList) {
				String[] tokens = fileInfo.split("`");
				String filename = tokens[0];

				try {
					serverOut.writeBytes(filename + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try {
				serverOut.writeBytes("done\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		/**
		 * [NOT USED] 
		 * 
		 * This method sends a command to each currently connected {@link FileServer} node to list all files.
		 * 
		 * @param command
		 * @param parameter
		 * @param socket
		 * @param serverOut
		 */
		private void listAllFiles(String command, String parameter, Socket socket, DataOutputStream serverOut) {

			if (fileServers.size() == 0) {
				Logger.log("MAIN SERVER", "Attempted '" + command + "' file operation with no available file servers.");
				try {
					serverOut.writeBytes("No available file servers.\n");
					serverOut.flush();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				return;
			}

			for (var server : fileServers) {
				try {
					server.lock();
					server.sendCommand(String.format("%s~%s~%s%n", socket.toString(), command, parameter));
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					server.unlock();
				}
			}
		}

		/**
		 * Handles user login.
		 * <p>
		 * This method sends a confirmation or error message to the {@link WebClient} in response to the command.
		 * 
		 * @param cmdArgs	A username and password.
		 * @param serverOut	The {@link DataOutputStream} of the {@link UAClientThread} that received the command.
		 */
		private void executeLoginCommand(String[] cmdArgs, DataOutputStream serverOut) {
			// login syntax: login {username} {password}
			
			if (cmdArgs == null || cmdArgs.length < 2) {
				return;
			}
			
			String username = cmdArgs[1];
			String password = cmdArgs[2];

			if (username == null || !userLogins.containsKey(username)) {
				try {
					serverOut.writeBytes("Invalid username.\n");
					serverOut.flush();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				return;
			}

			Logger.log("MAIN SERVER", String.format("Logging in user '%s'...", username));

			if (!validateLogin(username, password)) {
				try {
					serverOut.writeBytes("Login failed.\n");
					serverOut.flush();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				Logger.log("MAIN SERVER", "Login failed.");
				return;
			}

			currentUser = username;
			try {
				serverOut.writeBytes("Login successful.\n");
				serverOut.flush();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			Logger.log("MAIN SERVER", "Login successful.");
		}

		/**
		 * Handles exceptions thrown by a {@link UAClientThread}'s {@link Socket}. The socket is closed inside this 
		 * method. If the {@link Socket} was that of a {@link FileServer}'s {@link UAClientThread}, file redistribution 
		 * occurs in order to prevent the loss of the last copy of a file.
		 * 
		 * @param ex	The {@link SocketException} that was thrown.
		 * @param socket	The {@link Socket} that threw the exception.
		 * @param serverIn	The {@link BufferedInputStream} of the {@link UAClientThread} that threw the exception.
		 * @param serverOut	The {@link DataOutputStream} of the {@link UAClientThread} that threw the exception.
		 */
		private void handleSocketException(SocketException ex, Socket socket, BufferedInputStream serverIn, DataOutputStream serverOut) {

            if (ex.getMessage().equalsIgnoreCase("connection reset")) {
				closeConnection(socket, serverIn, serverOut);
				redistribute(socket);
            } else {
            	ex.printStackTrace();
            }
		}

		/**
		 * Closes the input stream, output stream, and {@link Socket} of a {@link UAClientThread}.
		 * 
		 * @param socket	The {@link Socket} to be closed.
		 * @param serverIn	The {@link BufferedInputStream} of the {@link Socket} to be closed.
		 * @param serverOut	The {@link DataOutputStream} of the {@link Socket} to be closed.
		 */
		private void closeConnection(Socket socket, BufferedInputStream serverIn, DataOutputStream serverOut) {

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

	/**
	 * 
	 * 
	 * 
	 */
	private static void redistribute(Socket socket) {

		int index = 0;
		Server failedServer = null;
		
        while (index < fileServers.size() && failedServer == null) {
            if (fileServers.get(index).socket.equals(socket)) {
                Logger.log("MAIN SERVER", "Found disconnected server.");
                synchronized(fileServers) {
                	 failedServer = fileServers.remove(index);
                }
            }
            index++;
        }
        
        if (failedServer == null) {
        	Logger.log("MAIN SERVER", "Failed to find disconnected server.");
        	return;
        }
        
        var fileList = failedServer.list();
        
        if (fileList == null || fileList.size() == 0) {
        	Logger.log("MAIN SERVER", "Disconnected server did not contain any files.");
        	return;
        }
        
        var backupLocations = new HashMap<String, SimpleEntry<String, Integer>>();
        
        for (String info : fileList) {
        	String[] tokens = info.split("`");
        	String filename = tokens[0];
        	String username = tokens[1];
        	int s1 = Integer.parseInt(tokens[2]);
        	int s2 = Integer.parseInt(tokens[3]);
        	if (s1 != failedServer.id) {
        		backupLocations.put(filename, new SimpleEntry<>(username, s1));
        	} else {
        		backupLocations.put(filename, new SimpleEntry<>(username, s2));
        	}
        }
        
        var rng = new Random();
        for (var entry : backupLocations.entrySet()) {
        	
        	String filename = entry.getKey();
        	String username = entry.getValue().getKey();
        	int serverIndex = entry.getValue().getValue();
        	
        	var server = serverMap.get(serverIndex);
        	int originalLocation = -1;
        	for (int i = 0; i < fileServers.size() && originalLocation == -1; i++) {
        		var s = fileServers.get(i);
        		if (s.id == server.id) {
        			originalLocation = i;
        		}
        	}
        	
        	if (originalLocation == -1) {
        		Logger.log("MAIN SERVER", "Failed to locate server with backup file copies for redistribution.");
        		return;
        	}
        	
        	int backupLocation;
        	
        	do {
        		backupLocation = rng.nextInt(fileServers.size());
        	} while (backupLocation == originalLocation);
        	
        	try {
        		server.lock();
        		server.sendCommand("backup\n");
        		server.sendCommand(String.format("%s %s %d%n", filename, username, backupLocation));
        	} catch (IOException ex) {
        		Logger.log("MAIN SERVER", String.format("Exception raised while attempting to retrieve files from backup server after server failure.%nDetails:%n%s", ex.getMessage()));
        	} finally {
        		server.unlock();
        	}
        }
	}


	/**
	 * Stores credentials for a new user in the current {@link UAServer}. A username cannot be registered if it already 
	 * exists in the current instance of {@link UAServer}.
	 * <p>
	 * This method sends a success or error message to the {@link WebClient} in response to the command.
	 * 
	 * @param cmdArgs	The username and password of the new user.
	 * @param serverOut	The {@link DataOutputStream} of the {@link UAClientThread} that received the command.
	 */
	private static void registerUser(String[] cmdArgs, DataOutputStream serverOut) {

		String username = cmdArgs[1];
		String password = cmdArgs[2];

		Logger.log("MAIN SERVER", String.format("Registering new user '%s'...", username));

		try {
			userLogins.put(username, password);
			userFiles.put(username, new ArrayList<String>());
			serverOut.writeBytes("Register successful.\n");
			serverOut.flush();
			Logger.log("MAIN SERVER", "Register successful.");
		} catch(Exception e) {
			e.printStackTrace();
			try {
				serverOut.writeBytes("Register failed.\n");
				serverOut.flush();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			Logger.log("MAIN SERVER", "Register failed.");
		}

	}

	/**
	 * Validates login credentials.
	 * 
	 * @param username The username sent by the {@link WebClient}.
	 * @param password	The password sent by the {@link WebClient}.
	 * @return	A boolean value indicating a successful validation of credentials.
	 */
	private static boolean validateLogin(String username, String password) {

		if(userLogins.get(username).equals(password)) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Returns the metadata for a user's files.
	 * 
	 * @param username	The username of a user.
	 * @return	An {@link ArrayList} of the user's file metadata.
	 */
	private static ArrayList<String> getUserFilenames(String username){
		return userFiles.get(username);
	}

	/**
	 * Reads a file's bytes in from a {@link UAClientThread}'s {@link BufferedInputStream}, then sends 
	 * those bytes to one or more {@link FileServer} nodes where they are written to disk for permanent storage. 
	 * If more than one {@link FileServer} node is available, a backup copy of the file is sent to a different 
	 * {@link FileServer} node. The nodes are selected in such a way that files are distributed evenly among 
	 * the available nodes.
	 *  
	 * @param dataIn	The {@link BufferedInputStream} to read a file from.
	 * @param user		The user stored in the {@link UAClientThread} that is receiving the file.
	 * @param filename	The name of the file to receive and distribute.
	 * @param size		The size of the file, in bytes, to receive and distribute.
	 * @throws IOException	When an exception occurs when reading a file's bytes from the {@link BufferedInputStream}.
	 */
	private static void distributeFile(BufferedInputStream dataIn, String user, String filename, String size) throws IOException  {

		Logger.log("MAIN SERVER", "============= Distributing file to file servers =============");
			
		int fileSize = Integer.parseInt(size);

		// ******************************************
		// WRITE FILE LOCALLY TO DISK FOR EMERGENCY
		// ******************************************
		
//		int pageSize = 4096;
//		byte[] buffer = new byte[pageSize];
//		int bytesRead = 0;
//		int bytesLeft = fileSize;
//
//		File userDir = new File("files" + File.separator + user);
//		
//		if(!userDir.exists()) {
//			userDir.mkdirs();
//		}
//		
//		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(userDir + File.separator +filename));
//		
//		while( (bytesRead = dataIn.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0){
//			bos.write(buffer, 0, Math.min(pageSize, bytesLeft));
//			bytesLeft -= bytesRead;
//		}
//		
//		bos.close();
		
		// ******************************************
		// END WRITE FILE LOCALLY TO DISK
		// ******************************************
		
		
		int[] servers = getServerIndices();
		var server1 = fileServers.get(servers[0]);
		var server2 = fileServers.get(servers[1]);
		String fileInfo = String.format("%s`%s`%d`%d", filename, user, server1.id, server2.id);

		transferFile(dataIn, user, filename, fileInfo, fileSize, server1, server2);
		
//		ArrayList<Byte> data = readAllBytes(dataIn, fileSize);
		
//		Logger.log("MAIN SERVER", "Sending bytes to server 1...");
		
//		server1.lock();
//		server1.sendCommand("add\n");
//		server1.sendCommand(String.format("%s %s %s%n", user, filename, size));
//		server1.sendBytes(data);
//		server1.add(fileInfo);
//		server1.unlock();
		
//		Logger.log("MAIN SERVER", "Sending bytes to server 2...");

//		server2.lock();
//		server2.sendCommand("add\n");
//		server2.sendCommand(String.format("%s %s %s\n", user, filename, size));
//		server2.sendBytes(data);
//		server2.add(fileInfo);
//		server2.unlock();
		
		Logger.log("MAIN SERVER", String.format("Distributed file [%s].", filename));
		fileCount++;
		userFiles.get(user).add(filename + "`" + servers[0] + "`" + servers[1]);
		
	}
	
	synchronized private static void transferFile(BufferedInputStream in, String user, String filename,  String fileInfo, int fileSize, Server server1, Server server2) throws IOException {
		
//		server1.lock();
		server1.sendCommand("add\n");
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		server1.sendCommand(String.format("%s %s %s\n", user, filename, fileSize));
		
//		server2.lock();
		server2.sendCommand("add\n");
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		server2.sendCommand(String.format("%s %s %s\n", user, filename, fileSize));
		
		int pageSize = 4096;
		byte[] buffer = new byte[pageSize];
		int bytesRead = 0;
		int bytesLeft = fileSize;
		int bytesSent = 0;
		
		Logger.log("MAIN SERVER", String.format("Bytes to read: [%d]", bytesLeft));
		
		while((bytesRead = in.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0) {
			server1.sendBytes(buffer, 0, Math.min(pageSize, bytesLeft));
			server2.sendBytes(buffer, 0, Math.min(pageSize, bytesLeft));
			bytesSent += Math.min(pageSize, bytesLeft);
			bytesLeft -= bytesRead;
		}
		
		Logger.log("MAIN SERVER", String.format("Total bytes sent: [%d]", bytesSent));
		
		server1.add(fileInfo);
//		server1.unlock();
		
		server2.add(fileInfo);
//		server2.unlock();
	}

	/**
	 * Gets the index of the servers on which to store an original and backup copy of a file.
	 * The method assumes that servers are started in alternating fashion, which will lead
	 * to files being evenly distributed across all virtual servers on all machines.
	 * 
	 * @return An array containing the indices of the primary and backup file-storage servers.
	 */
    private static int[] getServerIndices() {
    	
    	int server1 = fileCount % fileServers.size();
    	int server2 = server1 + 1;
    	
    	if (server2 >= fileServers.size()) {
    		server2 = 0;
    	}
		
		return new int[] {server1, server2};
    }
    
    /**
     * Generates two random numbers representing the servers on which to store the original
     * and backup copies of a file.
     * 
     * @return An array containing the indices of the primary and backup file-storage servers.
     */
    private static int[] getRandomServerIndices() {
    	var rng = new java.util.Random();
    	int server1 = rng.nextInt(fileServers.size());
    	int server2;
    	do {
    		server2 = rng.nextInt(fileServers.size());
    	} while (server2 == server1);
    	return new int[] {server1, server2};
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
						Logger.log("MAIN SERVER", "Dispatcher Service: " + content);
						String[] tokens = content.split("~");
						String socketString = tokens[0];
						String transmission = tokens[1];
						var clientOut = clients.get(socketString);
						if (clientOut != null) {
							try {
								clientOut.writeBytes(String.format("%s%n", transmission));
								clientOut.flush();
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					} else {
						Logger.log("MAIN SERVER", message);
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

    private static class Server {
    	
    	int id;
    	Socket socket;
    	BufferedInputStream in;
    	DataOutputStream out;
    	HashMap<String, String> files = new HashMap<>();
    	final ReentrantLock SERVER_LOCK = new ReentrantLock();
    	
    	Server(Socket socket, BufferedInputStream in, DataOutputStream out) {
    		this.socket = socket;
    		this.in = in;
    		this.out = out;
    		id = socket.getPort();
    	}
    	
    	void add(String filename) {
    			files.put(filename, filename);
    	}
    	
    	void remove(String filename) {
    			files.remove(filename);
    	}
    	
    	ArrayList<String> list() {
    		return new ArrayList<>(files.values());
    	}
    	
    	void shutdown() throws IOException {
			socket.close();
    	}
    	
    	void sendCommand(String command) throws IOException {
    		
    		if (command != null && command.length() > 0) {
    			if (command.charAt(command.length() - 1) != '\n') {
    				command += '\n';
    			}
	    			out.writeBytes(command);
//	    			out.flush();
    		}
    	}
    	
    	void sendBytes(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
			out.flush();
    	}
    	
    	void lock() {
    		SERVER_LOCK.lock();
    	}
    	
    	void unlock() {
    		SERVER_LOCK.unlock();
    	}
    	
    }
}
