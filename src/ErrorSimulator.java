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

	private ErrorScenario err;
	
	public ErrorSimulator() throws SocketException {
		socket = new DatagramSocket(Var.PORT_CLIENT);
		err    = null;
	}
	
	private void display(DatagramPacket packet){
		for(int i=0; i<packet.getLength(); i++){
			System.out.print(packet.getData()[i]+", ");
		}System.out.print("\n");
	}
	
	/*************************************************************************/
	// This method checks a packet for existing conditions comparing to the
	// error scenario. If the correct scenario is present, the sabotage 
	// method is called on the packet, else it is unchanged
	/*************************************************************************/
	
	private DatagramPacket checkSabotage(DatagramPacket packet){
		byte[] data = packet.getData();
		// Sabotage a Read packet
		if       ( data[1]==Var.READ[1] && err.getPacketType()==ErrorScenario.READ_PACKET){
			packet = err.Sabotage(packet);
			Log.out(
				"ErrorSimulatorChannel: READ Packet sabotaged with "
				+ErrorScenario.FAULT[err.getFaultType()-1]
				+" Fault (CODE: "+err.getErrorCode()+")"
			);
			return packet;
		}
		// Sabotage a Write packet
		if( data[1]==Var.WRITE[1]  && err.getPacketType()==ErrorScenario.WRITE_PACKET){
			packet = err.Sabotage(packet);
			Log.out(
				"ErrorSimulatorChannel: WRITE Packet sabotaged with "
				+ErrorScenario.FAULT[err.getFaultType()-1]
				+" Fault (CODE: "+err.getErrorCode()+")"
			);
			return packet;
		}
		// Sabotage a Data packet
		if( data[1] == Var.DATA[1] && err.getPacketType()==ErrorScenario.DATA_PACKET 
										  && (data[2]*256+data[3]) == err.getBlockNum()){
			packet = err.Sabotage(packet);
			Log.out(
				"ErrorSimulatorChannel: DATA Packet #"+err.getBlockNum()+" sabotaged with "
				+ErrorScenario.FAULT[err.getFaultType()-1]
				+" Fault (CODE: "+err.getErrorCode()+")"
			);
			return packet;
		} 
		// Sabotage an ACK packet
		if( data[1] == Var.ACK[1]  && err.getPacketType()==ErrorScenario.ACK_PACKET  
									 	  && (data[2]*256+data[3]) == err.getBlockNum()){
			packet = err.Sabotage(packet);
			Log.out(
				"ErrorSimulatorChannel: ACK Packet #"+err.getBlockNum()+" sabotaged with "
				+ErrorScenario.FAULT[err.getFaultType()-1]
				+" Fault (CODE: "+err.getErrorCode()+")"
			);
			return packet;
		}
		return packet;
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
		SocketAddress addrServer = SERVER_ADDRESS;//new InetSocketAddress("localhost", Var.PORT_SERVER);
		
		// Packet used to transfer between client and server
		DatagramPacket packet = null;
		
		// Data and Flags used in transfer
		byte[] data;
		boolean lastData  = false;
		boolean lastAck   = false;
		boolean error     = false;
		boolean initiated = false;
		
		
		// Begin loop: Get Response from Client and forward to server, vice-versa
		running = true;
		while (running) {
			// Receive packet from Client
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			if(!initiated) socket.receive(packet); // Receive on Well-Known port socket
			else        socClient.receive(packet); // Receive on Channel-Dedicated port socket
			
			initiated = true; // If here transfer has been initiated
			
			// Display packet that was received
			Log.packet("ErrorSimulatorChannel: Client -> ErrorSim", packet);
			display(packet);
			
			// Update address for Client, where future requests are sent
			addrClient = packet.getSocketAddress();

			// Parse Packet from Client
			data = packet.getData();
			//Check if last DATA packet
			if (data[0] == Var.DATA[0] && data[1] == Var.DATA[1] && packet.getLength() < 516) {
				Log.out("ErrorSimulatorChannel: Received Last DATA Packet in Transfer");
				lastData = true;
			//Check for last ACK packet
			} else if (data[0] == Var.ACK[0] && data[1] == Var.ACK[1] && lastData == true) {
				Log.out("ErrorSimulatorChannel: Received Last ACK Packet in Transfer");
				lastAck = true;
			}
			// Update Address
			packet.setSocketAddress(addrServer);
			// Check whether or not to sabotage
			packet = checkSabotage(packet);
			//Check for Error packet
			if (data[0] == Var.ERROR[0] && data[1] == Var.ERROR[1] && data[3] == 4) {
				error = true;
			}
			
			// Send socket to Server
			socServer.send(packet);
			Log.packet("ErrorSimulatorChannel: ErrorSim -> Server", packet);
			display(packet);

			// Quit if lastAck was sent
			if ((lastData && lastAck) || error) break;
			
			//-----------------------------------------
			
			// Receive back from Server
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			socServer.receive(packet);
			Log.packet("ErrorSimulatorChannel: Server -> ErrorSim", packet);
			display(packet);
			
			// Update address for server, where future requests are sent
			addrServer = packet.getSocketAddress();
			
			// Parse Packet from Server
			data = packet.getData();
			//Check if last DATA packet
			if (data[0] == Var.DATA[0] && data[1] == Var.DATA[1] && packet.getLength() < 516) {
				Log.out("ErrorSimulatorChannel: Received Last DATA Packet in Transfer");
				lastData = true;
			//Check for last ACK packet
			} else if (data[0] == Var.ACK[0] && data[1] == Var.ACK[1] && lastData == true) {
				Log.out("ErrorSimulatorChannel: Received Last ACK Packet in Transfer");
				lastAck = true;
			}
			// Update Address
			packet.setSocketAddress(addrClient);
			// Check whether or not to sabotage
			packet = checkSabotage(packet);
			//Check for Error packet
			if (data[0] == Var.ERROR[0] && data[1] == Var.ERROR[1] && data[3] == 4) {
				error = true;
			}
						
			// Send socket to Client
			socClient.send(packet);
			Log.packet("ErrorSimulatorChannel: ErrorSim -> Client", packet);
			display(packet);
			
			// Quit if lastAck was sent
			if ((lastData && lastAck) || error) break;
		}
		socClient.close();
		socServer.close();
		Log.out("Transfer complete. Closing Channel");
	}

	public void run() throws IOException {
		running = true;
		while (running) {
			err = new ErrorScenario();
			Log.out("Running new channel");
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
