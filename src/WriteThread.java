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
		int index = 0;
		DatagramPacket packet;
		byte[] data;
		// Send initial Acknowledge
		byte[] ack = Var.ACK_WRITE;
		byte[] bytesToWrite = null;
		super.sendPacket(ack);
		FileWriter fw = null;
		// Open FileWriter
		try {
			fw = new FileWriter(file);
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
		// Loop
		do {
			packet = super.receivePacket();
			data = packet.getData();
			if (data[0] != 0 || data[1] != 3)
				throw new IllegalArgumentException();

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
				fw.write(bytesToWrite);
			} catch (IOException e) {
				Log.err(e.getStackTrace().toString());
			}
			
			//Send the acknowledge
			super.sendPacket(ack);
		} while (bytesToWrite.length == Var.BLOCK_SIZE);
		
		try {
			fw.close();
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
		super.close();
	}

	
}
