import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import util.Log;
import util.Var;

public class ErrorSimulator {
	
	private DatagramSocket socket;
	
	private static final SocketAddress SERVER_ADDRESS = new InetSocketAddress("localhost", Var.PORT_SERVER);;
	
	private boolean running;

	public ErrorSimulator() throws SocketException {
		socket = new DatagramSocket(Var.PORT_CLIENT);
	}
	
	// Create and run a temporary channel between server and client
	public void runChannel(DatagramPacket requestPacket) throws IOException{
		// Set up sockets for communicating with client and server
		DatagramSocket socClient = new DatagramSocket(); // For Communication with Client
		DatagramSocket socServer = new DatagramSocket(); // For Communication with Server
		
		// Set up variables to remember addresses of client and server
		SocketAddress addrClient = requestPacket.getSocketAddress();
		SocketAddress addrServer = null; // after first response from server
		
		// data used
		byte[] data;
		boolean lastData = false;
		boolean lastAck = false;
		
		// First packet goes to Server's well-known port
		requestPacket.setSocketAddress(SERVER_ADDRESS);
		socServer.send(requestPacket);
		
		// Receive back from server
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		socServer.receive(packet);
		Log.packet("ErrorSimulatorChannel: Server -> Client", packet);
		
		// Update address for server, where future requests are sent
		addrServer = packet.getSocketAddress();
		
		// Parse Packet from Server
		data = packet.getData();
		// Data packet
		if(data[0]==Var.DATA[0] && data[1]==Var.DATA[1] && packet.getLength() != 516){
			Log.out("Received Last Packet");
			lastData = true; 
		// ack packet
		} else if (data[0]==Var.ACK[0] && data[1]==Var.ACK[1] && lastData == true){
			lastAck = true;
		}

		// Send first packet back to client
		packet.setSocketAddress(addrClient);
		socClient.send(packet);
		
		// Begin loop: forward message to client, get response and forward to server ...
		running = true;
		while (running) {
			
			// Receive packet from Client
			socClient.receive(packet);
			Log.packet("ErrorSimulatorChannel: Client -> Server", packet);
			
			// Parse Packet from Client
			data = packet.getData();
			// Data packet
			if(data[0]==Var.DATA[0] && data[1]==Var.DATA[1] && packet.getLength() != 516){
				Log.out("Received Last Packet");
				lastData = true; 
			// ack packet
			} else if (data[0]==Var.ACK[0] && data[1]==Var.ACK[1] && lastData == true){
				lastAck = true;
			}
				
			// Send socket to Server
			packet.setSocketAddress(addrServer);
			socServer.send(packet);
			
			// Break if lastAck was sent
			if(lastData && lastAck) break;
			
			// Receive back from Server
			socServer.receive(packet);
			Log.packet("ErrorSimulatorChannel: Server -> Client", packet);
			
			// Parse Packet from Server
			data = packet.getData();
			// Data packet
			if(data[0]==Var.DATA[0] && data[1]==Var.DATA[1] && packet.getLength() != 516){
				Log.out("Received Last Packet");
				lastData = true; 
			// ack packet
			} else if (data[0]==Var.ACK[0] && data[1]==Var.ACK[1] && lastData == true){
				lastAck = true;
			}

			// Send socket to Client
			packet.setSocketAddress(addrClient);
			socClient.send(packet);
			
			// Break if lastAck was sent
			if(lastData && lastAck) break;
		}
		socClient.close();
		socServer.close();
		Log.out("Transfer complete. Closing Channel");
	}
	
	
	public void run() throws IOException {

		running = true;
	
		while (running) {
			// New packet to receive requests from client
			DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			
			// Receive from client
			socket.receive(packet);
			Log.packet("ErrorSimulator: Client -> Server", packet);
	
			// Create and run a channel for received request
			runChannel(packet);
		}
	}

	public void close() {
		if (running) {
			running = false;
			socket.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

	public static void main(String[] args) throws SocketException, IOException {
		new ErrorSimulator().run();
	}

}
