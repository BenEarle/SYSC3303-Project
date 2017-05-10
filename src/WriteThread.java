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
		byte[] ack = { 0, 4, 0, 0 }; // initial ack message
		byte[] bytesToWrite = null;
		
		// Open FileWriter
		FileWriter fw = null;
		
		// Send initial Acknowledge
		Log.out("Server<WriteThread>: Sending Initial ACK" + ack.toString());
		super.sendPacket(ack);
		boolean firstData = true;
		// Loop until all packets are received
		do {
			// receive packet
			packet = super.receivePacket();
			if(firstData){
				firstData = false;
				try {
					fw = new FileWriter(Var.SERVER_ROOT + file);
				} catch (IOException e) {
					Log.err("ERROR Starting file writer",e);
					super.close();
					return;
				}
			}
			Log.packet("Server<WriteThread>: Received WRITE Data", packet);
			data = packet.getData();
			ack = ackIncrement(ack);
			if (TFTPErrorHelper.dataPacketChecker(udp, packet, ack[2] * 256 + ack[3]) != null){
				System.out.println("Server<ReadThread>: Invalid data packet.");
				super.close();
				return;
			}
			// Make an array with the bytes to write
			bytesToWrite = new byte[packet.getLength() - 4];
			System.arraycopy(data, 4, bytesToWrite, 0, bytesToWrite.length);

			// Write the block to the file
			try {
				fw.write(bytesToWrite);
			} catch (IOException e) {
				Log.err("ERROR writing to file", e);
			}

			// Send the acknowledge
			Log.out("Server<WriteThread>: Sending WRITE ACK" + ack.toString());
			super.sendPacket(ack);
		} while (bytesToWrite.length == Var.BLOCK_SIZE);

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
