import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import util.Log;

public class DelayedSendThread extends Thread {
	
	private DatagramPacket packet;
	private DatagramSocket socket;
	private int delay;
	private String msg;
	
	public DelayedSendThread(int delay, DatagramPacket packet, DatagramSocket socket, String msg){
		this.delay  = delay;
		this.socket = socket;
		this.packet = packet;
		this.msg    = msg;
	}
	
	public void run(){
		// Do nothing for delay
		System.out.println("Waiting");
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Done");
		
		Log.packet("LOOK AT ME!!!\n\n\n\n\n"+msg, packet);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
