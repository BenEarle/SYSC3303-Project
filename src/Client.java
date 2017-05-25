
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Scanner;

import util.FileReader;
import util.FileWriter;
import util.Log;
import util.TFTPErrorHelper;
import util.UDPHelper;
import util.Var;

public class Client {
	private static final String MODE = "netASCII";

	private UDPHelper udp;

	private InetSocketAddress addrHost, addrServer;
	private boolean running;
	private boolean testMode;
	Scanner reader;

	public Client(InputStream in) throws SocketException {
		udp = new UDPHelper(true);

		addrHost = new InetSocketAddress("localhost", Var.PORT_CLIENT);
		addrServer = new InetSocketAddress("localhost", Var.PORT_SERVER);
		reader = new Scanner(in);
		this.testMode = false;
	}

	public Client(InputStream in, boolean testMode) throws SocketException {
		this(in);
		this.testMode = testMode;
	}

	// Take input, send request to server, and enter required mode to process
	// transfer
	public void run() throws IOException {

		Log.out("Client: Starting Client");
		System.out.println(Var.CMDStart);
		System.out.println("Client: Type 'help' to get a list of available commands.");

		running = true;
		while (running) {

			if (testMode) {
				udp.setIP(addrHost.getAddress());
				udp.setPort(addrHost.getPort());
			} else {
				udp.setIP(addrServer.getAddress());
				udp.setPort(addrServer.getPort());
			}

			boolean RRQ = false, WRQ = false;
			String file = null;
			String StrIn = getUserInput("Client: ");
		//	byte[] data = new byte[Var.BUF_SIZE];
			// Create and send request according to input
			switch (StrIn.toLowerCase()) {
//			case("fill"):
//				FillItUp.fillMyDrive(6);
//				break;
			case ("r"):
			case ("read"):
				file = getUserInput("Filename: ");
				RRQ = true;
				break;
			case ("w"):
			case ("write"):
				file = getUserInput("Filename: ");
				WRQ = true;
				break;
			case ("v"):
			case ("verbose"):
				System.out.print("Client: Verbose mode is now ");
				if (Log.toggleEnable())
					System.out.println("enabled.");
				else
					System.out.println("disabled.");
				break;
			case ("test"):
			case ("t"):
				testMode = !testMode;
				System.out.print("Client: Test mode is now ");
				if (testMode)
					System.out.println("enabled.");
				else
					System.out.println("disabled.");
				break;
			case ("s"):
			case ("shutdown"):
			case ("quit"):
			case ("q"):
				close();
				break;
			case ("h"):
			case ("help"):
				System.out.println("Client: Available commands: read, write, verbose, test, help.");
				break;
			default:
				System.out.println("Client: Unrecognized command, type 'help' for a list of commands.");
				break;
			}

			if (RRQ || WRQ) {
				try {
					
					// Go into appropriate mode to receive message
					if (RRQ) {
						// Log.packet("Client: Sending READ",
						// udp.getLastPacket());
						readMode(file);
					} else if (WRQ) {
						// Log.packet("Client: Sending WRITE",
						// udp.getLastPacket());
						writeMode(file);
					}
				} catch (SocketException e) {
					if (running) {
						throw e;
					}
				}
			}
		}

	}

