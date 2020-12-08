package edu.uafs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

	private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss.SS");
	private static String logDirectory;
	
	/**
	 * Prints sender and message to the console.
	 * @param sender The source of the message.
	 * @param message The message to print.
	 */
	public static void print(String sender, String message) {
		System.out.printf("    -- %s:  %s -- %n%s%n%n",
				DF.format(LocalDateTime.now()), sender.toUpperCase(), message); 
	}
	
	/**
	 * Saves the sender and message to the "DistSys_Log" file in the Logger's specified directory.
	 * @param sender The source of the message.
	 * @param message The message to save.
	 */
	public static void save(String sender, String message) {
		if (logDirectory != null) {
			File file = new File(String.format("%s%sDistSys_Log", logDirectory, File.separator));
			try {
				PrintWriter writer = new PrintWriter(new FileWriter(file, true));
				writer.append(String.format("    -- %s:  %s -- %n%s%n%n",
				DF.format(LocalDateTime.now()), sender.toUpperCase(), message));
			} catch (IOException ex) {
				print("LOGGER", ex.getMessage());
			}
		}
	}
	
	/**
	 * Prints the sender and message to the console and saves them to a file at the same time.
	 * @param sender The source of the message.
	 * @param message The message to print and save.
	 */
	public static void log(String sender, String message) {
		print(sender, message);
		save(sender, message);
	}
	
	public static void setLogDirectory(String dir) {
		logDirectory = dir;
	}
	
	public static String getLogDirectory() {
		return logDirectory;
	}
	
}
