package util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;

public class Log {
	private static boolean enabled = true;
	
	/**
	 * Enable/disable all logging.
	 * @param e
	 */
	public static void enable(boolean e) {
		enabled = e;
	}
	
	/**
	 * Log a general message.
	 * @param s
	 */
	public static void out(String s) {
		if (!enabled) return;
		
		System.out.println(s);
	}
	
	/**
	 * Log an error message.
	 * @param s
	 */
	public static void err(String s) {		
		System.err.println(s);
	}
	
	/**
	 * Log an error message and stack trace.
	 * @param message
	 */
	public static void err(String message, Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String err = sw.toString();
		
		err(message + " -> " + err);
	}
	
	/**
	 * Log packet information.
	 * @param s
	 * @param packet
	 */
	public static void packet(String s, DatagramPacket packet) {
		if (!enabled) return;
		
		out(
				"\n" + s +  ":\n" +
				"\tport\t" + packet.getPort() + "\n" +
				"\tlength\t" + packet.getLength() + "\n" +
				"\tbytes\t[" + bBytes(packet.getData(), packet.getLength()) + "]\n" +
				"\tstring\t'" + bString(packet.getData(), packet.getLength()) + "'"
			);
	}

	/**
	 * Convert a byte array to a readable string of byte content.
	 * @param bytes
	 * @return
	 */
	public static String bBytes(byte[] bytes) {
		String s = "";
		
		if (bytes.length == 0) {
			return s;
		}
		
		for (int i = 0, l = bytes.length; ; i++) {
			if (i == l - 1) {
				s += bytes[i];
				break;
			} else {
				s += bytes[i] + ",";
			}
		}
		
		return s;
	}
	
	/**
	 * Convert a byte array to a readable string of byte content, limited by length.
	 * @param bytes
	 * @param length
	 * @return
	 */
	public static String bBytes(byte[] bytes, int length) {
		String s = "";
		
		if (bytes.length == 0 || length == 0) {
			return s;
		}
		
		for (int i = 0, l = bytes.length; ; i++) {
			if (i == l - 1 || i == length - 1) {
				s += bytes[i];
				break;
			} else {
				s += bytes[i] + ",";
			}
		}
		
		return s;
	}
	
	/**
	 * Convert a byte array to a string of characters.
	 * @param bytes
	 * @return
	 */
	public static String bString(byte[] bytes) {
		return new String(bytes, 0, bytes.length);
	}
	
	/**
	 * Convert a byte array to a string of characters, limited by length.
	 * @param bytes
	 * @param length
	 * @return
	 */
	public static String bString(byte[] bytes, int length) {
		return new String(bytes, 0, length);
	}
}
