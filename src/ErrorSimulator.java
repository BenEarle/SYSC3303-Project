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
	
	// Main ErrorSim attributes
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
	private boolean initiated;
	private boolean rollOver;
	
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
	private DatagramPacket lose(DatagramPacket packet) throws IOException {
		// Do nothing with this data packet, just wait to receive another one from the same place
		if(nextSendToClient){
			Log.out("ErrorSimulatorChannel: Waiting for another packet from Server");
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			socServer.receive(packet);
			Log.packet("ErrorSimulatorChannel: Receiving - Server -> ErrorSim", packet);
			packet.setSocketAddress(addrClient); // update the address
		} else {
			Log.out("ErrorSimulatorChannel: Waiting for another packet from Client");
			packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			if(!initiated) socket.receive(packet); // Receive on Well-Known port socket
			else socClient.receive(packet);
			Log.packet("ErrorSimulatorChannel: Receiving - Client -> ErrorSim", packet);
			packet.setSocketAddress(addrServer); // update the address
		}	
		return packet;
	}
	
	/*************************************************************************/
	// Delay a Packet
	/*************************************************************************/
	private DatagramPacket delay(DatagramPacket packet) throws IOException {
		Thread delayThread;
		if(nextSendToClient){ 
			// Create a thread to send a message to Client after a delay
			delayThread = new DelayedSendThread(err.getDelay(), packet, socClient, "ErrorSimulatorChannel: Sending delayed Packet - ErrorSim -> Client");
			delayThread.start();
			// If delay is longer than timeout, expect another packet from the server to be sent in place
			if(err.getDelay() > Var.TIMEOUT){
				Log.out("ErrorSimulatorChannel: Waiting for another packet from Server");
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
				Log.out("ErrorSimulatorChannel: Waiting for another packet from Client");
				packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
				if(!initiated) socket.receive(packet); // Receive on Well-Known port socket
				else socClient.receive(packet);
				Log.packet("ErrorSimulatorChannel: Receiving - Client -> ErrorSim", packet);
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
	private DatagramPacket duplicate(DatagramPacket packet) throws IOException {
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
	// Sabotage a packet according to error scenario specification
	/*************************************************************************/
	public DatagramPacket sabotage(DatagramPacket packet)  throws IOException {
		byte[] data = packet.getData();
		//-------------------------------------------------
		// 1-3 -- No sabotage for packets here
		//-------------------------------------------------
		// 4 -- Illegal TFTP operation Error
		if(err.getErrorCode() == 4) {
			if(err.getPacketType()==ErrorScenario.READ_PACKET || err.getPacketType()==ErrorScenario.WRITE_PACKET){
				//-------------------------------------------------
				// Set Opcode to 0xFFFF
				if(err.getFaultType()==ErrorScenario.OPCODE_FAULT){
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					packet.setData(data);
				//-------------------------------------------------
				// Set first 3 bytes of mode to ABC
				} else if(err.getFaultType()==ErrorScenario.MODE_FAULT){
					for(int i=4; i<data.length; i++){
						if(data[i]==0){ // Loop until end of file
							if(i < data.length-3){
								data[i+1] = (byte)'A'; data[i+2] = (byte)'B'; data[i+3] = (byte)'C';
							} 
							break;
						}
					}
					packet.setData(data);
				//-------------------------------------------------
				// Replace first two nulls with 0xFF
				} else if(err.getFaultType()==ErrorScenario.NULL_FAULT){
					int nullCount = 0;
					for(int i=4; i<data.length; i++){
						if(data[i]==0){
							nullCount++;
							data[i] = (byte)0xFF;
						}
						if(nullCount >= 2) break;
					}
					packet.setData(data);
				}
			} else if(err.getPacketType()==ErrorScenario.DATA_PACKET || err.getPacketType()==ErrorScenario.ACK_PACKET){
				//-------------------------------------------------
				// Set Opcode to 0xFFFF
				if(err.getFaultType()==ErrorScenario.OPCODE_FAULT){
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					packet.setData(data,0,packet.getLength());
				//-------------------------------------------------
				// Set Block Num to 0xFFFF
				} else if(err.getFaultType()==ErrorScenario.BLOCK_FAULT){
					data[2] = (byte)0xFF;
					data[3] = (byte)0xFF;
					packet.setData(data,0,packet.getLength());
					if(rollOver){
						if(nextSendToClient) socClient.send(packet);
						else                 socServer.send(packet);			
						packet = lose(packet); // wait for another packet in its place
					}
					
				//-------------------------------------------------
				// Make a packet larger by 100 bytes and fill with 0xFFs
				} else if(err.getFaultType()==ErrorScenario.SIZE_FAULT){
					byte[] newData = new byte[Var.BLOCK_SIZE+100];
					// Copy existing bytes
					for(int i=0; i<data.length; i++) newData[i] = data[i];
					// Copy new bytes
					for(int i=data.length; i<newData.length; i++) newData[i] = (byte)0xFF;
					packet.setData(newData);
				}	
			} else if( err.getPacketType()==ErrorScenario.ERR_PACKET ){
				//-------------------------------------------------
				// Set Opcode to 0xFFFF
				if(err.getFaultType()==ErrorScenario.OPCODE_FAULT){
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					packet.setData(data,0,packet.getLength());
				//-------------------------------------------------
				// Set Error Code to 0xFFFF
				} else if(err.getFaultType()==ErrorScenario.ERRCODE_FAULT){
					data[2] = (byte)0xFF;
					data[3] = (byte)0xFF;
					packet.setData(data,0,packet.getLength());
				}	
			}
		//-------------------------------------------------
		// Unknown Transfer ID Error
		} else if(err.getErrorCode() == 5){
			try{
				DatagramSocket tempSocket  = new DatagramSocket();
				DatagramPacket errorPacket = new DatagramPacket(
					packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort()
				);
				tempSocket.send(errorPacket);
				Log.packet("Unknown Socket: Sending", errorPacket);
				errorPacket = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
				tempSocket.receive(errorPacket);
				Log.packet("Unknown Socket: Receiving", errorPacket);
				tempSocket.close();
			} catch(Exception e){
				e.printStackTrace();
			}
		//-------------------------------------------------
		} return packet;
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
			Log.out("ErrorSimulatorChannel: ERR Packet sabotaged with "+ErrorScenario.FAULT[err.getFaultType()] +" Fault");
		}
		// Run error case if triggered
		if(triggered){
			if     ( err.getErrorCode()==1 ) packet = lose     (packet);
			else if( err.getErrorCode()==2 ) packet = delay    (packet); 
			else if( err.getErrorCode()==3 ) packet = duplicate(packet);	
			else                             packet = sabotage (packet);	
		}
		return packet;
	}
	
	/*************************************************************************/
	// This method checks the received packet and sets program behavior 
	// accordingly. Return true to proceed, false to keep receiving
	/*************************************************************************/
	public boolean parsePacket(DatagramPacket packet) throws IOException{
		byte[] data = packet.getData();
		if(data[0]==Var.READ[0] && data[1]==Var.READ[1]){
			if(expDataBlk==0 && expDataBlk==0){ // ensure no transfer in progess
				expDataBlk = 1;
				expAckBlk  = 1;
				return true;
			}
		} else if(data[0]==Var.WRITE[0] && data[1]==Var.WRITE[1]){
			if(expDataBlk==0 && expDataBlk==0){	// ensure no transfer in progess
				expDataBlk = 1;
				expAckBlk  = 0; // WRQ starts with an ACK 0
				return true;
			}
		} else if(data[0]==Var.DATA[0] && data[1]==Var.DATA[1]){
			// packet is expected
			if(expDataBlk == getBlockNum(packet)){
				if(expDataBlk == 65535){
					rollOver = true;
					expDataBlk = 0;
				} else {
					expDataBlk++;
				}
				if (packet.getLength() < 516) {
					Log.out("ErrorSimulatorChannel: Received Last DATA Packet in Transfer");
					lastData = true;
				}
				return true;
			// Unexpected packet. Forward but keep waiting for expected packet
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
		} else if(data[0]==Var.ACK[0] && data[1] == Var.ACK[1]){
			// packet is expected
			if(expAckBlk == getBlockNum(packet)){
				if(expAckBlk == 65535){
					rollOver = true;
					expAckBlk = 0;
				} else {
					expAckBlk++;
				}
				if (lastData == true) {
					Log.out("ErrorSimulatorChannel: Received Last ACK Packet in Transfer");
					lastAck = true;
				}
				return true;
			// Unexpected packet. Forward but keep waiting for expected packet
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
		}else if(data[0]==Var.ERROR[0] && data[1]==Var.ERROR[1]){
			if (data[3] != 5) error = true; // don't quit on error code 5
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
	
	private void runChannel() throws IOException {
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
		rollOver   = false;
		
		DatagramPacket packet = null;
		initiated = false;
		
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
	
	// Run the channel
	public void run() throws IOException {
		running = true;
		while (running) {
			err = new ErrorScenario(); //create a new scenario after every transfer
			Log.out("Running a new channel between the Client and Server");
			runChannel();
		}
	}
	/*************************************************************************/
	// Compute Block Num from socket
	/*************************************************************************/
	private int getBlockNum(DatagramPacket packet){
		byte[] data = packet.getData();
		return data[2]*256+data[3];
	}

	/*************************************************************************/
	// Close the channel
	/*************************************************************************/
	public void close() {
		if (running) {
			running = false;
			socket.close();
		}
	}

	/*************************************************************************/
	// Check if closed
	/*************************************************************************/
	public boolean isClosed() {
		return !running;
	}

	/*************************************************************************/
	//Run the Error Simulator
	/*************************************************************************/
	public static void main(String[] args) throws SocketException, IOException {
		new ErrorSimulator().run();
	}
}
/*************************************************************************/
