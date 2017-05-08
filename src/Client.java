
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Scanner;

import util.FileReader;
import util.FileWriter;
import util.Log;
import util.Var;

public class Client {
	private static final String MODE = "netASCII";

	private DatagramSocket socket;
	private InetSocketAddress addrHost, addrServer;
	private boolean running;
	private boolean testMode;
	Scanner reader;

	public Client(InputStream in) throws SocketException {
		socket = new DatagramSocket();
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
		InetSocketAddress address;
		DatagramPacket packet = null;
		Log.out("Client: Starting Client");
		if (testMode) {
			address = addrHost;
		} else {
			address = addrServer;
		}

		running = true;
		while (running) {
			boolean RRQ = false, WRQ = false;
			String file = null;
			String StrIn = getUserInput("Client: ");
			// Create and send request according to input
			switch (StrIn) {
			case("R"):
			case("r"):
			case("read"):
				file = getUserInput("Filename: ");
				packet = makePacket(address, Var.READ, file.getBytes(), Var.ZERO, MODE.getBytes(), Var.ZERO);
				Log.packet("Client: Sending READ", packet);
				RRQ = true;
				break;
			case("W"):
			case("w"):
			case("write"):
				file = getUserInput("Filename: ");
				packet = makePacket(address, Var.WRITE, file.getBytes(), Var.ZERO, MODE.getBytes(),
						Var.ZERO);
				Log.packet("Client: Sending WRITE", packet);
				WRQ = true;
				break;
			case("V"):
			case("v"):
			case("verbose"):
				Log.enable(true);
				break;
			case("test"):
			case("T"):
			case("t"):
				testMode = !testMode;
				if (testMode) {
					address = addrHost;
				} else {
					address = addrServer;
				}
				break;
			case("h"):
			case("H"):
			case("help"):
				System.out.println("Client: Available commands: read, write, verbose, test, help.");
				break;
			default: 
				System.out.println("Client: Unrecognized command, type 'help' for a list of commands.");
				break;
			}
				
			if(RRQ || WRQ){
				try {
					socket.send(packet);
	
					// Go into appropriate mode to receive message
					if (RRQ) {
						readMode(file);
					} else if(WRQ) {
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

		InetSocketAddress address = null; // Where all ack packets are sent
		FileWriter writer = new FileWriter(Var.CLIENT_ROOT + fileName);

		DatagramPacket packet = null; // packet to send and receive data during
										// transfer
		boolean lastPacket = false; // flag to indicate transfer is ending
		byte[] data; // Data in packet
		byte[] bytesToWrite; // Bytes to be written to file

		// Initialize packet block number to receive first block of data
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x01;

		// Loop until last data packet is received
		while (!lastPacket) {
			// Create packet then receive and get info from packet
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			socket.receive(packet);
			data = packet.getData();
			Log.packet("Client: Receiving READ DATA", packet);
			// Save address to send response to
			address = new InetSocketAddress(packet.getAddress(), packet.getPort());

			// Ensure packet is Valid Data packet
			if (data[0] == Var.DATA[0] && data[1] == Var.DATA[1]) {
				// Ensure packet has correct block number
				if (blockNum[0] == data[2] && blockNum[1] == data[3]) {
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
						Log.err("", e);
					}

					// Send the acknowledge packet
					packet = makePacket(address, Var.ACK, blockNum);
					socket.send(packet);
					blockNum = bytesIncrement(blockNum);
					Log.packet("Client: Sending READ ACK", packet);

				} else
					throw new IndexOutOfBoundsException();
			} else
				throw new IllegalArgumentException();
		}
		System.out.println("Client: Read Operation Successful");
		// Close output stream
		writer.close();
	}

	/*************************************************************************/
	// Mode for writing a file to the server
	/*************************************************************************/
	private void writeMode(String fileName) throws IOException {
		// Data for transfer
		InetSocketAddress address = null; // Where all data packets are sent
		FileReader reader = new FileReader(Var.CLIENT_ROOT + fileName);
		// This DataGram is used to receive ack
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		boolean lastPacket = false; // flag to indicate transfer is ending
		byte[] data; // Data in packet

		// Initialize blockNum to accept initial ack
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x00;

		// Loop until last data packet is sent
		while (!lastPacket) {
			// Create packet then receive and get info from packet
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			socket.receive(packet);
			data = packet.getData();
			Log.packet("Client: Receiving WRITE ACK", packet);
			// Save address to send data to
			address = new InetSocketAddress(packet.getAddress(), packet.getPort());
			// Ensure packet is ack
			if (data[1] == Var.ACK[1]) {
				// Ensure packet has correct index
				if (data[2] == blockNum[0] && data[3] == blockNum[1]) {
					// Get data from file and check if length read is less than
					// full block size
					try {
						data = reader.read();
						if (data.length != Var.BLOCK_SIZE)
							lastPacket = true;
						// Exception if no bytes left in file. Send last packet
						// empty
					} catch (Exception e) {
						data = new byte[0]; // Empty Message
						lastPacket = true;
					}
					blockNum = bytesIncrement(blockNum);

					// Send write data to Server
					packet = makePacket(address, Var.DATA, blockNum, data);
					Log.packet("Client: Sending WRITE Data", packet);
					socket.send(packet);

				} else
					throw new IndexOutOfBoundsException();
			} else
				throw new IllegalArgumentException();
		}

		// Receive final ACK packet
		packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		socket.receive(packet);
		data = packet.getData();
		Log.packet("Client: Receiving FINAL WRITE ACK", packet);
		// Confirm packet is ACK
		if (data[1] == Var.ACK[1]) {
			// Confirm block number is correct
			if (data[2] == blockNum[0] && data[3] == blockNum[1]) {

				System.out.println("Write: Operation Successful");

			} else
				throw new IndexOutOfBoundsException();
		} else
			throw new IllegalArgumentException();

		// Close file
		reader.close();
	}

	private byte[] bytesIncrement(byte[] data) {
		if (data[1] == 0xff) {
			data[0]++;
			data[1] = 0x00;
		} else {
			data[1]++;
		}
		return data;
	}

	/**
	 * Prompts and collects the data from the user of filename and read or write
	 * 
	 * @return ArrayList where first element is if its read or write and second
	 *         element is filename
	 */
	private ArrayList<String> getRequestData() {
		ArrayList<String> data = new ArrayList<String>();
		String rorW = getUserInput("Client: ");
		if (rorW.equals("S")) {
			close();
		}
		String file = getUserInput("Filename: ");
		data.add(rorW);
		data.add(file);
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
				//System.out.println();
			}
		} catch (IllegalStateException e) {
			if (running) {
				throw e;
			}
		}
		return s;
	}

	private DatagramPacket makePacket(InetSocketAddress sendAddr, byte[]... bytes) {
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
		return new DatagramPacket(buffer, buffer.length, sendAddr);
	}

	public void close() {
		if (running) {
			running = false;
			socket.close();
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
