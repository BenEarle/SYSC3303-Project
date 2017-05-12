import java.io.IOException;
import java.net.DatagramPacket;
import util.FileWriter;
import util.Log;
import util.TFTPErrorHelper;
import util.Var;

/*************************************************************************/
// This class is instantiated when the main server listener thread receives
// a write request. It will open the file and follow algorithm shown below
// to read from a file and send the data back to the server.
/*************************************************************************/

public class WriteThread extends ClientResponseThread {

	public WriteThread(DatagramPacket initialPacket) {
		super(initialPacket);
	}

	/*
	 * Algorithm for writing to a file:
	 * 
	 * Send ACK packet 0400
	 * Open FileWriter 
	 * do 
	 *  receive DATA 
	 *  ACK[3]++ 
	 *  write the data to the file 
	 *  filewriter.write(Data) 
	 *  send 
	 *  ACK 
	 * while(data.len == 512) socket.close()
	 * filewriter.close()
	 */
	
	public void run() {
		DatagramPacket packet;
		byte[] data;
		byte[] ack = Var.ACK_WRITE.clone(); // initial ack message

		// Open FileWriter
		FileWriter fw = null;

		// Send initial Acknowledge
		super.sendPacket(ack);
		boolean firstData = true;
		// Loop until all packets are received
		do {
			// receive packet
			packet = super.receivePacket();
			if (packet != null) {
				if (firstData) {
					firstData = false;
					try {
						fw = new FileWriter(Var.SERVER_ROOT + file);
					} catch (IOException e) {
						Log.err("ERROR Starting file writer", e);
						super.close();
						return;
					}
				}
	
				data = packet.getData();
				ack = ackIncrement(ack);
				if (TFTPErrorHelper.dataPacketChecker(udp, packet, ack[2] * 256 + ack[3]) != null) {
					System.out.println("Server<ReadThread>: Invalid data packet.");
					super.close();
					return;
				}
	
				// Write the block to the file
				try {
					fw.write(data, 4, packet.getLength());
				} catch (IOException e) {
					Log.err("ERROR writing to file", e);
				}
	
				// Send the acknowledge
				super.sendPacket(ack);
			}
		} while (packet.getLength() == Var.BUF_SIZE);

		// Close input stream
		try {
			fw.close();
		} catch (IOException e) {
			Log.err("ERROR Closing file writer", e);
		}
		super.close();
		// Sorry they share a command line...
		System.out.println("Server<WriteThread>: Write completed successfully.");
		System.out.print("Server<Main>: ");
	}

	private byte[] ackIncrement(byte[] data) {
		if (data[3] == 0xff) {
			data[2]++;
			data[3] = 0x00;
		} else {
			data[3]++;
		}
		return data;
	}
}
