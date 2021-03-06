package util;

import java.net.DatagramPacket;

/*************************************************************************/
// This class is used for static helper methods for handling error packets.
/*************************************************************************/

public class TFTPErrorHelper {
	
	public static Integer requestPacketChecker(UDPHelper u, DatagramPacket p) {
		/**
		 * Checks:
		 * 1) data size is greater than 6
		 * 2) opcode matches expected opcode of 01 or 02
		 * 3) null byte follows filename
		 * 4) filename is readable characters
		 * 5) mode is 'netascii' or 'octet'
		 * 6) ends in a null byte
		 */
		byte[] data = p.getData();
		int length = p.getLength();
		int i = 2;
		if (isError(data)) {
			return 4;
		}
		// Check opCode.
		if (length < 2) {
			// Data not long enough.
			sendError(u, (byte) 0x04, "Data packet not long enough");
			return 4;
		}

		// Check the READ/WRITE code is correct.
		if (!(data[0] == Var.READ[0] && data[1] == Var.READ[1])
				&& !(data[0] == Var.WRITE[0] && data[1] == Var.WRITE[1])) {
			// Invalid opcode for request.
			sendError(u, (byte) 0x04, "Invalid OP code for request");
			return 4;
		}

		// Capture the filename, if the buffer is overrun the packet is invalid.
		do {
			if (i >= length) {
				sendError(u, (byte) 0x04, "Missing Null terminator after filename");
				return 4;
			}
		} while (data[i++] != 0);
		String filename = new String(data, 2, i - 3);

		if (filename.length() == 0) {
			// The filename is missing.
			sendError(u, (byte) 0x04, "Filename missing");
			return 4;
		}

		if (!isAsciiPrintable(filename)) {
			// The filename contains bad characters.
			sendError(u, (byte) 0x04, "Filename contains non printable characters");
			return 4;
		}

		// Capture the mode, if the buffer is overrun the packet is invalid.
		int start = i;
		do {
			if (i >= length) {
				if (i == start) {
					sendError(u, (byte) 0x04, "Mode missing");
				} else {
					sendError(u, (byte) 0x04, "Packet does not end with null");
				}
				return 4;
			}
		} while (data[i++] != 0);
		String mode = new String(data, start, i - start - 1);

		// Check the NETASCII/OCTET mode is correct.
		if (!mode.toLowerCase().equals("netascii") && !mode.toLowerCase().equals("octet")) {
			sendError(u, (byte) 0x04, "Mode not an acceptable form");
			return 4;
		}

		// Packet format is correct.
		return null;
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
	 * checks individual characters for readable characters
	 * 
	 * @param ch
	 * @return
	 */
	private static boolean isAsciiPrintable(char ch) {
		return ch >= 32 && ch < 127;
	}

	public static Integer dataPacketChecker(UDPHelper u, DatagramPacket p, int expectedBlock, boolean firstLoop) {
		/**
		 * Checks:
		 * 1) data size is greater than 5
		 * 2) opcode matches expected opcode of 03
		 * 3) block number matches expected
		 * 4) length isn't greater than 516
		 */
		byte[] data = p.getData();
		int length = p.getLength();
		if (isError(data)) {
			return 4;
		}
		if (length < 4) {
			// data too small
			sendError(u, (byte) 0x04, "Data packet too small");
			return 4;
		}
		if (length > 516) {
			// Too long of packet
			sendError(u, (byte) 0x04, "Data packet is too large");
			return 4;
		}
		if (data[0] != Var.DATA[0] || data[1] != Var.DATA[1]) {
			// wrong op code
			
			sendError(u, (byte) 0x04, "Invalid data op code");
			return 4;
		}
		int blockNum = Byte.toUnsignedInt(data[2]) * 256 + Byte.toUnsignedInt(data[3]);

		//System.out.println("DATA: " + blockNum + "  " + expectedBlock);
		// Unexpected block number
		if(blockNum < expectedBlock || (!firstLoop && blockNum != expectedBlock)){
			// This is a duplicate data, resend the ack packet with the correct block number.
			byte[] d = Var.ACK_WRITE.clone();
			d[2] = data[2];
			d[3] = data[3];
			Log.out("Received a Potential duplicate DATA packet. Resending ACK " + blockNum + ".");
			u.sendPacket(d);
			return -1;
		} else if (blockNum > expectedBlock) {
			sendError(u, (byte) 0x04, "Recv wrong block number");
			return 4;
		}
		
		return null;
	}
	
	public static Integer ackPacketChecker(UDPHelper u, DatagramPacket p, int expectedBlock, boolean firstLoop) {
		/**
		 * Checks:
		 * 1) data size is 4
		 * 2) opcode matches expected opcode of 04
		 * 3) block number matches expected
		 */
		byte[] data = p.getData();
		int length = p.getLength();
		if (isError(data)) {
			return 4;
		}
		if (length != 4) {
			// data too small
			sendError(u, (byte) 0x04, "Ack packet wrong size");
			return 4;
		}

		if (data[0] != Var.ACK[0] || data[1] != Var.ACK[1]) {
			// wrong op code
			sendError(u, (byte) 0x04, "Invalid ACK op code");
			return 4;
		}

		int blockNum = Byte.toUnsignedInt(data[2]) * 256 + Byte.toUnsignedInt(data[3]);

		//System.out.println("ACK:  " + blockNum + "  " + expectedBlock);
		// Unexpected block number
		if (blockNum < expectedBlock || (!firstLoop && blockNum != expectedBlock)) {
			// This is a duplicate ack, ignore the packet it.
			Log.out("Received a Potential duplicate ACK " + blockNum + " packet, ignoring.");
			return -1;
		} else if (blockNum > expectedBlock) {	
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
		u.setResendOnTimeout(false);
		System.arraycopy(message.getBytes(), 0, data, 4, message.length());
		Log.err(message);
		// send the packet back to the person who sent us the wrong message
		u.sendPacket(data);
	}

	public static boolean isError(byte[] data) {
		return data.length >= 2 && data[0] == Var.ERROR[0] && data[1] == Var.ERROR[1];
	}

	public static void unPackError(DatagramPacket p) {
		byte[] data = p.getData();
		int length = p.getLength();
		
		// Check opCode.
		if (length < 2) {
			// Data not long enough.
			Log.err("Invalid ERROR packet received, missing opCode");
			return;
		}
		if (!(data[0] == Var.ERROR[0] && data[1] == Var.ERROR[1])) {
			// opCode is incorrect.
			Log.err("Invalid ERROR packet received, incorrect opCode " + data[0] + "" + data[1]);
			return;
		}
		
		// Check error code.
		if (length < 4) {
			// Data not long enough.
			Log.err("Invalid ERROR packet received, missing error code");
			return;
		}
		int errorCode = data[2] * 256 + data[3];
		if (errorCode < 0 || errorCode > 6) {
			// Error code is incorrect.
			Log.err("Invalid ERROR packet received, incorrect error code " + errorCode);
			return;
		}

		// Capture the message, if the buffer is overrun the packet is invalid.
		int i = 4;
		do {
			if (i >= length) {
				Log.err("Invalid ERROR packet received, missing end null character");
				return;
			}
		} while (data[i++] != 0);
		
		if (5 != i) {
			String errorMsg = new String(data, 4, i - 5);
			Log.err("Error packet type " + errorCode + " received:\n" + errorMsg);
		} else {
			Log.err("Error packet type " + errorCode + " received, no attached message.");
		}
	}
	
}
