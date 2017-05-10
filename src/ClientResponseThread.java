import java.net.DatagramPacket;
import util.UDPHelper;

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
		udp = new UDPHelper();
		udp.setReturn(initialPacket);
	}

	/*************************************************************************/
	// ClientResponseThread is abstract as it doesn't do anything on it's own. 
	// Read/WriteThread extend this class and both implement run(). 
	// This class handle the variables and common methods for threads that 
	// handle responses on the server.
	/*************************************************************************/
	
	protected void unpack(DatagramPacket p) {
		byte[] data = p.getData();
		// Parse packet confirming the format is correct
		if (data[0] != 0) // First bit must be 0
			throw new IllegalArgumentException();
		if (data[1] != 1 && data[1] != 2) // Second bit must either be a 1 or 2
			throw new IllegalArgumentException();
		int i = 2;
		file = "";
		// Loop through reading the file name
		while (i < data.length) {
			if (data[i] == 0)
				break;
			file += (char) data[i];
			i++;
		}
		// If we didn't find a 0 it means that the string is wrong
		if (i == data.length) 
			throw new IllegalArgumentException();
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
		if (mode.length() < 1 || file.length() < 1)
			throw new IllegalArgumentException();
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
