import java.io.IOException;
import java.net.DatagramPacket;

import util.FileWriter;
import util.Log;
import util.Var;

public class WriteThread extends ClientResponseThread {
	
	public WriteThread(DatagramPacket initialPacket, boolean verbose) {
		super(initialPacket, verbose);
	}
	
	// send ACK packet 0400
	//
	// make a file writer (filename)
	//
	// do
	// 	receive DATA
	// 	ACK[3]++
	// 	write the data to the file
	// 	filewriter.write(Data)
	// 	send ACK
	// while(data.len == 512)
	// socket.close()
	// filewriter.close()

	public void run() {
		DatagramPacket packet;
		byte[] data;
		byte[] ack = {0,4,0,0}; // initial ack message
		byte[] bytesToWrite = null;
		int index = 0; // current block of file		
		
		// Open FileWriter
		FileWriter fw = null;
		try {
			fw = new FileWriter(Var.SERVER_ROOT + file);
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
		
		// Send initial Acknowledge
		if(verbose) Log.out("SERVER<WriteThread>: Sending Initial ACK" + ack.toString());
		super.sendPacket(ack);
				
		// Loop until all packets are received
		do {
			// receive packet
			packet = super.receivePacket();
			if(verbose) Log.packet("SERVER<WriteThread>: Received WRITE Data", packet);
			data = packet.getData();
			if (data[0] != 0 || data[1] != 3)
				throw new IllegalArgumentException();
			
			// Get block number from data and assign to ack message
			ack[2] = data[2];
			ack[3] = data[3];
			index++;
			
			// Enforce correct packet number
			// ack 2 and 3 are a single number
			if (ack[2] * 256 + ack[3] != index)
				throw new IllegalArgumentException();

			// Make an array with the bytes to write
			bytesToWrite = new byte[packet.getLength() - 4];
			for (int i = 4; i < packet.getLength(); i++) {
				if (data[i] == 0)
					break;
				bytesToWrite[i - 4] = data[i];
			}
			
			//write the block to the file
			try {
				// To account for empty packets being 1 null byte
				if(bytesToWrite.length!=1 || bytesToWrite[0]!=0)
					fw.write(bytesToWrite);
			} catch (IOException e) {
				Log.err(e.getStackTrace().toString());
			}
			
			//Send the acknowledge
			if(verbose) Log.out("SERVER<WriteThread>: Sending WRITE ACK" + ack.toString());
			super.sendPacket(ack);
		} while (bytesToWrite.length == Var.BLOCK_SIZE);
		
		// Close input stream
		try {
			fw.close();
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
		super.close();
		if(verbose) Log.out("SERVER<WriteThread>: Write completed successfully");
	}

	
}
