
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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

	/**
	 * Main runner for the client, controls responses to user prompts
	 * @throws IOException
	 */
	public void run() throws IOException {

		Log.out("Client: Starting Client");
		System.out.println(Var.CMDStart);
		System.out.println("Client: Type 'help' to get a list of available commands.");
		running = true;
		while (running) {
			udp.setResendOnTimeout(true);
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
			switch (StrIn.toLowerCase()) {
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
			case ("i"):
			case ("ip"):
				this.getServerIP();
				break;
			case ("cd"):
			case ("c"):
				String newPath = getUserInput("New Path: ");
				if(newPath.charAt(newPath.length()) != '\\' || newPath.charAt(newPath.length()) != '/') {
					newPath += '\\';
				} 
				Var.CLIENT_ROOT = newPath;
				break;
			case ("ls"):
			case ("dir"):
				System.out.println("Current server directory: " + Var.CLIENT_ROOT);
			case ("h"):
			case ("help"):
				System.out.println("Client: Available commands: read, write, verbose, test, help, ip, cd.");
				break;
			default:
				System.out.println("Client: Unrecognized command, type 'help' for a list of commands.");
				break;
			}

			if (RRQ || WRQ) {
				try {

					// Go into appropriate mode to receive message
					if (RRQ) {
						readMode(file);
					} else if (WRQ) {
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

	/**
	 * Mode for reading a file from server and saving it locally
	 * @param fileName the file we are interested in reading off the server
	 * @throws IOException
	 */
	private void readMode(String fileName) throws IOException {

		DatagramPacket packet = null; // packet to send and receive data during
										// transfer
		boolean lastPacket = false; // flag to indicate transfer is ending
		byte[] data; // Data in packet

		// Initialize packet block number to receive first block of data
		byte[] blockNum = new byte[2];
		blockNum[0] = 0x00;
		blockNum[1] = 0x01;
		boolean firstData = true;
		boolean firstLoop = true;
		FileWriter writer = null;

		try {
			writer = new FileWriter(Var.CLIENT_ROOT + fileName);
		} catch (IOException e) {
			if (e.getMessage().equals("Access is denied")) {
				TFTPErrorHelper.sendError(udp, (byte) 2, "Access is denied.");
				Log.err("File already exists.");
				udp.setTestSender(false);
				return;
			} else if (e.getMessage().equals("File already exists")) {
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
				// Check the data packet is valid.
				Integer check = TFTPErrorHelper.dataPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1], firstLoop);
				if (check == null) {
					// Valid packet received, continue normally.

					if (firstData) {
						firstData = false;
						// Save address to send response to
						udp.setReturn(packet);
						udp.setTestSender(true);
					}

					// Get bytes to write to file from packet

					// Flag as last data packet if not full block size
					if (packet.getLength() != Var.BUF_SIZE)
						lastPacket = true;

					// write the block to the file
					try {
						writer.write(packet.getData(), 4, packet.getLength());
					} catch (IOException e) {
						if (e.getMessage().equals("There is not enough space on the disk"))
							TFTPErrorHelper.sendError(udp, (byte) 3, "Disk full, cannot complete operation.");
						else
							TFTPErrorHelper.sendError(udp, (byte) 3, "File writing exception: " + e.getMessage());

						try {
							writer.abort();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						udp.setTestSender(false);
						if (writer != null)
							writer.abort();
						return;
					}

					// Send the acknowledge packet
					udp.sendPacket(makeData(Var.ACK, blockNum));
					blockNum = bytesIncrement(blockNum);
					if (Byte.toUnsignedInt(blockNum[0]) * 256 + Byte.toUnsignedInt(blockNum[1]) == 65335) {
						firstLoop = false;
					}
				} else if (check == -1) {
					// Ignore the packet.
				} else {
					// Unrecoverable error encountered, send error packet and
					// exit.

					if (TFTPErrorHelper.isError(packet.getData()))
						TFTPErrorHelper.unPackError(packet);
					udp.setTestSender(false);
					if (writer != null)
						writer.abort();
					return;
				}
			} else {
				System.out.println("Connection timed out, file transfer failed.");
				udp.setTestSender(false);
				if (writer != null)
					writer.abort();
				return;
			}
		}
		System.out.println("Client: Read Operation Successful");
		// Close output stream
		writer.close();
		udp.setTestSender(false);
	}

	/**
	 * Mode for writing a file to the server
	 * @param fileName local file we are interesting in writing to the server
	 * @throws IOException
	 */
	private void writeMode(String fileName) throws IOException {
		// Data for transfer
		FileReader reader = null;
		boolean firstLoop = true;
		try {
			reader = new FileReader(Var.CLIENT_ROOT + fileName);
		} catch (IOException e) {
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
				// Check the ack packet is valid.
				Integer check = TFTPErrorHelper.ackPacketChecker(udp, packet, Byte.toUnsignedInt(blockNum[0]) * 256 + Byte.toUnsignedInt(blockNum[1]), firstLoop);
				if (check == null) {
					// Valid packet received, continue normally.

					if (firstPacket) {
						// Save address to send data to
						udp.setReturn(packet);
						udp.setTestSender(true);
						firstPacket = false;
					}

					// Get data from file and check if length read is less than
					// full block size
					try {
						data = reader.read(4);

						if (reader.isClosed()) {
							lastPacket = true;
						}
					} catch (IOException e) {
						TFTPErrorHelper.sendError(udp, (byte) 2, "Access denied for " + fileName + ".");
						udp.setTestSender(false);
						return;
					}

					// Increment Block Number
					blockNum = bytesIncrement(blockNum);
					if (Byte.toUnsignedInt(blockNum[0]) * 256 + Byte.toUnsignedInt(blockNum[1]) == 65335) {
						firstLoop = false;
					}
					// Add OPCode to data.
					data[0] = Var.DATA[0];
					data[1] = Var.DATA[1];
					data[2] = blockNum[0];
					data[3] = blockNum[1];

					// Send write data to Server
					udp.sendPacket(data);
				} else if (check == -1) {
					// Ignore the packet.
				} else {
					if (TFTPErrorHelper.isError(packet.getData()))
						TFTPErrorHelper.unPackError(packet);
					udp.setTestSender(false);
					return;
				}
			} else {
				System.out.println("Connection timed out, file transfer failed.");
				udp.setTestSender(false);
				return;
			}
		}

		// Receive final ACK packet
		packet = udp.receivePacket();
		if (packet == null)
			System.out.println("Never recieved the final acknowledgement, please check the validity of the transfer.");
		else if (TFTPErrorHelper.ackPacketChecker(udp, packet, blockNum[0] * 256 + blockNum[1],firstLoop) != null) {
			if (TFTPErrorHelper.isError(packet.getData()))
				TFTPErrorHelper.unPackError(packet);
			udp.setTestSender(false);
			return;
		}
		data = packet.getData();
		// Confirm packet is ACK
		System.out.println("Client: Write operation successful.");
		// Close file
		reader.close();
		udp.setTestSender(false);
	}

	/**
	 * Increments a byte array of two bytes
	 * @param data
	 * @return
	 */
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

	/**
	 * Prompts the user to get the ip where the server is running
	 */
	private void getServerIP() {
		String serverIP = getUserInput("IP of Destination: ");
		try {
			if (InetAddress.getByName(serverIP).isReachable(100)){
				if (serverIP.equals("")) {
					System.out.println("\tIP destination now: localHost");
				} else {
					System.out.println("\tIP destination now: " + serverIP);
				}
				addrServer = new InetSocketAddress(serverIP, Var.PORT_SERVER);
				addrHost = new InetSocketAddress(serverIP, Var.PORT_CLIENT);
				return;
			} else {
				//Timeout hit, destination unreachable
				System.out.println("\tNo response from IP: " + serverIP
						+ "\n\tPlease check connection or try a valid IP");
			}
		} catch (UnknownHostException e) {
			//Caught an error in the host ip and so is unreachable
			System.out.println("\tProblem setting destination IP to " + serverIP +
							   "\n\tPlease try again with a valid IP address");
		} catch (IOException e) {
			//Caught an io exception, cant recover
			System.out.println("\tProblem setting destination IP to " + serverIP +
							   "\n\tPlease make sure the Host has Internet Access");
		}
		System.out.println("\tDestination IP staying: " + addrServer.getHostString());		
	}


	/**
	 * Makes the data section of the packet to respond
	 * @param bytes the information to be inputed into the data
	 * @return byte array with all the data combined
	 */
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
		// it

		return buffer;
	}

	/**
	 * Ends the running and closes the sockets
	 */
	public void close() {
		if (running) {
			running = false;
			udp.close();
			reader.close();
		}
	}

	/**
	 * Check to see if it is running
	 * @return true if running and false if not running
	 */
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
