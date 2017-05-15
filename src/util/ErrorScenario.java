package util;

import java.util.Scanner;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import util.Log;
import util.Var;

public class ErrorScenario {
	/*************************************************************************/
	// Constants
	/*************************************************************************/
	public static final int IRRELEVANT   = -1;
	public static final int UNDEFINED    = -2;
	
	public static final String[] PACKET  = {"READ","WRITE","DATA","ACK"};
	public static final int READ_PACKET  = 1;
	public static final int WRITE_PACKET = 2;
	public static final int DATA_PACKET  = 3;
	public static final int ACK_PACKET   = 4;

	public static final String[] FAULT   = {"NONE","OPCODE","MODE","NULL","BLOCK","SIZE","SOURCE"};
	public static final int OPCODE_FAULT = 1;
	public static final int MODE_FAULT   = 2;
	public static final int NULL_FAULT   = 3;
	public static final int BLOCK_FAULT  = 4;
	public static final int SIZE_FAULT   = 5;
	public static final int SOURCE_FAULT = 6;
	
	/*************************************************************************/
	// Instance Variables
	/*************************************************************************/
	private int errorCode;
	private int packetType;
	private int faultType;
	private int blockNum;
	private Scanner  scanner;
	
	/*************************************************************************/
	// Accessors
	/*************************************************************************/
	public int getErrorCode(){
		return errorCode;
	}
	public int getPacketType(){
		return packetType;
	}
	public int getFaultType(){
		return faultType;
	}
	public int getBlockNum(){
		return blockNum;
	}
	/*************************************************************************/
	// Constructor
	/*************************************************************************/
	public ErrorScenario(){
		scanner = new Scanner(System.in);
		this.errorCode  = promptErrorCode ();
		this.packetType = promptPacketType(errorCode);
		this.faultType  = promptFault     (errorCode, packetType);
		this.blockNum   = promptBlockNum  (errorCode, packetType);
	}
	
