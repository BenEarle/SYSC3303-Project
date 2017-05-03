import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import util.Log;
import util.Var;

public abstract class ClientResponseThread extends Thread {
	protected DatagramSocket socket;
	protected String file, mode;
	protected InetAddress clientIP;
	protected int clientPort;
	
	ClientResponseThread(DatagramPacket initialPacket) {
		unpack(initialPacket);
		setUpSocket();
	}

	private void unpack(DatagramPacket p) {
		clientPort = p.getPort();
		clientIP = p.getAddress();
		byte[] data = p.getData();

		// parse packet confirming the format is correct
		if (data[0] != 0) // first bit must be 0
			throw new IllegalArgumentException();

		if (data[1] != 1 && data[1] != 2) // second bit must either be a 1 or 2
			throw new IllegalArgumentException();

		int i = 2;
		file = "";
		while (i < data.length) {
			if (data[i] == 0)
				break;
			file += (char) data[i];
			i++;
		}

		if (i == data.length)// if we didn't find a 0 it means that the string
								// is wrong
			throw new IllegalArgumentException();

		// Increment i again to skip over the null char
		i++;

		mode = "";
		while (i < data.length) {
			if (data[i] == 0)
				break;
			mode += (char) data[i];
			i++;
		}

		if (mode.length() < 1 || file.length() < 1)
			throw new IllegalArgumentException();
	}

	private void setUpSocket() {
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			Log.err("ERROR: Creating socket.");
			Log.err(e.getStackTrace().toString());
		}
	}
	
	@SuppressWarnings("unused")
	private void sendPacket(byte[] data){
		DatagramPacket sPacket = new DatagramPacket(data, data.length, clientIP, clientPort);
		try {
			socket.send(sPacket);
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
	}

	@SuppressWarnings("unused")
	private byte[] receivePacket(){
		byte[] data = new byte[Var.BUF_SIZE];
		DatagramPacket rPacket = new DatagramPacket(data, data.length);
		try {
			socket.receive(rPacket);
		} catch (IOException e) {
			Log.err(e.getStackTrace().toString());
		}
		return data;
	}
}
