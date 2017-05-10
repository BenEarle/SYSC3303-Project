package util;

import java.net.DatagramPacket;

/*************************************************************************/
// This class is used for static helper methods for handling error packets.
/*************************************************************************/

public class TFTPErrorHelper {

	public static Integer requestPacketChecker(DatagramPacket p, String mode) {
		/**
		 * Checks:
		 * 1) data size is greater than 6
		 * 2) opcode matches expected opcode of 01 or 02
		 * 3) null byte follows filename
		 * 4) filename is readable characters
		 * 5) mode is 'netascii' or 'octet'
		 * 6) ends in a null byte
		 */
		Integer returnError = null;
		byte[] data = p.getData();
		String dataString = Log.bString(p.getData());
		//Check opCode
		if (data.length < 6) {
			returnError = 4;
			//data not long enough
		}
		
		if ((data[0] != 0x00) && ((data[1] != 0x01) || (data[2] != 0x02))) {
			returnError = 4;
			//Invalid opcode for request
		}
		
		int endOfFileName = findByteIndex(data,4, 0x00);
		if (endOfFileName == -1) {
			returnError = 4;
			//no null to indicate end of fileName
		}
		
		String fileName = dataString.substring(4, endOfFileName);
		if (!isAsciiPrintable(fileName)) {
			returnError = 4;
			//File name contains non printable characters
		}
		
		byte lastCharacter = data[data.length - 1];
		if (lastCharacter != 0x00) {
			returnError = 4;
			//Packet does not end in null
		}
		
		return returnError;
	}
	
	private static int findByteIndex(byte[] b, int startingFrom, int findByte) {
		for(int i = startingFrom; i < b.length ; i++) {
			if (b[i] == 0x00) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Checks that all characters in string is ascii printable
	 * @param str string to check
	 * @return if it is ascii printable
	 */
	private static boolean isAsciiPrintable(String str) {
	      if (str == null) {
	          return false;
	      }
	      int sz = str.length();
	      for (int i = 0; i < sz; i++) {
	          if (isAsciiPrintable(str.charAt(i)) == false) {
	              return false;
	          }
	      }
	      return true;
	  }
	
	/**
	 * checks indivdual characters for readable characters
	 * @param ch
	 * @return
	 */
	private static boolean isAsciiPrintable(char ch) {
	      return ch >= 32 && ch < 127;
	  }
	
	public static void sendError(DatagramPacket p, byte type, String message) {
		byte[] data = new byte[Var.BUF_SIZE];
		data[0] = 0;
		data[1] = 5;
		data[2] = 0;
		data[3] = type;
		for (int i = 0; i < message.length(); i++) {
			data[i + 4] = (byte) message.charAt(i);
		}
		// send the packet back to the person who sent us the wrong message
		UDPHelper udp = new UDPHelper(p);
		udp.sendPacket(data);
		if (type != 5) {
			// quit transfer
			System.exit(0);
		}
	}

	public static boolean isError(byte[] data) {
		return (data[1] == 5);
	}
	
	public static void unPackError(DatagramPacket p){
		byte[] data = p.getData();
		byte[] message = new byte[Var.BLOCK_SIZE];
		for (int i = 4; i < data.length; i++) {
			message[i - 4] = data[i];
		}
		Log.err("Error packet type " + data[3] + " received.");
		Log.err(message.toString());
	}
}
