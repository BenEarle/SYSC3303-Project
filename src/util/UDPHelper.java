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
	private DatagramPacket sentPacket;
	private DatagramPacket recPacket;
	private boolean testSender = false;
	private boolean resendOnTimeout = false;

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

	public void setResendOnTimeout(Boolean b){
		resendOnTimeout = b;
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
		sentPacket = new DatagramPacket(data.clone(), data.length, IP, port);
		Log.packet("Sending Packet", sentPacket);
		try {
			socket.send(sentPacket);
		} catch (IOException e) {
			Log.err("ERROR Sending packet", e);
		}
	}

	public DatagramPacket receivePacket() {
		recPacket = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		int packetsSent = 0; 
		while(packetsSent < Var.NUMBER_OF_RETRY){
			try {
				packetsSent++;
				socket.receive(recPacket);
				Log.packet("Packet Received", recPacket);
				
				if(testSender && !(recPacket.getAddress().equals(IP) && recPacket.getPort() == port)) {
					//Send error code 5 and continue
					//Change sending info for the send error handler to use
					int correctPort = port;
					InetAddress correctIP = IP;
					port = recPacket.getPort();
					IP = recPacket.getAddress();
					TFTPErrorHelper.sendError(this, (byte) 5, "Invalid sender. Was expecting a response from: " + IP.toString() + ":" + port);
					//Correct state of ip and port
					port = correctPort;
					IP = correctIP;
					return receivePacket();
				} else if (!testSender) {
					port = recPacket.getPort();
				}
				
				return recPacket;
			} catch (SocketTimeoutException ste) {
				if(sentPacket != null && resendOnTimeout)
					Log.out("Socket receive timed out.");
					this.resendLastPacket();
			} catch (SocketException e) {
				// If the socket should be closed this is fine.
				if (!closed || !e.getMessage().equals("socket closed")) {
					Log.err("ERROR Receiving packet", e);
				}
			} catch (IOException e) {
				Log.err("ERROR Receiving packet", e);
			}
		}
		return null;
	}

	public void setTestSender(boolean b){
		testSender = b;
	}
	
	public DatagramPacket getRecPacket() {
		return recPacket;
	}
	
	public void close() {
		if (!closed) {
			closed = true;
			socket.close();
		}
	}

	public void resendLastPacket() {
		Log.packet("Resending Last Packet", sentPacket);
		try {
			socket.send(sentPacket);
		} catch (IOException e) {
			Log.err("ERROR Sending packet", e);
		}
	}

}
