import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import util.Log;
import util.Var;

public class Server {
	private DatagramSocket socRecv;
	private boolean running;

	public Server() throws SocketException {
		socRecv = new DatagramSocket(Var.PORT_SERVER);
	}

	public void run() throws IOException {
		running = true;

		while (running) {
			// Wait to get a packet.
			DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			socRecv.receive(packet);
			Log.packet("Server Receive", packet);

			// Read the packet to make sure it's valid, then respond with the appropriate code on a new socket.
			int res = readPacket(packet);
			DatagramSocket socSend = new DatagramSocket();
			switch (res) {
				case 1:
					socSend.send(new DatagramPacket(Var.S_READ, Var.S_READ.length, packet.getSocketAddress()));
					Log.packet("Server Sending READ", packet);
					break;
				case 2:
					socSend.send(new DatagramPacket(Var.S_WRITE, Var.S_WRITE.length, packet.getSocketAddress()));
					Log.packet("Server Sending WRITE", packet);
					break;
				default:
					Log.out("Server got invalid packet, closing.");
					close();
			}
			socSend.close();
		}
	}

	/**
	 * 
	 * @return 0:fail, 1:read, 2:write
	 */
	public int readPacket(DatagramPacket packet) {
		byte[] buf = packet.getData();
		int length = packet.getLength();
		int i = 2;
		int type = 0;

		// Check the READ/WRITE code is correct.
		if (buf[0] == Var.READ[0] && buf[1] == Var.READ[1]) {
			type = 1;
		} else if (buf[0] == Var.WRITE[0] && buf[1] == Var.WRITE[1]) {
			type = 2;
		} else {
			return 0;
		}

		// Capture the filename, if the buffer is overrun the packet is invalid.
		do {
			if (i >= length) {
				return 0;
			}
		} while (buf[i++] != 0);
		String filename = new String(buf, 2, i - 3);
		Log.out("\tfilenm\t" + filename);

		// Capture the mode, if the buffer is overrun the packet is invalid.
		int start = i;
		do {
			if (i >= length) {
				return 0;
			}
		} while (buf[i++] != 0);
		String mode = new String(buf, start, i - start - 1);
		Log.out("\tmode\t" + mode);
		// Check the NETASCII/OCTET mode is correct.
		if (!mode.toLowerCase().equals("netascii") && !mode.toLowerCase().equals("octet")) {
			return 0;
		}

		// Make sure only a single 0 at the end.
		if (i != length) {
			return 0;
		}

		return type;
	}

	public void close() {
		if (running) {
			running = false;
			socRecv.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

	public static void main(String[] args) throws SocketException {
		new Server().close();
	}

}
