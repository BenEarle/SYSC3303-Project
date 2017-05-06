import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import util.Log;
import util.Var;

/*************************************************************************/
// This thread is started as soon as the server is ran. It will run
// constantly waiting for a request on port 69. When it receives a packet
// it will open a new thread to service the request then continue listing
// for more.
/*************************************************************************/

public class ControlThread extends Thread {
	private DatagramSocket socRecv;
	private boolean running;

	public ControlThread() {
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
			Log.out("SERVER<ControlThread>: Waiting to receive a packet...");
			// Wait to get a packet.
			DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			try {
				socRecv.receive(packet);
			} catch (IOException e) {
				Log.err("SERVER<ControlThread>: ERROR: " + e.toString());
				Log.err(e.getStackTrace().toString());
			}
			Log.packet("SERVER<ControlThread>: Server Receive", packet);
			int res = readPacket(packet);
			switch (res) {
			case 1:
				// Start a new ReadThread to handle the request.
				new ReadThread(packet).start();
				Log.packet("SERVER<ControlThread>: Server received a READ request. ", packet);
				break;
			case 2:
				// Start a new WriteThread to handle the request.
				new WriteThread(packet).start();
				Log.packet("SERVER<ControlThread>: Server received a WRITE request", packet);
				break;
			default:
				Log.out("SERVER<ControlThread>: Server got invalid packet, closing.");
				close();
				throw new IllegalArgumentException();
			}
		}
	}

	
	/*************************************************************************/
	// This method is used to decode the type of packet. 
	// Meaning of return value: 
	// 0 - Error 
	// 1 - Read request
	// 2 - Write request
	/*************************************************************************/

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
		Log.out("SERVER<ControlThread>: Closed control thread.");
		if (running) {
			running = false;
			socRecv.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

}