	/*************************************************************************/
	// Mode for reading a file from server and saving it locally
	/*************************************************************************/
	private void readMode(String fileName) throws IOException {

		DatagramPacket packet = null; // packet to send and receive data during
										// transfer
		boolean lastPacket = false; // flag to indicate transfer is ending
		byte[] data; // Data in packet
		byte[] bytesToWrite; // Bytes to be written to file

		// Initialize packet block number to receive first block of data
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x01;
		boolean firstData = true;
		FileWriter writer = null;
		
		try {
			writer = new FileWriter(Var.CLIENT_ROOT + fileName);
		} catch (IOException e) {
			if (e.getMessage().equals("File already exists")) {
				Log.err("File already exists.");
				udp.setTestSender(false);
				return;
			}
			throw e;
		}
		data = makeData(Var.READ, fileName.getBytes(), Var.ZERO, MODE.getBytes(), Var.ZERO);
		udp.sendPacket(data);
		
		// Loop until last data packet is received
		while (!lastPacket) {
			// Create packet then receive and get info from packet
			packet = udp.receivePacket();
			if (packet != null) {
				if (TFTPErrorHelper.dataPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1]) != null) {
					if (TFTPErrorHelper.isError(packet.getData()))
						TFTPErrorHelper.unPackError(packet);
					udp.setTestSender(false);
					if (writer != null)
						writer.abort();
					return;
				}
				if (firstData) {
					firstData = false;
					// Save address to send response to
					udp.setReturn(packet);
					udp.setTestSender(true);
				}
				data = packet.getData();
				// Log.packet("Client: Receiving READ DATA",
				// udp.getLastPacket());

				// Get bytes to write to file from packet
				bytesToWrite = new byte[packet.getLength() - 4];
				System.arraycopy(data, 4, bytesToWrite, 0, bytesToWrite.length);

				// Flag as last data packet if not full block size
				if (bytesToWrite.length != Var.BLOCK_SIZE)
					lastPacket = true;

				// write the block to the file
				try {
					writer.write(bytesToWrite);
				} catch (IOException e) {
				//	System.out.println(e.getMessage());
					if(e.getMessage().equals("There is not enough space on the disk"))
						TFTPErrorHelper.sendError(udp, (byte) 3, "Disk full, cannot complete opperation.");
					writer.abort();
					return;
				}

				// Send the acknowledge packet
				udp.sendPacket(makeData(Var.ACK, blockNum));
				blockNum = bytesIncrement(blockNum);
				// Log.packet("Client: Sending READ ACK", udp.getLastPacket());
			}
		}
		System.out.println("Client: Read Operation Successful");
		// Close output stream
		writer.close();
		udp.setTestSender(false);
	}

	/*************************************************************************/
	// Mode for writing a file to the server
	/*************************************************************************/
	private void writeMode(String fileName) throws IOException {
		// Data for transfer
		FileReader reader = null;
		try {
			reader = new FileReader(Var.CLIENT_ROOT + fileName);
		} catch (IOException e){
			if (e.getMessage().contains("Access is denied"))
				Log.err("Access denied for " + fileName + ".");
			else 
				Log.err("File " + fileName + " not found.");
			return;
		}

		// This DataGram is used to receive ack
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		boolean lastPacket = false; // flag to indicate transfer is ending
		byte[] data; // Data in packet

		// Initialize blockNum to accept initial ack
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x00;
		boolean firstPacket = true;
		data = makeData(Var.WRITE, fileName.getBytes(), Var.ZERO, MODE.getBytes(), Var.ZERO);
		udp.sendPacket(data);
		
		// Loop until last data packet is sent
		while (!lastPacket) {
			// Create packet then receive and get info from packet
			packet = udp.receivePacket();
			if (packet != null) {
				if (TFTPErrorHelper.ackPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1]) != null) {
					
					if (TFTPErrorHelper.isError(packet.getData()))
						TFTPErrorHelper.unPackError(packet);
					udp.setTestSender(false);
					return;
				}

				if (firstPacket) {
					// Save address to send data to
					udp.setReturn(packet);
					udp.setTestSender(true);
					firstPacket = false;
				}
				data = packet.getData();
				// Log.packet("Client: Receiving WRITE ACK",
				// udp.getLastPacket());

				// Get data from file and check if length read is less than
				// full block size
				try {
					data = reader.read(4);

					if (reader.isClosed()) {
						lastPacket = true;
					}
				} catch (IOException e) {
					TFTPErrorHelper.sendError(udp, (byte) 2, "Access denied for " + fileName + ".");
					return;
				}
				blockNum = bytesIncrement(blockNum);

				// Add OPCode to data.
				data[0] = Var.DATA[0];
				data[1] = Var.DATA[1];
				data[2] = blockNum[0];
				data[3] = blockNum[1];

				// Send write data to Server
				udp.sendPacket(data);
				// Log.packet("Client: Sending WRITE Data",
				// udp.getLastPacket());

			}
		}

		// Receive final ACK packet
		packet = udp.receivePacket();
		if (packet == null) System.out.println("Server<ReadThread>: Server never recieved the final ack packet, please check the validity of the transfer.");
		else if (TFTPErrorHelper.ackPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1]) != null) {
			if (TFTPErrorHelper.isError(packet.getData()))
				TFTPErrorHelper.unPackError(packet);
			udp.setTestSender(false);
			return;
		}
		data = packet.getData();
		// Log.packet("Client: Receiving FINAL WRITE ACK", udp.getLastPacket());
		// Confirm packet is ACK
		System.out.println("Client: Write operation successful.");
		// Close file
		reader.close();
		udp.setTestSender(false);
	}

	private byte[] bytesIncrement(byte[] data) {
		if (data[1] == -1) {
			data[0]++;
			data[1] = 0x00;
		} else {
			data[1]++;
		}
		return data;
	}

	/**
	 * Prompts the user for input with the given prompt
	 * 
	 * @param prompt
	 *            string to display to the user
	 * @return the input from the user
	 */
	private String getUserInput(String prompt) {
		System.out.print(prompt);
		String s = "";
		try {
			if (reader.hasNextLine()) {
				s = reader.nextLine();
				// System.out.println();
			}
		} catch (IllegalStateException e) {
			if (running) {
				throw e;
			}
		}
		return s;
	}

	private byte[] makeData(byte[]... bytes) {
		// Get the required length of the byte array.
		int length = 0;
		for (byte[] b : bytes) {
			length += b.length;
			if (length > Var.BUF_SIZE) {
				// If the length is too much then return;
				return null;
			}
		}

		// Create the buffer to hold the full array.
		byte[] buffer = new byte[length];

		// Copy each byte array into the buffer.
		int i = 0;
		for (byte[] b : bytes) {
			System.arraycopy(b, 0, buffer, i, b.length);
			i += b.length;
		}

		// Create a packet from the buffer (using the host address) and return
		// it.
		return buffer;
	}

	public void close() {
		if (running) {
			running = false;
			udp.close();
			reader.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

	public static void main(String[] args) throws SocketException, IOException {
		Log.enable(false);
		boolean test = false;
		if (args.length > 0) {
			int i;
			for (i = 0; i < args.length; i++) {
				if (args[i].equals("v") || args[i].equals("V")) {
					Log.enable(true);
				}
				if (args[i].equals("t") || args[i].equals("T")) {
					test = true;
				}
			}
		}
		new Client(System.in, test).run();
	}

}
