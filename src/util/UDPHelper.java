package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/*************************************************************************/
// This project has a lot of code for sending and receiving UDP packets
// UDPHelper is used to keep all of that code in one place.
/*************************************************************************/

public class UDPHelper {
	private DatagramSocket socket;
	private InetAddress IP;
	private int port;
	private boolean closed = true;
	private DatagramPacket p;
	private boolean testSender = false;

	public UDPHelper() {
		this(false);
	}

	public UDPHelper(boolean timeout) {
		setUpSocket(timeout);
		try {
			setIP(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		try {
			setIP(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			closed = false;
		} catch (SocketException e) {
			Log.err("ERROR Starting socket", e);
		}
	}

	private void setUpSocket(int port, boolean timeout) {
		try {
			socket = new DatagramSocket(port);
			if (timeout)
				socket.setSoTimeout(Var.TIMEOUT);
			closed = false;
		} catch (SocketException e) {
			Log.err("ERROR Starting socket", e);
		}
	}

	public void sendPacket(byte[] data) {
		p = new DatagramPacket(data, data.length, IP, port);
		Log.packet("Sending Packet", p);
		try {
			socket.send(p);
		} catch (IOException e) {
			Log.err("ERROR Sending packet", e);
		}
	}

	public DatagramPacket receivePacket() {
		p = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);

		try {
			socket.receive(p);
			Log.packet("Packet Received", p);
			
			if(testSender && !(p.getAddress().equals(IP) && p.getPort() == port)){
				//Send error code 5 and continue
				TFTPErrorHelper.sendError(this, (byte) 5, "Invalid sender. Was expecting a response from: " + IP.toString() + ":" + port);
				return null;
			}
			
			return p;
		} catch (SocketTimeoutException ste) {
			// Nothing here.
		} catch (SocketException e) {
			// If the socket should be closed this is fine.
			if (!closed || !e.getMessage().equals("socket closed")) {
				Log.err("ERROR Receiving packet", e);
			}
		} catch (IOException e) {
			Log.err("ERROR Receiving packet", e);
		}

		return null;
	}

	public void setTestSender(boolean b){
		testSender = b;
	}
	
	public DatagramPacket getLastPacket() {
		return p;
	}
	
	public void close() {
		if (!closed) {
			closed = true;
			socket.close();
		}
	}

}
