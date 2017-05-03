import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import util.Log;
import util.Var;

public class Client {
	private static final String FILENAME = "test.txt";
	private static final String MODE = "netASCII";
	
	private DatagramSocket socket;
	private InetSocketAddress addrHost;
	private boolean running;
	
	public Client() throws SocketException {
		socket = new DatagramSocket();
		addrHost = new InetSocketAddress("localhost", Var.PORT_CLIENT);
	}
	
	public void run() throws IOException {
		DatagramPacket packet;
		running = true;
		
		// Send 10 packets alternating between read and write.
		for (int i = 0; i < 10; i++) {
			if (i % 2 != 0) {	// READ
				packet = makePacket(Var.READ, FILENAME.getBytes(), Var.ZERO, MODE.getBytes(), Var.ZERO);
				Log.packet("Client Sending READ", packet);
			} else {			// WRITE
				packet = makePacket(Var.WRITE, FILENAME.getBytes(), Var.ZERO, MODE.getBytes(), Var.ZERO);
				Log.packet("Client Sending WRITE", packet);
			}
			socket.send(packet);

			socket.receive(packet);
			Log.packet("Client Receive", packet);
		}
		
		// Send out an invalid packet.
		packet = makePacket(Var.WRITE, "aaaa".getBytes(), new byte[] {0, 2, 6});
		Log.packet("Client Sending Invalid", packet);
		socket.send(packet);
	}
	
	private DatagramPacket makePacket(byte[]... bytes) {
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
		return new DatagramPacket(buffer, buffer.length, addrHost);
	}
	
	public void close() {
		if (running) {
			running = false;
			socket.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

	public static void main(String[] args) throws SocketException, IOException {
		new Client().run();
	}

}
