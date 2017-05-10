package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPHelper {
	private DatagramSocket socket;
	private InetAddress IP;
	private int port;

	public UDPHelper() {
		setUpSocket();
	}

	public UDPHelper(int port) {
		setUpSocket(port);
	}

	public void setIP(InetAddress IP) {
		this.IP = IP;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setReturn(DatagramPacket p) {
		port = p.getPort();
		IP = p.getAddress();
	}

	private void setUpSocket() {
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			Log.err("ERROR Starting socket", e);
		}
	}

	private void setUpSocket(int port) {
		try {
			socket = new DatagramSocket(port);
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
		} catch (IOException e) {
			Log.err("ERROR Receiving packet", e);
		}
		Log.packet("Packet Received", rPacket);
		return rPacket;
	}
}
