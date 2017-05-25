import java.net.DatagramPacket;
import util.UDPHelper;
import util.Var;

/*************************************************************************/
//ClientResponseThread is abstract as it doesn't do anything on it's own. 
//Read/WriteThread extend this class and both implement run(). 
//This class handle the variables and common methods for threads that 
//handle responses on the server.
/*************************************************************************/

public abstract class ClientResponseThread extends Thread {
	protected UDPHelper udp;
	protected String file, mode;

	ClientResponseThread(DatagramPacket initialPacket) {
		udp = new UDPHelper(true);
		udp.setReturn(initialPacket);
		udp.setTestSender(true);
		unpack(initialPacket);
	}

	/*************************************************************************/
	// ClientResponseThread is abstract as it doesn't do anything on it's own.
	// Read/WriteThread extend this class and both implement run().
	// This class handle the variables and common methods for threads that
	// handle responses on the server.
	/*************************************************************************/

	protected void unpack(DatagramPacket p) {
		byte[] data = p.getData();
		int i = 2;
		file = "";
		// Loop through reading the file name
		while (i < data.length) {
			if (data[i] == 0)
				break;
			file += (char) data[i];
			i++;
		}
		// Increment i again to skip over the null char
		i++;
		// Read the mode from the packet.
		mode = "";
		while (i < data.length) {
			if (data[i] == 0)
				break;
			mode += (char) data[i];
			i++;
		}
	}

	/*************************************************************************/
	// The following methods reduce repeated code by handling the networking
	// for the server components.
	/*************************************************************************/

	protected void sendPacket(byte[] data) {
		udp.sendPacket(data);
	}

	protected DatagramPacket receivePacket() {
		return udp.receivePacket();
	}

	protected void close() {
		udp.close();
	}

}
