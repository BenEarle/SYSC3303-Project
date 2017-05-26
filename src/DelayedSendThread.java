import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import util.Log;

public class DelayedSendThread extends Thread {
	
	private DatagramPacket packet;
	private DatagramSocket socket;
	private int delay;
	private String msg;
	
	/*************************************************************************/
	// This class sends a message on the specific server after a specified
	// delay
	/*************************************************************************/
	public DelayedSendThread(int delay, DatagramPacket packet, DatagramSocket socket, String msg){
		this.delay  = delay;
		this.socket = socket;
		this.msg    = msg;
		this.packet = packet;
	}
	
	public void run(){
		// Do nothing for delay
		Log.out("ErrorSimulatorChannel: Sending packet in "+delay+" ms");
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.packet("-------------------------------------\n"+msg, this.packet);
		try {
			socket.send(this.packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
