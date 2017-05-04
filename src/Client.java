import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	public Client() throws SocketException {
		socket = new DatagramSocket();
		addrHost = new InetSocketAddress("localhost", Var.PORT_CLIENT);
		addrServer = new InetSocketAddress("localhost", Var.PORT_SERVER);
		reader = new Scanner(System.in);
		this.testMode = false;
	}
	
	public Client(boolean testMode) throws SocketException {
		this();
		this.testMode = testMode;
	}
	
	public void run() throws IOException {
		InetSocketAddress address;
		DatagramPacket packet;
		running = true;
		Log.out("Starting Client");
		if (testMode) {
			address = addrHost;
		} else {
			address = addrServer;
		}
		while(true) {
			ArrayList<String> userData = getRequestData();
			if (userData.get(0).equals("R")) {
				packet = makePacket(address,Var.READ, userData.get(1).getBytes(), Var.ZERO,MODE.getBytes(), Var.ZERO);
				Log.packet("Client Sending READ", packet);
			} else {
				packet = makePacket(address, Var.WRITE, userData.get(1).getBytes(), Var.ZERO,MODE.getBytes(), Var.ZERO);
				Log.packet("Client Sending WRITE", packet);
			}
			socket.send(packet);
			
			socket.receive(packet);
			if (userData.get(0).equals("R")) {
				readMode(packet, userData.get(1));
			} else {
				writeMode(packet, userData.get(1));
			}
			Log.packet("Client Receive", packet);
		}
	}
	
	private void readMode(DatagramPacket packet, String fileName) throws IOException {
		byte[] fileData;
		int blockLength = packet.getLength();
		InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());
		FileWriter writer = new FileWriter(fileName);
		byte[] blockNum = new byte[2];
		while ((blockLength = packet.getLength()) % Var.BLOCK_SIZE == 0) {
			if (packet.getData()[1] == Var.DATA[1] && blockNum[0] == packet.getData()[2]
					&& blockNum[1] == packet.getData()[3]) {
				fileData = Arrays.copyOfRange(packet.getData(), 5, packet.getLength());
				writer.write(fileData);
				packet = makePacket(address, Var.ACK, blockNum);
				socket.send(packet);
				blockNum = bytesIncrement(blockNum);
			}
		}
		if (packet.getData()[1] == Var.DATA[1] && blockNum[0] == packet.getData()[2]
				&& blockNum[1] == packet.getData()[3]) {
			fileData = Arrays.copyOfRange(packet.getData(), 5, packet.getLength());
			writer.write(fileData);
			packet = makePacket(address, Var.ACK, blockNum);
			socket.send(packet);
			blockNum = bytesIncrement(blockNum);
		}
		writer.close();
	}
	
	private void writeMode(DatagramPacket packet, String fileName) throws IOException {
		InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());
		FileReader file = new FileReader(fileName);
		byte[] blockNum = new byte[2];
		blockNum[1] = 0x01;
		blockNum[0] = 0x00;
		byte[] data = file.read();
		int dataLength = data.length;
		packet = makePacket(address, Var.DATA, blockNum, data);
		socket.send(packet);
		Log.packet("Client Sending WRITE Data:", packet);
		while (dataLength % Var.BLOCK_SIZE == 0) {
			socket.receive(packet);
			if (packet.getData()[1] == Var.ACK[1]) {
				if (packet.getData()[2] == blockNum[0] && packet.getData()[3] == blockNum[1]) {
					data = file.read();
					dataLength = data.length;
					blockNum = bytesIncrement(blockNum);
					packet = makePacket(address, Var.DATA, blockNum, data);
					socket.send(packet);
					Log.packet("Client Sending WRITE Data", packet);
				}
			}
		}
		socket.receive(packet);
		if (packet.getData()[1] == Var.ACK[1]) {
			if (packet.getData()[2] == blockNum[0] && packet.getData()[3] == blockNum[1]) {
				Log.out("Write Seccuessful");
			}
		}
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
	 * @return ArrayList where first element is if its read or write and second element is filename
	 */
	private ArrayList<String> getRequestData() {
		ArrayList<String> data = new ArrayList<String>();
		String rorW = getUserInput("Read or Write or Shutdown ('R' or 'W' or 'S'): ");
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
	 * @param prompt string to display to the user
	 * @return the input from the user
	 */
	private String getUserInput(String prompt) {
		System.out.print(prompt);
		String s = reader.nextLine();
		System.out.println();
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
		
		// Create a packet from the buffer (using the host address) and return it.
		return new DatagramPacket(buffer, buffer.length, sendAddr);
	}
	
	public void close() {
		if (running) {
			running = false;
			socket.close();
			reader.close();
			System.exit(0);
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
	            if(args[i].equals("v") || args[i].equals("V")) {
	            	Log.enable(true);
	            }
	            if(args[i].equals("t") || args[i].equals("T")) {
	            	test = true;
	            }
	        }
		}
		new Client(test).run();
	}

}
