/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
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

/*
*	
*/
public class UAServer {

	private static ArrayList<Server> fileServers = new ArrayList<>();
	private static HashMap<Integer, Server> serverMap = new HashMap<>();
	private static HashMap<String, DataOutputStream> clients = new HashMap<>();
	private static LinkedList<String> messageQueue = new LinkedList<>();
  	private static HashMap<String, String> users = new HashMap<>();
	private static HashMap<String, ArrayList<String>> userFiles = new HashMap<>();

    private static int fileCount = 0;

	public static void main (String[] args) throws Exception {
		// args: portnumber and number of threads


		if (args.length < 2) {
			System.out.println("Invalid syntax:    java UAServer port nthreads");
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

	public static class UAClientThread implements Runnable {

		private Socket socket;
		private String currentUser = "none";

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
								Logger.log("MAIN SERVER", "Add command successfully executed!");
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
					server1.sendCommand(String.format("remove~%s~%s%n", username, filename));
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
					server2.sendCommand(String.format("remove~%s~%s%n", username, filename));
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

		private void executeLoginCommand(String[] cmdArgs, DataOutputStream serverOut) {
			// login syntax: login {username} {password}
			
			if (cmdArgs == null || cmdArgs.length < 2) {
				return;
			}
			
			String username = cmdArgs[1];
			String password = cmdArgs[2];

			if (username == null || !users.containsKey(username)) {
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

		private void handleSocketException(SocketException ex, Socket socket, BufferedInputStream serverIn, DataOutputStream serverOut) {

            if (ex.getMessage().equalsIgnoreCase("connection reset")) {
				closeConnection(socket, serverIn, serverOut);
				redistribute(socket);
            } else {
            	ex.printStackTrace();
            }
		}

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



	private static void registerUser(String[] cmdArgs, DataOutputStream serverOut) {

		String username = cmdArgs[1];
		String password = cmdArgs[2];

		Logger.log("MAIN SERVER", String.format("Registering new user '%s'...", username));

		try {
			users.put(username, password);
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

	private static void distributeFile(BufferedInputStream dataIn, String user, String filename, String size) throws Exception {

		Logger.log("MAIN SERVER", "============= Distributing file to file servers =============");
			
		int fileSize = Integer.parseInt(size);
		Logger.log("MAIN SERVER", "Size of file about to receive: " + fileSize);
				
		int[] servers = getServerIndices();
		var server1 = fileServers.get(servers[0]);
		var server2 = fileServers.get(servers[1]);
		
//		File filesDir = new File("files" + File.separator + user);
		
//		if(!filesDir.exists()) {
//			filesDir.mkdirs();
//		}
		
		String fileInfo = String.format("%s`%s`%d`%d", filename, user, server1.id, server2.id);
		
		ArrayList<Byte> data = readAllBytes(dataIn, fileSize);
		
		Logger.log("MAIN SERVER", "Sending bytes...");
		
		server1.lock();
		server1.sendCommand("add\n");
		server1.sendCommand(String.format("%s %s %s\n", user, filename, size));
		server1.sendBytes(data);
		server1.add(fileInfo);
		server1.unlock();
		
		server2.lock();
		server2.sendCommand("add\n");
		server2.sendCommand(String.format("%s %s %s\n", user, filename, size));
		server2.sendBytes(data);
		server2.add(fileInfo);
		server2.unlock();
		
		Logger.log("MAIN SERVER", "Successfully sent " + NumberFormat.getNumberInstance(Locale.US).format(fileSize) + " bytes to each file server.");
		fileCount++;
		userFiles.get(user).add(filename + "`" + servers[0] + "`" + servers[1]);
	}
	
	private static ArrayList<Byte> readAllBytes(BufferedInputStream in, int fileSize) throws IOException {
		ArrayList<Byte> data = new ArrayList<>();
		int pageSize = 4096;
		byte[] buffer = new byte[pageSize];
		int bytesRead = 0;
		int bytesLeft = fileSize;
		while((bytesRead = in.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0) {
			for (int i = 0; i < bytesRead; i++) {
				data.add(buffer[i]);
			}
			bytesLeft -= bytesRead;
		}
		return data;
	}

	/**
	 * Gets the index of the servers on which to store an original and backup copy of a file.
	 * The method assumes that servers are started in alternating fashion, which will lead
	 * to files being evenly distributed across all virtual servers on all machines.
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
    		synchronized(files) {
    			files.put(filename, filename);
    		}
    	}
    	
    	void remove(String filename) {
    		synchronized(files) {
    			files.remove(filename);
    		}
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
	    		synchronized(out) {
	    			out.writeBytes(command);
	    			out.flush();
	    		}
    		}
    	}
    	
    	void sendBytes(byte[] b, int off, int len) throws IOException {
    		synchronized(out) {
    			out.write(b, off, len);
    			out.flush();
    		}
    	}
    	
    	void sendBytes(ArrayList<Byte> data) throws IOException {
    		int pageSize = 4096;
    		byte[] buffer = new byte[pageSize];
    		int len = Math.min(pageSize, data.size());
    		for (int position = 0, index = 0; position < data.size(); position++) {
    			buffer[index] = data.get(position);
    			index++;
    			if (index >= len) {
    				index = 0;
    				sendBytes(buffer, 0, len);
    				len = Math.min(pageSize, data.size() - position - 1);
    			}
    		}
    	}
    	
    	void lock() {
    		SERVER_LOCK.lock();
    	}
    	
    	void unlock() {
    		SERVER_LOCK.unlock();
    	}
    }
}
