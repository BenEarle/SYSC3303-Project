import java.io.FileNotFoundException;
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
	 * Receive RRQ Open Socket and File do Read data from file Send data Receive
	 * ack while(data.len == 512)
	 */

	public void run() {
		DatagramPacket packet;
		FileReader fr = null;
		try {
			fr = new FileReader(Var.SERVER_ROOT + file);
		} catch (FileNotFoundException e) {
			// There is potential here to send a file not found error back to
			// the client.
			Log.err("ERROR Starting file reader", e);
			super.close();
			return;
		}

		// Initialize block index to send first block
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x01;

		boolean lastPacket = false; // flag to indicate if last packet is sent
		byte[] data = null; // data from file
		byte[] msg = null; // bytes in message to send

		// Get data from file
		try {
			data = fr.read();
			// Check if length read is less than full block size
			if (data.length != Var.BLOCK_SIZE)
				lastPacket = true;
		} catch (Exception e) {
			data = new byte[0]; // Empty Message
			lastPacket = true;
		}

		// Build message from data
		msg = new byte[4 + data.length];
		msg[0] = Var.DATA[0];
		msg[1] = Var.DATA[1];
		msg[2] = blockNum[0];
		msg[3] = blockNum[1];
		for (int i = 0; i < data.length; i++)
			msg[i + 4] = data[i]; // Copy data into message

		// Send first data packet
		Log.out("Server<ReadThread>: Sending Initial READ Data");
		super.sendPacket(msg);

		// Loop until all packets are sent
		while (!lastPacket) {
			// Receive packet
			Log.out("Server<ReadThread>: Receiving ACK Data");
			packet = super.receivePacket();
			if (TFTPErrorHelper.ackPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1]) == null) {
				// Get data from file and check if length read is less than
				// full block size
				try {
					data = fr.read();
					if (data.length != Var.BLOCK_SIZE)
						lastPacket = true;
					// Exception if no bytes left in file. Send last packet
					// empty
				} catch (Exception e) {
					data = new byte[0]; // Empty Message
					lastPacket = true;
				}

				// Increment Block Number
				blockNum = bytesIncrement(blockNum);

				// Build message from data
				msg = new byte[4 + data.length];
				msg[0] = Var.DATA[0];
				msg[1] = Var.DATA[1];
				msg[2] = blockNum[0];
				msg[3] = blockNum[1];
				for (int i = 0; i < data.length; i++)
					msg[i + 4] = data[i]; // Copy data into message

				// Send Packet
				Log.out("Server<ReadThread>: Sending READ Data");
				super.sendPacket(msg);

			} else {
				System.out.println("Server<ReadThread>: Invalid ACK packet.");
				super.close();
				return;
			}
		}

		// Receive final ACK packet
		Log.out("Server<ReadThread>: Receiving Final ACK Data");
		packet = super.receivePacket();
		if (TFTPErrorHelper.ackPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1]) != null) {
			System.out.println("Server<ReadThread>: Invalid ACK packet.");
			super.close();
			return;
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
		if (data[1] == 0xff) {
			data[0]++;
			data[1] = 0x00;
		} else {
			data[1]++;
		}
		return data;
	}

}
