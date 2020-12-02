/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
					if (line.equals("file server connected")) {
						log("A file server has connected");
						fileServers.add(new SimpleEntry<>(socket, serverOut));
					} else if (line.equals("client connected")) {
						log("A client has connected.");
						clients.put(socket.toString(), serverOut);
					} else if (line.contains("distribute~")) {
                        String filename = line.split("~")[1];
                        synchronized (distributor.serverResponses) {
                            distributor.serverResponses.put(socket.getPort(), filename);
                        }
                    } else {
						// handle response from file server
						if (line.contains("F|")) {
                            synchronized(messageQueue) {
                                messageQueue.offer(line);
                            }
						} else {
							// handle response from client
							log(String.format("%s :  %s", socket.toString(), line));
							String[] cmdArgs = line.split(" ");
							if (cmdArgs.length == 2) {

								String command = cmdArgs[0].toLowerCase().trim();
								String parameter = cmdArgs[1].trim();

								if (command.equals("list") && parameter.equalsIgnoreCase("all")) {
									for (SimpleEntry<Socket, PrintWriter> entry : fileServers) {
                                        PrintWriter p = entry.getValue();
										p.format("%s~%s~%s%n", socket.toString(), command, parameter);
									}
								} else if (command.equals("add") || command.equals("remove") || command.equals("list")) {
									if(fileServers.size() > 0) {
										
                                        if (command.equals("add")) {
                                        	
                                        	
                                        	// add syntax: add {username} {filename} {file size in bytes}
											distributeFile(socket, this.currentUser, parameter, cmdArgs[2]);
											
                                        } else {
                                            int hashCode = parameter.hashCode();
                                            int serverNumber = Math.abs(hashCode % fileServers.size());
                                            PrintWriter fsout = fileServers.get(serverNumber).getValue();
                                            String messageToFileServer = String.format("%s~%s~%s", socket.toString(), command, parameter);
                                            fsout.println(messageToFileServer);
                                        }
									} else {
										log("Attempted '" + command + "' file operation with no available file servers.");
										serverOut.println("No available file servers.");
									}

								} else {
									serverOut.println("Invalid command.");
								}
							} else if (cmdArgs.length == 3) {

								if (cmdArgs[0].equals("register")){

									// register syntax: register {username} {password}
									String username = cmdArgs[1];
									String password = cmdArgs[2];

									log(String.format("Registering new user '%s'...", username));
									if ( registerUser(serverIn, serverOut, username, password) ) {
										serverOut.println("Register successful.");
										log("Register successful.");
									} else {
										serverOut.println("Register failed.");
										log("Register failed.");
									}
								} else if (cmdArgs[0].equals("login")){

									// login syntax: login {username} {password}
									String username = cmdArgs[1];
									String password = cmdArgs[2];

									if( cmdArgs[1] != null && users.containsKey(cmdArgs[1]) ) {

										log(String.format("Logging in user '%s'...", username));
										if( validateLogin(serverIn, serverOut, username, password) ) {
											currentUser = username;
											serverOut.println("Login successful.");
											log("Login successful.");
										} else {
											serverOut.println("Login failed.");
											log("Login failed.");
										}
									} else {
										serverOut.println("Invalid username.");
									}

								} else {
									serverOut.println("Invalid command.");
								}

							} else if (cmdArgs.length == 1){
								if(cmdArgs[0].equals("whoami")) {
									serverOut.println("Current user: " + currentUser);
								} else {
									serverOut.println("Invalid command.");
								}
							} else {
								serverOut.println("Invalid command.");
								log(String.format("Invalid command: %s ", cmdArgs.toString()));
							}
						}
					}

//					log("Waiting for message from " + socket.toString());
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

		// private boolean addFile(String user, String filename) {

		// 	try {
		// 		int[] indices = getServerIndices(filename);

		//         var dest1 = fileServers.get(indices[0]);
		//         var dest2 = fileServers.get(indices[1]);
		//         String messageToFileServer = String.format("%s~%s~%s", socket.toString(), "add", user+":"+filename);
		//         dest1.getValue().println(messageToFileServer);
		//         dest2.getValue().println(messageToFileServer);
		//         fileCount++;
		// 		userFiles.get(user).add(filename);
		//         return true;
		// 	} catch (Exception e) {
		// 		e.printStackTrace();
		// 		return false;
		// 	}

		// }

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



	private static boolean registerUser(BufferedReader serverIn, PrintWriter serverOut, String username, String password) throws IOException {

		try {
			users.put(username, password);
			userFiles.put(username, new ArrayList<String>());
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	private static boolean validateLogin(BufferedReader serverIn, PrintWriter serverOut, String username, String password) throws IOException {

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

		log("````````````````````````````````````````````````````````````````````````````````````in distributeFile");
		
		try {
			int fourKBpage = 4096;
			byte[] b = new byte[fourKBpage];

			log("getting input/output streams");
			DataInputStream clientIn = new DataInputStream(clientSocket.getInputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			int bytesRead = 0;
			int offset = Integer.parseInt(size);
			
			
			log("getting server incides");
			int[] servers = getServerIndices(filename);
			DataOutputStream server1 = new DataOutputStream(fileServers.get(servers[0]).getKey().getOutputStream());
			DataOutputStream server2 = new DataOutputStream(fileServers.get(servers[1]).getKey().getOutputStream());

			log("sending bytes...");
			// maybe read from byte 0 to size instead of Mackey logic
			while( (bytesRead = clientIn.read(b, 0, Math.min(fourKBpage, offset))) > 0){
				log(offset + " bytes left to send");
				server1.write(b);
				server2.write(b);
				offset -= bytesRead;
			}
			
			fileCount++;
			userFiles.get(user).add(filename + "`" + servers[0] + "`" + servers[1]);

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
