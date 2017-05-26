import java.net.DatagramPacket;
import util.Log;
import util.TFTPErrorHelper;
import util.UDPHelper;
import util.Var;

/*************************************************************************/
// This thread is started as soon as the server is ran. It will run
// constantly waiting for a request on port 69. When it receives a packet
// it will open a new thread to service the request then continue listing
// for more.
/*************************************************************************/

public class ControlThread extends Thread {
	private UDPHelper udp;
	private boolean running;

	public ControlThread() {
		udp = new UDPHelper(69, true);
	}
	
	public void run() {
		running = true;
		Log.out("SERVER<ControlThread>: Waiting to receive a packet...");
		while (running) {
			// Wait to get a packet.
			DatagramPacket packet = udp.receivePacket();
			if (packet != null){
				if (TFTPErrorHelper.requestPacketChecker(udp, packet) == null){				
					//Log.packet("SERVER<ControlThread>: Server Receive", packet);
					int res = readPacket(packet);
					switch (res) {
					case 1:
						// Start a new ReadThread to handle the request.
						new ReadThread(packet).start();
						System.out.println("\nSERVER<ControlThread>: Server received a READ request. ");
						//Log.packet("Request: ", packet);
						break;
					case 2:
						// Start a new WriteThread to handle the request.
						new WriteThread(packet).start();
						System.out.println("\nSERVER<ControlThread>: Server received a WRITE request");
						//Log.packet("Request: ", packet);
						break;
					default:
						Log.err("SERVER<ControlThread>: Server got invalid packet, closing.");
						close();
					}
					Log.out("SERVER<ControlThread>: Waiting to receive a packet...");
				} else {
					//Do we care about the code???
					if (packet.getData()[1] == 5)
						TFTPErrorHelper.unPackError(packet);
				} 
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
		if (running) {
			Log.out("SERVER<ControlThread>: Closed control thread.");
			running = false;
			udp.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

}
