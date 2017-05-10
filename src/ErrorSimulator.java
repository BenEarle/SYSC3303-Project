import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import util.Log;
import util.Var;
import util.ErrorScenario;


/*************************************************************************/
// This class will be used to simulate errors in future iterations of the
// project. Currently all it does is act as a proxy from the client to 
// the server. It will listen on port 22 for new requests from the client,
// when it receives one it will open a new socket to receive and forward
// messages between the client and server.
/*************************************************************************/

public class ErrorSimulator {
	private DatagramSocket socket;
	private static final SocketAddress SERVER_ADDRESS = new InetSocketAddress("localhost", Var.PORT_SERVER);;
	private boolean running;

	private ErrorScenario errorScenario;
	
	public ErrorSimulator() throws SocketException {
		socket = new DatagramSocket(Var.PORT_CLIENT);
		errorScenario = new ErrorScenario();
	}
	
	
	private void display(DatagramPacket packet){
		for(int i=0; i<packet.getData().length; i++){
			System.out.print(packet.getData()[i]+", ");
		}System.out.print("\n");
	}
	
	/*************************************************************************/
	// This method creates and runs a temporary channel between server and 
	// client. This method is called each time the client makes a new request
	// from the server.
	/*************************************************************************/
	
	public void runChannel() throws IOException {
		// Set up sockets for communicating with client and server
		DatagramSocket socClient = new DatagramSocket(); 
		DatagramSocket socServer = new DatagramSocket(); 

		// Set up variables to remember addresses of client and server
		SocketAddress addrClient = null;
		SocketAddress addrServer = new InetSocketAddress("localhost", Var.PORT_SERVER);; // after first response from server

		// Packet used to transfer between client and server
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		
		// data used
		byte[] data;
		boolean lastData = false;
		boolean lastAck  = false;
		boolean started  = false;
		
		
		// Begin loop: forward message to client, get response and forward to server ...
		running = true;
		while (running) {

			// Receive packet from Client
			if(!started) socket.receive(packet);
			else      socClient.receive(packet);
			
			started = true;
			
			Log.packet("ErrorSimulatorChannel: Client -> ErrorSim", packet);
			display(packet);
			
			// Update address for Client, where future requests are sent
			addrClient = packet.getSocketAddress();

			//**************************************
			// Parse Packet from Client
			data = packet.getData();
			//Check for last DATA packet
			if (data[0] == Var.DATA[0] && data[1] == Var.DATA[1] && packet.getLength() != 516) {
				Log.out("Received Last Packet");
				lastData = true;
			//Check for last ACK packet
			} else if (data[0] == Var.ACK[0] && data[1] == Var.ACK[1] && lastData == true) {
				lastAck = true;
			}
			// Check whether or not to sabotage
			if( data[1] == Var.READ[1] && errorScenario.packetType == errorScenario.READ_PACKET){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: READ Packet sabotaged");
			}
			if( data[1] == Var.WRITE[1] && errorScenario.packetType == errorScenario.WRITE_PACKET){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: WRITE Packet sabotaged");
			}
			if( data[1] == Var.DATA[1] && errorScenario.packetType == errorScenario.DATA_PACKET && (data[2]*256+data[3]) == errorScenario.blockNum){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: DATA Packet sabotaged");
			}
			if( data[1] == Var.ACK[1] && errorScenario.packetType == errorScenario.ACK_PACKET && (data[2]*256+data[3]) == errorScenario.blockNum){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: ACK Packet sabotaged");
			}
			//**************************************
			
			// Send socket to Server
			packet.setSocketAddress(addrServer);
			socServer.send(packet);
			Log.packet("ErrorSimulatorChannel: ErrorSim -> Server", packet);
			display(packet);

			// Break if lastAck was sent
			if (lastData && lastAck)
				break;

			// Receive back from Server
			socServer.receive(packet);
			Log.packet("ErrorSimulatorChannel: Server -> ErrorSim", packet);
			display(packet);
			
			// Update address for server, where future requests are sent
			addrServer = packet.getSocketAddress();
			
			//**************************************
			// Parse Packet from Server
			data = packet.getData();
			//Check for last DATA packet
			if (data[0] == Var.DATA[0] && data[1] == Var.DATA[1] && packet.getLength() != 516) {
				Log.out("Received Last Packet");
				lastData = true;
			//Check for last ACK packet
			} else if (data[0] == Var.ACK[0] && data[1] == Var.ACK[1] && lastData == true) {
				lastAck = true;
			}
			// Check whether or not to sabotage
			if( data[1] == Var.READ[1] && errorScenario.errorCode == errorScenario.READ_PACKET){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: READ Packet sabotaged");
			}
			if( data[1] == Var.WRITE[1] && errorScenario.errorCode == errorScenario.WRITE_PACKET){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: WRITE Packet sabotaged");
			}
			if( data[1] == Var.DATA[1] && errorScenario.errorCode == errorScenario.DATA_PACKET && (data[2]*256+data[3]) == errorScenario.blockNum){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: DATA Packet sabotaged");
			}
			if( data[1] == Var.ACK[1] && errorScenario.errorCode == errorScenario.ACK_PACKET && (data[2]*256+data[3]) == errorScenario.blockNum){
				packet = errorScenario.Sabotage(packet);
				Log.out("ErrorSimulator: ACK Packet sabotaged");
			}
			//**************************************
			
			// Send socket to Client
			packet.setSocketAddress(addrClient);
			socClient.send(packet);
			Log.packet("ErrorSimulatorChannel: ErrorSim -> Client", packet);
			display(packet);
			

			// Break if lastAck was sent
			if (lastData && lastAck)
				break;
		}
		socClient.close();
		socServer.close();
		Log.out("Transfer complete. Closing Channel");
	}

