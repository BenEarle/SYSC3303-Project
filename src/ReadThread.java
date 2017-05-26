import java.io.IOException;
import java.net.DatagramPacket;

import util.FileReader;
import util.Log;
import util.TFTPErrorHelper;
import util.Var;

/*************************************************************************/
// This class is instantiated when the main server listener thread receives
// a read request. It will open the file and follow algorithm shown below
// to read from a file and send the data back to the server.
/*************************************************************************/

public class ReadThread extends ClientResponseThread {

	public ReadThread(DatagramPacket initialPacket) {
		super(initialPacket);
	}

	/*
	 * Algorithm for Reading from a file:
	 * 
	 * Receive RRQ
	 * Open Socket and File
	 * do
	 *  Read data from file
	 *  Send data
	 *  Receive
	 * ack while(data.len == 512)
	 */

	public void run() {
		DatagramPacket packet;
		FileReader fr = null;
		try {
			fr = new FileReader(Var.SERVER_ROOT + file);
		} catch (IOException e) {
			if (e.getMessage().contains("Access is denied"))
				TFTPErrorHelper.sendError(udp, (byte) 2, "Access denied for " + file + ".");
			else	
				TFTPErrorHelper.sendError(udp, (byte) 1, "File " + file + " not found.");
			return;
		}

		// Initialize block index to send first block
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x01;

		boolean lastPacket = false; // flag to indicate if last packet is sent
		byte[] data; // data from file

		// Get data from file
		try {
			data = fr.read(4);

			// Check if end of file has been reached.
			if (fr.isClosed()) {
				lastPacket = true;
			}
		} catch (Exception e) {
			if (e.getMessage().contains("locked")) {
				TFTPErrorHelper.sendError(udp, (byte) 2, "Access denied for " + file + ".");
				super.close(); 
				return;
			}
			Log.err("ERROR Reading file", e);
			data = new byte[4]; // Empty Message.
			lastPacket = true;
		}

		// Add OPCode to data.
		data[0] = Var.DATA[0];
		data[1] = Var.DATA[1];
		data[2] = blockNum[0];
		data[3] = blockNum[1];

		// Send first data packet
		Log.out("Server<ReadThread>: Sending Initial READ Data");
		super.sendPacket(data);

		// Loop until all packets are sent
		while (!lastPacket) {
			// Receive packet
			Log.out("Server<ReadThread>: Receiving ACK Data");
			packet = super.receivePacket();
			if (packet != null) {
				// Check the ack packet is valid.
				Integer check = TFTPErrorHelper.ackPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1]);
				if (check == null) {
					// Valid packet received, continue normally.

					// Get data from file
					try {
						data = fr.read(4);

						// Check if end of file has been reached.
						if (fr.isClosed()) {
							lastPacket = true;
						}
					} catch (IOException e) {
						if (e.getMessage().contains("locked")) {
							TFTPErrorHelper.sendError(udp, (byte) 2, "Access denied for " + file + ".");
							super.close(); 
							return;
						}
						Log.err("ERROR Reading file", e);
						data = new byte[4]; // Empty Message.
						lastPacket = true;
					}

					// Increment Block Number
					blockNum = bytesIncrement(blockNum);

					// Add OPCode to data.
					data[0] = Var.DATA[0];
					data[1] = Var.DATA[1];
					data[2] = blockNum[0];
					data[3] = blockNum[1];
	
					// Send Packet
					Log.out("Server<ReadThread>: Sending READ Data");
					super.sendPacket(data);
	
				} else if (check == -1) {
					// Ignore the packet.
				} else {
					// Unrecoverable error encountered, send error packet and exit.
					if (TFTPErrorHelper.isError(packet.getData())) {
						TFTPErrorHelper.unPackError(packet);
					}
					//System.out.println("Server<ReadThread>: Invalid ACK packet.");
					
					super.close();
					return;
				}
			} else {
				System.out.println("Server<ReadThread>: Connection timed out, file transfer failed.");
				super.close();
				return;
			}
		}

		// Receive final ACK packet
		Log.out("Server<ReadThread>: Receiving Final ACK Data");
		packet = super.receivePacket();
		if (packet == null) {
			System.out.println("Server<ReadThread>: Server never recieved the final ack packet, please check the validity of the transfer.");
		} else {
			Integer check = TFTPErrorHelper.ackPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1]);
			if (check != null && check != -1) {
				if (TFTPErrorHelper.isError(packet.getData()))
					TFTPErrorHelper.unPackError(packet);
				System.out.println("Server<ReadThread>: Invalid ACK packet.");
				super.close();
				return;
			}
		}

		// Close file
		try {
			fr.close();
		} catch (IOException e) {
			Log.err("ERROR Closing file reader", e);
		}
		super.close();
		// Sorry they share a command line...
		System.out.println("Server<ReadThread>: Read completed successfully.");
		System.out.print("Server<Main>: ");
	}

	/*************************************************************************/
	// This method is used to increment the byte number for the data packets.
	// It will increment the smaller byte until it overflows then increment
	// the larger one.
	/*************************************************************************/

	private byte[] bytesIncrement(byte[] data) {
		if (data[1] == -1) {
			data[0]++;
			data[1] = 0x00;
		} else {
			data[1]++;
		}
		return data;
	}

}
