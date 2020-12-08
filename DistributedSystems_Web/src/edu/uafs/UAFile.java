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

	private String filename;
	private String path;
	private String owner;
	private int size;
	private String fileID;
	
	public UAFile(String filename, String path, String owner, int size) {
		this.filename = filename;
		this.path = path;
		this.owner = owner;
		this.size = size;
		fileID = String.format("%s:%s", owner, filename);
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public void setOwner() {
		this.owner = owner;
	}
	
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}

	public String getFileID(){
		return fileID;
	}
	
	public int hashCode() {
		return fileID.hashCode();
	}

	public boolean equals(UAFile file){
		return fileID.equals(file.getFileID());
	}
	
}
