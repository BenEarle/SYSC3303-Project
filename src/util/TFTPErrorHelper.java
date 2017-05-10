package util;

import java.net.DatagramPacket;

/*************************************************************************/
// This class is used for static helper methods for handling error packets.
/*************************************************************************/

public class TFTPErrorHelper {

	public static Integer requestPacketChecker(UDPHelper u, DatagramPacket p) {
		/**
		 * Checks: 1) data size is greater than 6 2) opcode matches expected
		 * opcode of 01 or 02 3) null byte follows filename 4) filename is
		 * readable characters 5) mode is 'netascii' or 'octet' 6) ends in a
		 * null byte
		 */
		byte[] data = p.getData();
		String dataString = Log.bString(p.getData());
		// Check opCode
		if (data.length < 6) {
			// data not long enough
			sendError(u, (byte) 0x04, "Data packet not long enough");
			return 4;
		}

		if ((data[0] != 0x00) && ((data[1] != 0x01) || (data[2] != 0x02))) {
			// Invalid opcode for request
			sendError(u, (byte) 0x04, "Invalid OP code for Request");
			return 4;
		}

		int endOfFileName = findByteIndex(data, 3, 0x00);
		if (endOfFileName == -1) {
			// no null to indicate end of fileName
			sendError(u, (byte) 0x04, "Missing Null termintor after fileName");
			return 4;
		}

		String fileName = dataString.substring(2, endOfFileName);
		if (!isAsciiPrintable(fileName)) {
			// File name contains non printable characters
			sendError(u, (byte) 0x04, "FileName contains non printable characters");
			return 4;
		}

		String mode = dataString.substring(endOfFileName + 1, dataString.length() - 1).toLowerCase();
		if (!mode.equals("netascii") && !mode.equals("octet")) {
			// Mode is incorrect
			sendError(u, (byte) 0x04, "Mode not an acceptable form");
			return 4;
		}

		byte lastCharacter = data[data.length - 1];
		if (lastCharacter != 0x00) {
			// Packet does not end in null
			sendError(u, (byte) 0x04, "Packet does not end with null");
			return 4;
		}
		return null;
	}

	private static int findByteIndex(byte[] b, int startingFrom, int findByte) {
		for (int i = startingFrom; i < b.length; i++) {
			if (b[i] == 0x00) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Checks that all characters in string is ascii printable
	 * 
	 * @param str
	 *            string to check
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
	 * 
	 * @param ch
	 * @return
	 */
	private static boolean isAsciiPrintable(char ch) {
		System.out.println(ch);
		return ch >= 32 && ch < 127;
	}

	public static Integer dataPacketChecker(UDPHelper u, DatagramPacket p, int expectedBlock) {
		/**
		 * Checks:
		 * 1) data size is greater than 5
		 * 2) opcode matches expected opcode of 03
		 * 3) block number matches expected
		 * 4) length isn't greater than 516
		 */
		byte[] data = p.getData();
		if (data.length < 5) {
			// data too small
			sendError(u, (byte) 0x04, "Data packet too small");
			return 4;
		}

		if (data[0] != 0x00 && data[1] != 0x03) {
			// wrong op code
			sendError(u, (byte) 0x04, "Invalid data op code");
			return 4;
		}

		int blockNum = data[2] * 256 + data[3];
		if (blockNum != expectedBlock) {
			// Got wrong block
			sendError(u, (byte) 0x04, "Recv wrong block number");
			return 4;
		}
		if (data.length > 516) {
			// Too long of packet
			sendError(u, (byte) 0x04, "Data packet is too large");
			return 4;
		}
		return null;
	}
	
	public static Integer ackPacketChecker(UDPHelper u, DatagramPacket p, int expectedBlock) {
		/**
		 * Checks:
		 * 1) data size is 4
		 * 2) opcode matches expected opcode of 04
		 * 3) block number matches expected
		 */
		byte[] data = p.getData();
		if (data.length != 4) {
			// data too small
			sendError(u, (byte) 0x04, "Ack packet wrong size");
			return 4;
		}
		if (data[0] != 0x00 && data[1] != 0x04) {
			//wrong op code
			sendError(u, (byte) 0x04, "Invalid ACK op code");
			return 4;
		}
		
		int blockNum = data[2] * 256 + data[3];
		if(blockNum != expectedBlock) {
			//Got wrong block
			sendError(u, (byte) 0x04, "ACK wrong block number");
			return 4;
		}
		return null;
	}

	public static void sendError(UDPHelper u, byte type, String message) {
		byte[] data = new byte[5 + message.length()];
		data[0] = 0;
		data[1] = 5;
		data[2] = 0;
		data[3] = type;

		System.arraycopy(message.getBytes(), 0, data, 4, message.length());

		// send the packet back to the person who sent us the wrong message
		u.sendPacket(data);
	}

	public static boolean isError(byte[] data) {
		return (data[1] == 5);
	}

	public static void unPackError(DatagramPacket p) {
		byte[] data = p.getData();
		byte[] message = new byte[Var.BLOCK_SIZE];
		for (int i = 4; i < data.length; i++) {
			message[i - 4] = data[i];
		}
		Log.err("Error packet type " + data[3] + " received.");
		Log.err(message.toString());
	}
}
