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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class FileServer {

	private static HashMap<String, UAFile> files = new HashMap<>();
	private static File fileDirectory;

	public static void main(String[] args) throws IOException {

		String path = "";
		String host = "";
		int port = 0;

		try {
			if (args.length < 3) {
				throw new Exception("Bad.");
			}
			path = args[0];
			host = args[1];
			port = Integer.parseInt(args[2]);
		} catch (Exception e) {
			System.out.println(
					"Invalid syntax:    java FileServer [starting file directory] [UAServer IP addr] [UAServer port]");
			System.exit(10);
		}

		fileDirectory = new File(path);

		try {
			if (!fileDirectory.exists()) {
				// will create directory along with parent directories if necessary
				fileDirectory.mkdirs();
			}
		} catch (Exception e) {
			Logger.log("FILE SERVER", "Exception when attempting to create starting directory. Exiting...");
			System.exit(1);
		}

		Socket socket = new Socket(host, port);
		BufferedReader commandIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		BufferedInputStream fileServerIn = new BufferedInputStream(socket.getInputStream());
		DataOutputStream fileServerOut = new DataOutputStream(socket.getOutputStream());

		try {
			fileServerOut.writeBytes("file server connected\n");
			fileServerOut.flush();
		} catch (IOException ex) {
			Logger.log("FILE SERVER", ex.getMessage());
		}
		Logger.log("FILE SERVER",
				String.format("Connected to UAServer at [%s:%d]\nStarting directory is [%s]", host, port, path));

		String line;
		while ((line = commandIn.readLine()) != null) {

			switch (line) {
			case "add":
				try {
					Logger.log("FILE SERVER", "Received add command.");
					String meta = commandIn.readLine();
					addFile(meta, new DataInputStream(socket.getInputStream()), fileServerOut);
				} catch (IOException ex) {
					Logger.log("FILE SERVER",
							String.format("Exception thrown while adding a file.%nDetails:%n%s%n%n", ex.getMessage()));
				}
				break;
			case "backup":
				try {
					Logger.log("FILE SERVER", "Received backup command.");
					download(commandIn, fileServerIn, fileServerOut, true);
				} catch (IOException ex) {
					Logger.log("FILE SERVER", String
							.format("Exception thrown while backing up a file.%nDetails:%n%s%n%n", ex.getMessage()));
				}
				break;
			case "download":
				try {
					Logger.log("FILE SERVER", "Received download command.");
					download(commandIn, fileServerIn, fileServerOut, false);
				} catch (IOException ex) {
					Logger.log("FILE SERVER", String
							.format("Exception thrown while downloading a file.%nDetails:%n%s%n%n", ex.getMessage()));
				}
			case "remove":
				try {
					Logger.log("FILE SERVER", "Received remove command.");
					removeFile(commandIn);
				} catch (IOException ex) {
					Logger.log("FILE SERVER", String.format("Exception thrown while removing a file.%nDetails:%n%s%n%n",
							ex.getMessage()));
				}
				break;
			}
		}
	}

	/**
	 * Reads a line of text from an input stream.
	 * 
	 * @param in The input stream from which to read.
	 * @return The line of text read from the input stream.
	 * @throws IOException
	 */
	public static String readLine(BufferedInputStream in) throws IOException {
		var sb = new StringBuilder();
		char next;
		while ((next = (char) in.read()) != '\n') {
			sb.append(next);
		}
		return sb.toString();
	}

	/**
	 * Reads file data from the server's input stream and creates the file on the
	 * server's local directory.
	 * 
	 * @param meta   Meta data about the file (owner's username, filename, and
	 *               size).
	 * @param dataIn The server's input stream from which to read the file.
	 * @param out    The server's output stream.
	 * 
	 * @throws IOException
	 */
	private static void addFile(String meta, DataInputStream dataIn, DataOutputStream out) throws IOException {

		String[] tokens = meta.split(" ");
		String user = tokens[0];
		String filename = tokens[1];
		String path = String.format("%s%s%s%s%s", fileDirectory, File.separator, user, File.separator, filename);
		int fileSize = Integer.parseInt(tokens[2]);

		// create user's directory inside starting directory if it doesn't exist
		File userdir = new File(fileDirectory + File.separator + user);
		if (!userdir.exists()) {
			Logger.log("FILE SERVER", String.format("Creating user directory [%s]...", userdir.getPath()));
			userdir.mkdirs();
		}

		Thread thread = new Thread(new Runnable() {

			int pageSize = 4096;
			byte[] buffer = new byte[pageSize];
			int bytesRead = 0;
			int bytesLeft = fileSize;
			File file = new File(userdir + File.separator + filename);
			UAFile uaFile = new UAFile(filename, path, user, fileSize);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, true));

			@Override
			public void run() {

				try {
					while ((bytesRead = dataIn.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0) {
						bos.write(buffer, 0, Math.min(pageSize, bytesLeft));
						bytesLeft -= bytesRead;
					}

					if (bos != null) {
						bos.close();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}

				files.put(uaFile.getFileID(), uaFile);

			}
			
		});

		thread.start();

		try {
			thread.join(30000);
			Logger.log("FILE SERVER", String.format("Successfully wrote file [%s] to disk.", filename));
		} catch (InterruptedException ex) {
			Logger.log("FILE SERVER", "An input stream timed out while attempting to read a file.");
		}

	}

	/**
	 * Sends a file from this server over the server's output stream.
	 * 
	 * @param r
	 * @param in
	 * @param out
	 * @param backup
	 * @throws IOException
	 */
	private static void download(BufferedReader r, BufferedInputStream in, DataOutputStream out, boolean backup)
			throws IOException {
		String meta = r.readLine();
		String[] tokens = meta.split(" ");
		String key = String.format("%s:%s", tokens[1], tokens[0]);
		String backDest = tokens[2];
		UAFile uaFile = files.get(key);

		if (uaFile == null) {
			return;
		}

		File file = new File(uaFile.getPath());

		if (file == null || !file.exists()) {
			return;
		}

		int pageSize = 4096;
		byte[] buffer = new byte[pageSize];
		int bytesRead = 0;
		int bytesLeft = uaFile.getSize();
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

		if (backup) {
			out.writeBytes("backup\n");
			out.writeBytes(backDest + "\n");
			out.writeBytes(uaFile.getSize() + "\n");
			out.writeBytes(tokens[0] + "\n");
			out.writeBytes(tokens[1] + "\n");
		} else {
			out.writeBytes("download\n");
			out.writeBytes(uaFile.getOwner() + "\n");
			out.writeBytes(uaFile.getSize() + "\n");
		}

		while ((bytesRead = bis.read(buffer, 0, Math.min(pageSize, bytesLeft))) > 0) {
			out.write(buffer, 0, Math.min(pageSize, bytesLeft));
			bytesLeft -= bytesRead;
		}

		bis.close();
		Logger.log("FILE SERVER", String.format("Successfully transferred file [%s].", uaFile.getFilename()));
	}

	/**
	 * Deletes a file from the server's local directory and removes it from the
	 * server's file map.
	 * 
	 * @param r Buffered reader used to read file ID.
	 * 
	 * @throws IOException
	 */
	private static void removeFile(BufferedReader r) throws IOException {
		String info = r.readLine();
		UAFile uaFile = files.get(info);

		if (uaFile == null) {
			return;
		}

		File file = new File(uaFile.getPath());
		if (!file.isDirectory() && file.delete()) {
			files.remove(info);
		}

		Logger.log("FILE SERVER", String.format("Successfully removed file [%s].", uaFile.getFilename()));
	}
}