	/*************************************************************************/
	// Sabotage a packet according to error scenario specification
	/*************************************************************************/
	public DatagramPacket Sabotage(DatagramPacket packet){
		byte[] data = packet.getData();
		//-------------------------------------------------
		// File not Found Error
		if       (errorCode == 1) {
			//TODO in later Iteration
		//-------------------------------------------------
		// Access Violation Error
		} else if(errorCode == 2) {
			//TODO in later Iteration
		//-------------------------------------------------
		// Disk Full or Allocation Exceeded Error
		} else if(errorCode == 3) {
			//TODO in later Iteration
		//-------------------------------------------------
		// Illegal TFTP operation Error
		} else if(errorCode == 4) {
			if(packetType==READ_PACKET || packetType==WRITE_PACKET){
				//-------------------------------------------------
				// Set Opcode to 0xFFFF
				if(faultType==OPCODE_FAULT){
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					packet.setData(data);
				//-------------------------------------------------
				// Set first 3 bytes of mode to ABC
				} else if(faultType==MODE_FAULT){
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
				} else if(faultType==NULL_FAULT){
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
			// Data and Ack have almost the same data cases
			} else if(packetType==DATA_PACKET || packetType==ACK_PACKET){
				//-------------------------------------------------
				// Set Opcode to 0xFFFF
				if(faultType==OPCODE_FAULT){
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					packet.setData(data,0,packet.getLength());
				//-------------------------------------------------
				// Set Block Num to 0xFFFF
				} else if(faultType==BLOCK_FAULT){
					data[2] = (byte)0xFF;
					data[3] = (byte)0xFF;
					packet.setData(data,0,packet.getLength());
				//-------------------------------------------------
				// Make a packet larger by 100 bytes and fill with 0xFFs
				} else if(faultType==SIZE_FAULT){
					byte[] newData = new byte[Var.BLOCK_SIZE+100];
					// Copy existing bytes
					for(int i=0; i<data.length; i++) newData[i] = data[i];
					// Copy new bytes
					for(int i=data.length; i<newData.length; i++) newData[i] = (byte)0xFF;
					packet.setData(newData);
				}	
			}
		//-------------------------------------------------
		// Unknown Transfer ID Error
		} else if(errorCode == 5){
			try{
				DatagramSocket tempSocket  = new DatagramSocket();
				DatagramPacket errorPacket = new DatagramPacket(
					packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort()
				);
				tempSocket.send(errorPacket);
				Log.packet("INVALID SOCKET SOURCE: Sending Packet", errorPacket);
				errorPacket = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
				tempSocket.receive(errorPacket);
				Log.packet("INVALID SOCKET SOURCE: Receiving Packet", errorPacket);
				tempSocket.close();
			} catch(Exception e){
				e.printStackTrace();
			}
		//-------------------------------------------------
		// File Already Exists Error
		} else if(errorCode == 6) {
			//TODO in later Iteration
		}
		//-------------------------------------------------
		return packet;
	}	
	
	/*************************************************************************/
	// Prompt user for Error Code
	/*************************************************************************/
	public int promptErrorCode(){
		int errorType = UNDEFINED;		
		while(errorType == UNDEFINED){
			Log.out(
				"Select a TFTP Error Code to Test:\n"
			  + " 0) None\n"
			  + " 1) File not Found\n"
			  + " 2) Access Violation\n"
			  + " 3) Disk full or allocation exceeded\n"
			  + " 4) Illegal TFTP operation\n"
			  + " 5) Unknown transfer ID\n"
			  + " 6) No such user"
			);
			switch(scanner.next().trim()){
				case "0": errorType = 0; break;
				case "1": errorType = 1; break;
				case "2": errorType = 2; break;
				case "3": errorType = 3; break;
				case "4": errorType = 4; break;
				case "5": errorType = 5; break;
				case "6": errorType = 6; break;
				default: Log.out("ERROR - Invalid Entry"); break;
			}
		}
		return errorType;
	}
	
	/*************************************************************************/
	// Get Packet Type for Error From User
	/*************************************************************************/
	public int promptPacketType(int errorCode){
		int packetType = UNDEFINED;
		//-------------------------------------------------
		// No Error
		if (errorCode == 0) {
			packetType = IRRELEVANT;
		//-------------------------------------------------
		// File not Found Error
		} else if (errorCode == 1) {
			packetType = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Access Violation Error
		} else if(errorCode == 2) {
			packetType = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Disk Full or Allocation Exceeded Error
		} else if(errorCode == 3) {
			packetType = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Illegal TFTP operation Error
		} else if(errorCode == 4){
			while(packetType == UNDEFINED){
				Log.out(
					"Select a Message Type to Test:\n"
				  + " 1) RRQ\n"
				  + " 2) WRQ\n"
				  + " 3) DATA\n"
				  + " 4) ACK"
				);

				scanner = new Scanner(System.in);
				switch(scanner.next().trim()){
					case "1": packetType = READ_PACKET;  break;
					case "2": packetType = WRITE_PACKET; break;
					case "3": packetType = DATA_PACKET;  break;
					case "4": packetType = ACK_PACKET;   break;
			        default: Log.out("ERROR - Invalid Entry"); break;
				}
			}
		//-------------------------------------------------
		// Unknown Transfer ID Error
		} else if(errorCode == 5){
			while(packetType == UNDEFINED){
				Log.out(
					"Select a Message Type to Test:\n"
				  + " 1) DATA\n"
				  + " 2) ACK"
				);
				scanner = new Scanner(System.in);
				switch(scanner.nextLine().trim()){
					case "1": packetType = DATA_PACKET;  break;
					case "2": packetType = ACK_PACKET;   break;
			        default: Log.out("ERROR - Invalid Entry"); break;
				}
			}
		//-------------------------------------------------
		// File Already Exists Error
		} else if(errorCode == 6) {
			packetType = UNDEFINED;
			//TODO in later Iteration
		}
		//-------------------------------------------------
		return packetType;
	}
	
	/*************************************************************************/
	// Get Message Type for Error From User
	/*************************************************************************/
	public int promptFault(int errorCode, int packetType){
		int faultType = UNDEFINED;
		//-------------------------------------------------
		// No Error
		if (errorCode == 0) {
			faultType = IRRELEVANT;
		///-------------------------------------------------
		// File not Found Error
		} else if (errorCode == 1) {
			faultType = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Access Violation Error
		} else if(errorCode == 2) {
			faultType = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Disk Full or Allocation Exceeded Error
		} else if(errorCode == 3) {
			faultType = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Illegal TFTP operation Error
		} else if(errorCode == 4){
			if( packetType==READ_PACKET || packetType==WRITE_PACKET){
				while(faultType == UNDEFINED){
					Log.out(
						"Select a Fault Case to Test:\n"
					  + " 1) opcode\n"
					  + " 2) mode\n"
					  + " 3) null"
					);

					scanner = new Scanner(System.in);
					switch(scanner.nextLine().trim()){
						case "1": faultType = OPCODE_FAULT; break;
						case "2": faultType = MODE_FAULT;   break;
						case "3": faultType = NULL_FAULT;   break;
				        default: Log.out("ERROR - Invalid Entry"); break;
					}
				}
			} else if( packetType == ACK_PACKET || packetType == DATA_PACKET){
				while(faultType == UNDEFINED){
					Log.out(
						"Select a Fault Case to Test:\n"
					  + " 1) opcode\n"
					  + " 2) block\n"
					  + " 3) size"
					);
					scanner = new Scanner(System.in);
					switch(scanner.nextLine().trim()){
						case "1": faultType = OPCODE_FAULT; break;
						case "2": faultType = BLOCK_FAULT;  break;
						case "3": faultType = SIZE_FAULT;  break;
				        default: Log.out("ERROR - Invalid Entry"); break;
					}
				}
			}
		//-------------------------------------------------
		// Unknown Transfer ID Error
		} else if(errorCode == 5){
			faultType = SOURCE_FAULT;
		//-------------------------------------------------
		// File Already Exists Error
		} else if(errorCode == 6) {
			faultType = UNDEFINED;
			//TODO in later Iteration
		}
		//-------------------------------------------------
		return faultType;
	}
	
	/*************************************************************************/
	// Get Packet Type for BLock Num to Trigger the Error
	/*************************************************************************/
	public int promptBlockNum(int errorCode, int packetType){
		int blockNum = UNDEFINED;	
		//-------------------------------------------------
		// No Error
		if (errorCode == 0) {
			blockNum = IRRELEVANT;
		///-------------------------------------------------
		// File not Found Error
		} else if       (errorCode == 1) {
			blockNum = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Access Violation Error
		} else if(errorCode == 2) {
			blockNum = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Disk Full or Allocation Exceeded Error
		} else if(errorCode == 3) {
			blockNum = UNDEFINED;
			//TODO in later Iteration
		//-------------------------------------------------
		// Illegal TFTP operation Error
		} else if(errorCode == 4){
			if( packetType == READ_PACKET || packetType == WRITE_PACKET){
				blockNum = IRRELEVANT;
			} else {
				while(blockNum == UNDEFINED){
					Log.out(
						"Enter a packet number to trigger the fault:"
					);
					blockNum = Integer.parseInt(scanner.nextLine().trim());
				}
			}
		//-------------------------------------------------
		// Unknown Transfer ID Error
		} else if(errorCode == 5){
			if( packetType == READ_PACKET || packetType == WRITE_PACKET){
				blockNum = IRRELEVANT;
			} else {
				while(blockNum == UNDEFINED){
					Log.out(
						"Enter a packet number to trigger the fault:"
					);
					blockNum = Integer.parseInt(scanner.nextLine().trim());
				}
			}
		//-------------------------------------------------
		// File Already Exists Error
		} else if(errorCode == 6) {
			blockNum = UNDEFINED;
			//TODO in later Iteration
		}
		//-------------------------------------------------
		return blockNum;
	}
	/*************************************************************************/
}
