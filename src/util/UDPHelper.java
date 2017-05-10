package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/*************************************************************************/
// This project has a lot of code for sending and receiving UDP packets
// UDPHelper is used to keep all of that code in one place.
/*************************************************************************/

public class UDPHelper {
	private DatagramSocket socket;
	private InetAddress IP;
	private int port;

	public UDPHelper() {
		this(false);
	}

	public UDPHelper(boolean timeout) {
		setUpSocket(timeout);
	}

	public UDPHelper(DatagramPacket p) {
		this();
		setReturn(p);
	}

	public UDPHelper(int socketPort) {
		this(socketPort, false);
	}

	public UDPHelper(int socketPort, boolean timeout) {
		setUpSocket(socketPort, timeout);
	}

	public UDPHelper(int socketPort, DatagramPacket p) {
		this(socketPort);
		setReturn(p);
	}

	public void setIP(InetAddress IP) {
		this.IP = IP;
	}

	public void setPort(int socketPort) {
		this.port = socketPort;
	}

	public void setReturn(DatagramPacket p) {
		port = p.getPort();
		IP = p.getAddress();
	}

	private void setUpSocket(boolean timeout) {
		try {
			socket = new DatagramSocket();
			if (timeout)
				socket.setSoTimeout(Var.TIMEOUT);
		} catch (SocketException e) {
			Log.err("ERROR Starting socket", e);
		}
	}

	private void setUpSocket(int port, boolean timeout) {
		try {
			socket = new DatagramSocket(port);
			if (timeout)
				socket.setSoTimeout(Var.TIMEOUT);
		} catch (SocketException e) {
			Log.err("ERROR Starting socket", e);
		}
	}

	public void sendPacket(byte[] data) {
		DatagramPacket sPacket = new DatagramPacket(data, data.length, IP, port);
		Log.packet("Sending Packet", sPacket);
		try {
			socket.send(sPacket);
		} catch (IOException e) {
			Log.err("ERROR Sending packet", e);
		}
	}

	public DatagramPacket receivePacket() {
		DatagramPacket rPacket = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		try {
			socket.receive(rPacket);
		} catch (SocketTimeoutException ste) {
			return null;
		} catch (IOException e) {
			Log.err("ERROR Receiving packet", e);
		}
		Log.packet("Packet Received", rPacket);
		return rPacket;
	}

	public void close() {
		socket.close();
	}
}