	public void run() throws IOException {

		running = true;

		while (running) {

			// Create and run a channel for received request
			runChannel();
			
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


/*
// Receive request from client
socket.receive(requestPacket);
Log.packet("ErrorSimulator: Client -> ErrorSim", requestPacket);
for(int i=0; i<requestPacket.getData().length; i++){
	System.out.print(requestPacket.getData()[i]+", ");
}System.out.print("\n");

//**************************************
// Parse Packet from Client
data = requestPacket.getData();
// Check block number and type to determine wether or not to sabotage
if( data[1] == errorScenario.errorCode ) {
	requestPacket = errorScenario.Sabotage(requestPacket);
	Log.out("ErrorSimulator: READ/WRITE Request Packet Sabotaged");
}
//**************************************


// First packet goes to Server's well-known port
requestPacket.setSocketAddress(SERVER_ADDRESS);
socServer.send(requestPacket);
Log.packet("ErrorSimulatorChannel: Server -> ErrorSim", requestPacket);
for(int i=0; i<requestPacket.getData().length; i++){
	System.out.print(requestPacket.getData()[i]+", ");
}System.out.print("\n");


// Receive back from server
DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
socServer.receive(packet);
Log.packet("ErrorSimulatorChannel: Server -> ErrorSim", packet);
for(int i=0; i<packet.getData().length; i++){
	System.out.print(packet.getData()[i]+", ");
}System.out.print("\n");

// Update address for server, where future requests are sent
addrServer = packet.getSocketAddress();

//**************************************
// Parse Packet from Server
data = packet.getData();
// Check for last data packet
if (data[0] == Var.DATA[0] && data[1] == Var.DATA[1] && packet.getLength() != 516) {
	Log.out("Received Last Packet");
	lastData = true;
// Check for last ack packet
} else if (data[0] == Var.ACK[0] && data[1] == Var.ACK[1] && lastData == true) {
	lastAck = true;
}
// Check block number and type to determine wether or not to sabotage
if( data[1] == errorScenario.errorCode && (data[2]*256+data[3]) == errorScenario.blockNum ) {
	packet = errorScenario.Sabotage(packet);
	Log.out("ErrorSimulator: READ/WRITE Packet sabotaged");
}
//**************************************

// Send first packet back to client
packet.setSocketAddress(addrClient);
socClient.send(packet);
Log.packet("ErrorSimulatorChannel: ErrorSim -> Client", packet);
for(int i=0; i<packet.getData().length; i++){
	System.out.print(packet.getData()[i]+", ");
}System.out.print("\n");
 */
