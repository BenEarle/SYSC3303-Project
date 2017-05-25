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
		while(true) {
			// receive packet
			packet = super.receivePacket();
			if (packet != null) {
				if (firstData) {
					firstData = false;
					try {
						fw = new FileWriter(Var.SERVER_ROOT + file);
					} catch (IOException e) {
						if (e.getMessage().equals("File already exists")) {
							TFTPErrorHelper.sendError(udp, (byte) 6, "File already exists.");
						} else {
							Log.err("ERROR Starting file writer", e);
						}
						super.close();
						return;
					}
				}
	
				data = packet.getData();
				if(data[2] * 256 + data[3] == ack[2] * 256 + ack[3] +1)
					ack = ackIncrement(ack);
				if (TFTPErrorHelper.dataPacketChecker(udp, packet, ack[2] * 256 + ack[3]) != null) {
					//System.out.println("Server<ReadThread>: Invalid data packet.");
					if (TFTPErrorHelper.isError(packet.getData()))
						TFTPErrorHelper.unPackError(packet);
					if (fw != null)
						try {
							fw.abort();
						} catch (IOException e) {
							e.printStackTrace();
						}
					super.close();
					return;
				}
	
				// Write the block to the file
				try {
					fw.write(data, 4, packet.getLength());
				} catch (IOException e) {
					if (e.getMessage().equals("There is not enough space on the disk"))
						TFTPErrorHelper.sendError(udp, (byte) 3, "Disk full, cannot complete opperation.");
					try {
						fw.abort();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					return;
				}
				
		 
				super.sendPacket(ack);
				if (packet.getLength() != Var.BUF_SIZE) {
					break;
				}
			}
		}

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
		if (data[3] == -1) {
			data[2]++;
			data[3] = 0x00;
		} else {
			data[3]++;
		}
		return data;
	}
}
