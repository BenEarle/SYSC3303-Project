import java.io.IOException;
import java.net.DatagramPacket;

import util.FileWriter;
import util.Log;
import util.Var;

public class ReadThread extends ClientResponseThread {

	public ReadThread(DatagramPacket initialPacket, boolean verbose) {
		super(initialPacket, verbose);
	}

	/*
	 * RRQ
	 * do
	 * 	Read data from file
	 * 	Send data
	 * 	Receive ack 
	 * while(data.len == 512)
	 * 
	 */
	
	public void run() {
		int index = 0;
		DatagramPacket packet;
		byte[] data;
		// Send initial Acknowledge
		byte[] ack = Var.ACK_WRITE;
		byte[] bytesToWrite = null;
		super.sendPacket(ack);
		
	}
	
}
