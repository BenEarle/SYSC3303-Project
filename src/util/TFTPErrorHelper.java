package util;

import java.net.DatagramPacket;

public class TFTPErrorHelper {
	public static void sendError(DatagramPacket p, byte type, String message) {
		byte[] data = new byte[Var.BUF_SIZE];
		data[0] = 0;
		data[1] = 5;
		data[2] = 0;
		data[3] = type;
		for(int i = 0; i < message.length(); i ++){
			data[i + 4] = (byte) message.charAt(i);
		}
		//send the packet back to the person who sent us the wrong message
		UDPHelper udp = new UDPHelper();
		udp.setReturn(p);
		udp.sendPacket(data);
		if(type != 5){
			//quit transfer
			System.exit(0);
		}
	}
	
	
}
