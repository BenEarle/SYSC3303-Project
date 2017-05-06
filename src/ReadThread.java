import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;

import util.FileReader;
import util.Log;
import util.Var;

public class ReadThread extends ClientResponseThread {

	public ReadThread(DatagramPacket initialPacket) {
		super(initialPacket);
	}

	/*
	 * RRQ
	 * Open FR
	 * do
	 * 	Read data from file
	 * 	Send data
	 * 	Receive ack 
	 * while(data.len == 512)
	 * 
	 */
	
	public void run() {
		DatagramPacket packet;
		FileReader fr = null;
		try {
			fr = new FileReader(Var.SERVER_ROOT + file);
		} catch (FileNotFoundException e) {
			Log.err(e.getStackTrace().toString());
		}
		
		// Initialize block index to send first block
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x01;
		
		boolean lastPacket = false; // flag to indicate if last packet is sent
		byte[] data = null; // data from file
		byte[] msg  = null; // bytes in message to send
		
		// Get data from file
		try {
			data = fr.read();
			// Check if length read is less than full block size
			if(data.length != Var.BLOCK_SIZE) lastPacket = true; 
		} catch (Exception e) {
			data = new byte[1]; // Empty Message
			lastPacket = true;
		}

		// Build message from data
		msg = new byte[4+data.length];
		msg[0] = Var.DATA[0];
		msg[1] = Var.DATA[1];
		msg[2] = blockNum[0];
		msg[3] = blockNum[1];
		for(int i=0; i<data.length; i++) msg[i+4] = data[i]; // Copy data into message
		
		// Send first data packet
		Log.out("SERVER<ReadThread>: Sending Initial READ Data");	
		super.sendPacket(msg);

		// Loop until all packets are sent
		while (!lastPacket) {
			// Receive packet
			Log.out("SERVER<ReadThread>: Receiving ACK Data");
			packet = super.receivePacket();
			// Ensure packet is ack
			if (packet.getData()[0]==Var.ACK[0] &&  packet.getData()[1]==Var.ACK[1]) {
				// Ensure packet has correct index
				if (packet.getData()[2] == blockNum[0] && packet.getData()[3] == blockNum[1]) {
					// Get data from file and check if length read is less than full block size 
					try {
						data = fr.read();
						if(data.length != Var.BLOCK_SIZE) lastPacket = true; 
					// Exception if no bytes left in file. Send last packet empty
					} catch(Exception e) {
						data = new byte[1]; // Empty Message
						lastPacket = true;
					}
					
					// Increment Block Number
					blockNum = bytesIncrement(blockNum);
					
					// Build message from data
					msg = new byte[4+data.length];
					msg[0] = Var.DATA[0];
					msg[1] = Var.DATA[1];
					msg[2] = blockNum[0];
					msg[3] = blockNum[1];
					for(int i=0; i<data.length; i++) msg[i+4] = data[i]; // Copy data into message
					
					// Send Packet
					Log.out("SERVER<ReadThread>: Sending READ Data");
					super.sendPacket(msg);
					
				} else throw new IndexOutOfBoundsException();
			} else throw new IllegalArgumentException();
		}
		// Receive final ACK packet
		Log.out("SERVER<ReadThread>: Receiving Final ACK Data");
		packet = super.receivePacket();
		// Ensure ACK is valid
		if (packet.getData()[0]==Var.ACK[0] && packet.getData()[1]==Var.ACK[1]) {
			// Ensure block number is valid
			if (packet.getData()[2] == blockNum[0] && packet.getData()[3] == blockNum[1]) {
				
				Log.out("SERVER<ReadThread>: Read completed successfully.");
				
			} else throw new IndexOutOfBoundsException();
		} else throw new IllegalArgumentException();
		
		// Close file
		try {
			fr.close();
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
	}
	
	private byte[] bytesIncrement(byte[] data) {
		if (data[1] == 0xff) {
			data[0]++;
			data[1] = 0x00;
		} else {
			data[1]++;
		}
		return data;
	}
	
}
