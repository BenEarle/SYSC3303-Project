package util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;

public class Log {
	private static boolean enabled = true;
	private static boolean saveLog = false;
	private static String logged;
	
	/**
	 * Enable/disable all logging.
	 * @param e
	 */
	public static void enable(boolean e) {
		enabled = e;
	}
	
	/**
	 * Enable/disable saving the log to a string.
	 * @param e
	 */
	public static void saveLog(boolean e) {
		saveLog = e;
		logged = "";
	}
	
	public static String getLog() {
		String ret = logged;
		logged = "";
		return ret;
	}
	
	public static boolean toggleEnable(){
		enabled = !enabled;
		return enabled;
	}
	
	/**
	 * Log a general message.
	 * @param s
	 */
	public static void out(String s) {
		if (!enabled) return;
		
		System.out.println(s);
		
		if (saveLog) logged += s + "\n";
	}
	
	/**
	 * Log an error message.
	 * @param s
	 */
	public static void err(String s) {		
		System.err.println(s);
		
		if (saveLog) logged += s + "\n";
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
		String type;
		String sendString;
		byte[] data = packet.getData();
		if (!enabled) return;
		switch(data[1]) {
		case(0x01) : 
			type = "RRQ";
			break;
		case(0x02) :
			type = "WRQ";
			break;
		case(0x03) :
			type = "DATA";
			break;
		case(0x04):
			type = "ACK";
			break;
		case(0x05):
			type = "ERROR";
			break;
		default:
				type = "Unknown";
		}
		if (data[0] != 0x00) {
			type = "Unknown";
		}
		if (type.equals("RRQ") || type.equals("WRQ")) {
			sendString = "\n" + s +  ":\n" + 
						 "\tIP\t\t" + packet.getAddress().toString() + "\n" +
						 "\tPort\t\t" + packet.getPort() + "\n" +
						 "\tLength\t\t" + packet.getLength() + "\n" + 
						 "\tType\t\t" + type + "\n" +
						 "\tName and Mode\t" + new String(data).substring(2).trim() + "";
		} else if (type.equals("DATA")) {
			sendString = "\n" + s +  ":\n" + 
					 "\tIP\t\t" + packet.getAddress().toString() + "\n" +
					 "\tPort\t\t" + packet.getPort() + "\n" +
					 "\tLength\t\t" + packet.getLength() + "\n" + 
					 "\tData length\t" + (packet.getLength() - 4) + "\n" + 
					 "\tType\t\t" + type + "\n" +
					 "\tBlock #\t\t" +  (Byte.toUnsignedInt(packet.getData()[2]) * 256 + Byte.toUnsignedInt(packet.getData()[3])) + "";
		} else if ( type.equals("ACK")) {
			sendString = "\n" + s +  ":\n" + 
					 "\tIP\t\t" + packet.getAddress().toString() + "\n" +
					 "\tPort\t\t" + packet.getPort() + "\n" +
					 "\tLength\t\t" + packet.getLength() + "\n" +  
					 "\tType\t\t" + type + "\n" +
					 "\tBlock #\t\t" +  (Byte.toUnsignedInt(packet.getData()[2]) * 256 + Byte.toUnsignedInt(packet.getData()[3])) + "";
		}else if (type.equals("ERROR")){
			sendString = "\n" + s +  ":\n" + 
					 "\tIP\t\t" + packet.getAddress().toString() + "\n" +
					 "\tPort\t\t" + packet.getPort() + "\n" +
					 "\tLength\t\t" + packet.getLength() + "\n" + 
					 "\tType\t\t" + type + "\n" +
					 "\tError Code\t" + packet.getData()[2] + packet.getData()[3] + "\n" + 	
			 		 "\tError Message\t" + bString(data).substring(4).trim();			
			
		} else{
			sendString = "\n" + s +  ":\n" + 
					 "\tIP\t\t" + packet.getAddress().toString() + "\n" +
					 "\tPort\t\t" + packet.getPort() + "\n" +
					 "\tLength\t\t" + packet.getLength() + "\n" + 
					 "\tType\t\tNot Defined";
		}
		out(sendString);
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
