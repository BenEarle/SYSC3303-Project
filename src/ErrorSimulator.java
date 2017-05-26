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
	private static final SocketAddress SERVER_ADDRESS = new InetSocketAddress("localhost", Var.PORT_SERVER);
	private boolean running;

	// Sockets
	private DatagramSocket socClient; 
	private DatagramSocket socServer; 
	
	// Addresses
	private SocketAddress addrClient;
	private SocketAddress addrServer;
	
	// Packet Count
	private int expAckBlk;
	private int expDataBlk;
	private boolean lastAck;
	private boolean lastData;
	private boolean error;
	
	// Error Scenario
	private ErrorScenario err;
	
	// Flag to indicate of error case was triggered
	private boolean triggered;
	
	// Flag to indicate keep track where last packet was sent
	private boolean nextSendToClient = true;
	
	/*************************************************************************/
	// Constructor 
	/*************************************************************************/
	public ErrorSimulator() throws SocketException {
		socket = new DatagramSocket(Var.PORT_CLIENT);
		err    = null;
	}
	
	/*************************************************************************/
	// Lose a Packet
	/*************************************************************************/
	public DatagramPacket lose(DatagramPacket packet) throws IOException {
		// Do nothing with this data packet, just wait to receive another one from the same place
		if(nextSendToClient){
			// Wait for another data packet from the Server
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			socServer.receive(packet);
			Log.packet("ErrorSimulatorChannel: Receiving - Server -> ErrorSim", packet);
			packet.setSocketAddress(addrClient); // update the address
		} else {
			// Wait for another data packet from the Client
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			socClient.receive(packet);
			Log.packet("ErrorSimulatorChannel: Receiving - Client -> ErrorSim", packet);
			packet.setSocketAddress(addrServer); // update the address
		}	
		return packet;
	}
	
	/*************************************************************************/
	// Delay a Packet
	/*************************************************************************/
	public DatagramPacket delay(DatagramPacket packet) throws IOException {
		Thread delayThread;
		if(nextSendToClient){ 
			// Create a thread to send a message to Client after a delay
			delayThread = new DelayedSendThread(err.getDelay(), packet, socClient, "ErrorSimulatorChannel: Sending delayed Packet - ErrorSim -> Client");
			delayThread.start();
			// If delay is longer than timeout, expect another packet from the server to be sent in place
			if(err.getDelay() > Var.TIMEOUT){
				packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
				socServer.receive(packet);
				Log.packet("ErrorSimulatorChannel: Receiving - Server -> ErrorSim", packet);
				packet.setSocketAddress(addrClient);
			// Otherwise, just delay thread for duration of delay
			} else {
				try {Thread.sleep(err.getDelay());} 
				catch (InterruptedException e) { e.printStackTrace(); }
				packet = null; // don't send this packet, it will be sent by the thread
			}
		} else {
			// Create a thread to send a message to Server after a delay
			delayThread = new DelayedSendThread(err.getDelay(), packet, socServer, "ErrorSimulatorChannel: Sending delayed Packet - ErrorSim -> Server");
			delayThread.start();
			// If delay is longer than timeout, expect another packet from the client to be sent in place
			if(err.getDelay() > Var.TIMEOUT){
				packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
				socClient.receive(packet);
				Log.packet("ErrorSimulatorChannel: Receiving - Client -> ErrorSim", packet);
				//display(packet);
				packet.setSocketAddress(addrServer);
			// Otherwise, just delay thread for duration of delay
			} else {
				try {Thread.sleep(err.getDelay());} 
				catch (InterruptedException e) { e.printStackTrace(); }
				packet = null; // don't send this packet, it will be sent by the thread
			}
		}
		return packet;
	}
	
	/*************************************************************************/
	// Duplicate a Packet
	/*************************************************************************/
	public DatagramPacket duplicate(DatagramPacket packet) throws IOException {
		Thread delayThread;
		// Create a thread to send a message to client after a delay
		if(nextSendToClient){ 
			delayThread = new DelayedSendThread(err.getDelay(), packet, socClient, "ErrorSimulatorChannel: Sending delayed Packet - ErrorSim -> Client");
		// Create a thread to send a message to server after a delay
		} else {
			delayThread = new DelayedSendThread(err.getDelay(), packet, socServer, "ErrorSimulatorChannel: Sending delayed Packet - ErrorSim -> Server");
		}					
		delayThread.start();
		// Copy original packet
		DatagramPacket newPacket = new DatagramPacket( packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort());
		newPacket.setData(packet.getData(),0,packet.getLength());
		return newPacket; //send other packet normally
		
	}
	/*************************************************************************/
	// This method checks a packet for the existing conditions described in the
	// error scenario. If the correct scenario is present, the error scenario
	// is triggered 
	/*************************************************************************/
	private DatagramPacket checkTrigger(DatagramPacket packet) throws IOException{
		
		if(triggered) return packet; // only proceed if scenario not yet triggered
		
		byte[] data = packet.getData();
		
		// Trigger an error for a RRQ packet
		if( data[1]==Var.READ[1] && err.getPacketType()==ErrorScenario.READ_PACKET){
			triggered = true;
			Log.out("ErrorSimulatorChannel: READ Packet sabotaged with "+ErrorScenario.FAULT[err.getFaultType()]+" Fault");	
		// Trigger an error for a WRQ packet
		} else if( data[1]==Var.WRITE[1]  && err.getPacketType()==ErrorScenario.WRITE_PACKET){
			triggered = true;
			Log.out("ErrorSimulatorChannel: WRITE Packet sabotaged with "+ErrorScenario.FAULT[err.getFaultType()] +" Fault");
		// Trigger an error for a Specific DATA packet
		} else if( data[1]==Var.DATA[1] && err.getPacketType()==ErrorScenario.DATA_PACKET && getBlockNum(packet)==err.getBlockNum() ){
			triggered = true;
			Log.out("ErrorSimulatorChannel: DATA Packet #"+err.getBlockNum()+" sabotaged with "+ErrorScenario.FAULT[err.getFaultType()] +" Fault");
		// Trigger an error for a Specific ACK packet
		} else if( data[1]==Var.ACK[1] && err.getPacketType()==ErrorScenario.ACK_PACKET && getBlockNum(packet)==err.getBlockNum() ){
			triggered = true;
			Log.out("ErrorSimulatorChannel: ACK Packet #" +err.getBlockNum()+" sabotaged with "+ErrorScenario.FAULT[err.getFaultType()] +" Fault");
		// Trigger an error for an ERROR packet
		} else if( data[1] == Var.ERROR[1]  && err.getPacketType()==ErrorScenario.ERR_PACKET){
			triggered = true;
			Log.out("ErrorSimulatorChannel: ERR Packet #"+err.getBlockNum()+" sabotaged with "+ErrorScenario.FAULT[err.getFaultType()] +" Fault");
		}
		// Run error case if triggered
		if(triggered){
			if     ( err.getErrorCode()==1 ) packet = lose(packet);
			else if( err.getErrorCode()==2 ) packet = delay(packet); 
			else if( err.getErrorCode()==3 ) packet = duplicate(packet);	
			else                             packet = err.Sabotage(packet);	
		}
		return packet;
	}
	
	/*************************************************************************/
	// This method checks the received packet and sets program behavior 
	// accordingly. Return true to proceed, false to keep receiving
	/*************************************************************************/
	public boolean parsePacket(DatagramPacket packet) throws IOException{
		byte[] data = packet.getData();
		// RRQ
		if(data[0]==Var.READ[0] && data[1]==Var.READ[1]){
			if(expDataBlk==0 && expDataBlk==0){ // ensure no transfer in progess
				expDataBlk = 1;
				expAckBlk  = 1;
				return true;
			}
		// WRQ
		} else if(data[0]==Var.WRITE[0] && data[1]==Var.WRITE[1]){
			if(expDataBlk==0 && expDataBlk==0){	// ensure no transfer in progess
				expDataBlk = 1;
				expAckBlk  = 0;
				return true;
			}
		// DATA
		} else if(data[0]==Var.DATA[0] && data[1]==Var.DATA[1]){
			// packet is expected
			if(expDataBlk == getBlockNum(packet)){
				expDataBlk++;
				if (packet.getLength() < 516) {
					Log.out("ErrorSimulatorChannel: Received Last DATA Packet in Transfer");
					lastData = true;
				}
				return true;
			// Unexpected packet. Forward but do not change modes
			} else {
				Log.out("ErrorSimulatorChannel: Forwarding Unexpected Block");
				if( nextSendToClient ){ // forward to server
					packet.setSocketAddress(addrServer);
					socServer.send(packet);
					Log.packet("ErrorSimulatorChannel: Sending - ErrorSim -> Server", packet); //display(packet);
				} else { // forward to client
					packet.setSocketAddress(addrClient);
					socClient.send(packet);
					Log.packet("ErrorSimulatorChannel: Sending - ErrorSim -> Client", packet); //display(packet);
				}
				return false;
			}
		// ACK
		} else if(data[0]==Var.ACK[0] && data[1] == Var.ACK[1]){
			// packet is expected
			if(expAckBlk == getBlockNum(packet)){
				expAckBlk++;
				if (lastData == true) {
					Log.out("ErrorSimulatorChannel: Received Last ACK Packet in Transfer");
					lastAck = true;
				}
				return true;
			// Unexpected packet. Forward but do not change modes
			} else {
				Log.out("ErrorSimulatorChannel: Forwarding Unexpected Block");
				if( nextSendToClient ){ // forward to server
					packet.setSocketAddress(addrServer);
					socServer.send(packet);
					Log.packet("ErrorSimulatorChannel: Sending - ErrorSim -> Server", packet); //display(packet);
				} else { // forward to client
					packet.setSocketAddress(addrClient);
					socClient.send(packet);
					Log.packet("ErrorSimulatorChannel: Sending - ErrorSim -> Client", packet); //display(packet);
				}
				return false;
			}
		// ERROR
		}else if(data[0]==Var.ERROR[0] && data[1]==Var.ERROR[1]){
			if (data[3] != 5) error = true; // don't quit on code 5
			return true;
		} 
		Log.out("ErrorSimulatorChannel: Ignoring Unexpected Block");
		return false;
	}
	
	
	/*************************************************************************/
	// This method creates and runs a temporary channel between server and 
	// client. This method is called each time the client makes a new request
	// from the server.
	/*************************************************************************/
	
	public void runChannel() throws IOException {
		// Set up sockets for communicating with client and server
		socClient = new DatagramSocket(); 
		socServer = new DatagramSocket(); 

		// Set up variables to remember addresses of client and server
		addrClient = null;
		addrServer = SERVER_ADDRESS;//new InetSocketAddress("localhost", Var.PORT_SERVER);
		expAckBlk  = 0;
		expDataBlk = 0;
		lastData   = false;
		lastAck    = false;
		error      = false;
		triggered  = false;
		
		DatagramPacket packet = null;
		boolean initiated = false;
		
		// Begin loop: Get Response from Client and forward to server, vice-versa
		running = true;
		while (running) {
			//-----------------------------------------
			// Receive packet from Client
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			if(!initiated){
				do{
					socket.receive(packet); // Receive on Well-Known port socket
					Log.packet("ErrorSimulatorChannel: Receiving - Client -> ErrorSim", packet); //display(packet);
				} while (parsePacket(packet) == false);
				addrClient = packet.getSocketAddress();
			} else {
				// Keep receiving until expected packet is received
				do{
					socClient.receive(packet); // Receive on Channel-Dedicated port socket
					Log.packet("ErrorSimulatorChannel: Receiving - Client -> ErrorSim", packet); //display(packet);
				} while (parsePacket(packet) == false);
			}
		
			packet.setSocketAddress(addrServer); // Set packet to go to Server
			nextSendToClient = false; // Should expect next packet from Server
			
			// Check if error case is triggered
			packet = checkTrigger(packet);
			
			// Send socket to Server if not nullified by checkTrigger
			if(packet!=null){
				socServer.send(packet);
				Log.packet("ErrorSimulatorChannel: Sending - ErrorSim -> Server", packet); //display(packet);
			}
			
			// Quit if lastAck was sent or error case
			if ((lastData && lastAck) || error) break;
			
			//-----------------------------------------
			// Receive packet from Server
			// Keep receiving until expected packet is received
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			do{
				socServer.receive(packet);
				Log.packet("ErrorSimulatorChannel: Receiving - Server -> ErrorSim", packet); //display(packet);
			} while (parsePacket(packet) == false);
			if(!initiated){
				addrServer = packet.getSocketAddress();
				initiated = true;
			} 
			
			packet.setSocketAddress(addrClient); // Set packet to go to Client
			nextSendToClient = true;  // Should expect next packet from Client
					
			// Check if error case is triggered
			packet = checkTrigger(packet);
				
			// Send socket to Client if not nullified by checkTrigger
			if(packet!=null){ 
				socClient.send(packet);
				Log.packet("ErrorSimulatorChannel: Sending - ErrorSim -> Client", packet); //display(packet);
			}
			
			// Quit if lastAck was sent or error case
			if ((lastData && lastAck) || error) break;
		}

		//Close sockets
		socClient.close();
		socServer.close();
		Log.out("Transfer complete. Closing Channel");
	}
	public void run() throws IOException {
		running = true;
		while (running) {
			err = new ErrorScenario(); //create a new scenario after every transfer
			Log.out("Running a new channel between the Client and Server");
			runChannel();
		}
	}
	public int getBlockNum(DatagramPacket packet){
		byte[] data = packet.getData();
		return data[2]*256+data[3];
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
