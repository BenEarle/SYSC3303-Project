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
		boolean firstLoop = true;
		FileWriter fw = null;
		byte[] ack = Var.ACK_WRITE.clone(); // initial ack message

		// Send initial Acknowledge
		super.sendPacket(ack);
		ack = ackIncrement(ack);
		boolean firstData = true;
		// Loop until all packets are received
		while(true) {
			// receive packet
			packet = super.receivePacket();
			if (packet != null) {
				// If this is the first data open up the file writer.
				if (firstData) {
					firstData = false;
					try {
						fw = new FileWriter(Var.SERVER_ROOT + file);
					} catch (IOException e) {
						if (e.getMessage().equals("Access is denied")) {
							TFTPErrorHelper.sendError(udp, (byte) 2, "Access is denied.");
						} else if (e.getMessage().equals("File already exists")) {
							TFTPErrorHelper.sendError(udp, (byte) 6, "File already exists.");
						} else {
							Log.err("ERROR Starting file writer", e);
						}
						super.close();
						return;
					}
				}
				
				// Check the data packet is valid.
				Integer check = TFTPErrorHelper.dataPacketChecker(udp, packet, ack[2] * 256 + ack[3], firstLoop);
				if (check == null) {
					// Valid packet received, continue normally.
					
					// Write the block to the file
					try {
						fw.write(packet.getData(), 4, packet.getLength());
					} catch (IOException e) {
						if (e.getMessage().equals("There is not enough space on the disk"))
							TFTPErrorHelper.sendError(udp, (byte) 3, "Disk full, cannot complete operation.");
						else
							TFTPErrorHelper.sendError(udp, (byte) 3, "File writing exception: " + e.getMessage());
						
						try {
							fw.abort();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						super.close();
						return;
					}
					
					// Send the ack packet, then increment the ack block number.
					super.sendPacket(ack);
					ack = ackIncrement(ack);
					if (Byte.toUnsignedInt(ack[2]) * 256 + Byte.toUnsignedInt(ack[3]) == 65335) {
						firstLoop = false;
					}
					if (packet.getLength() != Var.BUF_SIZE) {
						break;
					}
				} else if (check == -1) {
					// Ignore the packet.
				} else {
					// Unrecoverable error encountered, send error packet and exit.
					
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
			} else {
				System.out.println("Server<WriteThread>: Connection timed out, file transfer failed.");
				if (fw != null)
					try {
						fw.abort();
					} catch (IOException e) {
						e.printStackTrace();
					}
				super.close();
				return;
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
