import java.io.IOException;
import java.net.DatagramPacket;

import util.FileWriter;
import util.Log;
import util.Var;

public class WriteThread extends ClientResponseThread {

	public WriteThread(DatagramPacket initialPacket) {
		super(initialPacket);
	}
	// send ACK packet 0400
	//
	// make a file writer (filename)
	//
	// do
	// receive DATA
	// ACK[3]++
	// write the data to the file
	// filewriter.write(Data)
	// send ACK
	// while(data.len == 512)
	// final write
	// final ACK
	// socket.close()
	// filewriter.close()

	public void run() {
		int index = 0;
		byte[] data;
		//Send initial Acknowledge 
		byte[] ack = Var.ACK_WRITE;
		super.sendPacket(ack);
		
		//Open FileWriter
		try {
			FileWriter fw = new FileWriter(file);
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
		
		//Loop
		do {
			data = super.receivePacket();
			if(data[0] != 0 || data[1] != 3)
				throw new IllegalArgumentException();
			
			ack[2] = data[2];
			ack[3] = data[3];
			index ++;
			// Enforce correct packet number
			// ack 2 and 3 are a 
			if(ack[2]*256+ack[3] != index)
				throw new IllegalArgumentException();
			
			
		}while(true);

	}

}
