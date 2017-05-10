package util;

import java.net.DatagramPacket;

/*************************************************************************/
// This class is used for static helper methods for handling error packets.
/*************************************************************************/

public class TFTPErrorHelper {

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
