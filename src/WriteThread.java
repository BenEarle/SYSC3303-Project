import java.net.DatagramPacket;

public class WriteThread extends ClientResponseThread {

	public WriteThread(DatagramPacket initialPacket) {
		super(initialPacket);
	}
	
	public void run(){
		//send ACK packet 0400
		//
	}
	
}
