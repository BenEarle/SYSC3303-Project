import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import util.Log;
import util.Var;

public class ControlThread extends Thread {
	private DatagramSocket socRecv;
	private boolean running;
	private boolean verbose;
	
	public ControlThread(boolean verbose) {
		this.verbose = verbose;
		try {
			socRecv = new DatagramSocket(Var.PORT_SERVER);
		} catch (SocketException e) {
			Log.err("ERROR: The server was unable to bind to port 69.");
			Log.err(e.getStackTrace().toString());
		}
	}

	public void run() {
		running = true;

		while (running) {
			if(verbose) System.out.println("SERVER<ControlThread>: Waiting to receive a packet...");
			// Wait to get a packet.
			DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			try {
				socRecv.receive(packet);
			} catch (IOException e) {
				Log.err("ERROR: " + e.toString());
				Log.err(e.getStackTrace().toString());
			}
			Log.packet("Server Receive", packet);
			int res = readPacket(packet);
			switch (res) {
			case 1:
				//READ
				Log.packet("Server Sending READ", packet);
				break;
			case 2:
				//Start a new WriteThread to handle the request.
				new WriteThread(packet).start();
				Log.packet("Server Sending WRITE", packet);
				break;
			default:
				Log.out("Server got invalid packet, closing.");
				close();
				throw new IllegalArgumentException();
			}
		}
	}

	public int readPacket(DatagramPacket packet) {
		byte[] buf = packet.getData();
		int type = 0;
		
		// Check the READ/WRITE code is correct.
		if (buf[0] == Var.READ[0] && buf[1] == Var.READ[1]) {
			type = 1;
		} else if (buf[0] == Var.WRITE[0] && buf[1] == Var.WRITE[1]) {
			type = 2;
		} else {
			return 0;
		}
		return type;
	}
	
	public void close() {
		if(verbose) System.out.println("SERVER<ControlThread>: closed control thread.");
		if (running) {
			running = false;
			socRecv.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

}



