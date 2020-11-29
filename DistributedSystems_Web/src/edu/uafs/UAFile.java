/**
 * Names:         Colton Key, Ross Payne, and Julton Sword
 * Assignment:    Final Project - LionDB Distributed Server
 * Class:         CS 3003 - Distributed Systems (4:00 - 5:15 PM)
 */

package edu.uafs;

import java.io.File;
import java.net.Socket;
import java.util.LinkedList;

public class UAFile {

	File file;
	int fileID;
	LinkedList<Socket> socketList = new LinkedList<>();
	
	public UAFile(File file, int fileID, Socket socket) {
		this.file = file;
		this.fileID = fileID;
		socketList.add(socket);
	}
	
	void addSocket(Socket socket) {
		socketList.add(socket);
	}
	
	public int hashCode() {
		return fileID;
	}
	
}
