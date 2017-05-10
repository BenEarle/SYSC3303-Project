package util;

import java.util.Scanner;
import java.net.DatagramPacket;
import util.Log;
import util.Var;

public class ErrorScenario {

	// Constants
	public static final int IRRELEVANT   = -1;
	public static final int UNDEFINED    = -2;
	
	public static final int READ_PACKET  = 1;
	public static final int WRITE_PACKET = 2;
	public static final int DATA_PACKET  = 3;
	public static final int ACK_PACKET   = 4;
	
	public static final int OPCODE_FAULT = 1;
	public static final int MODE_FAULT   = 2;
	public static final int NULL_FAULT   = 3;
	public static final int BLOCK_FAULT  = 4;
	public static final int SIZE_FAULT   = 5;
	
	// Instance Variables
	public int errorCode;
	public int packetType;
	public int faultType;
	public int blockNum;
	
	/*************************************************************************/
	// Constructor
	/*************************************************************************/
	public ErrorScenario(){
		errorCode  = promptErrorCode ();
		packetType = promptPacketType(errorCode);
		faultType  = promptFault     (errorCode, packetType);
		blockNum  = promptBlockNum  (errorCode, packetType);
	}
	
	
	/*************************************************************************/
	// Sabotages A packet according to error scenario details
	/*************************************************************************/
	public DatagramPacket Sabotage(DatagramPacket packet){
		
		byte[] data;
		data = packet.getData();
		
		//-------------------------------------------------
		if(errorCode == 1) {
			//TODO
			
		//-------------------------------------------------
		} else if(errorCode == 2) {
			//TODO
			
		//-------------------------------------------------
		} else if(errorCode == 3) {
			//TODO
			
		//-------------------------------------------------
		} else if(errorCode == 4) {
			// Read and Write have same error cases
			if(packetType == READ_PACKET || packetType == WRITE_PACKET){
				//-------------------------------------------------
				// Set Opcode to 0xFFFF
				if(faultType == OPCODE_FAULT){
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					packet.setData(data);
				//-------------------------------------------------
				// Set first 3 bytes of mode to ABC
				} else if(faultType == MODE_FAULT){
					for(int i=4; i<data.length; i++){
						if(data[i]==0){ // Loop until end of file
							if(i < data.length-3){
								data[i+1] = (byte)'A';
								data[i+2] = (byte)'B';
								data[i+3] = (byte)'C';
							}
							break;
						}
					}
					packet.setData(data);
				//-------------------------------------------------
				// Replace 1st null with 0xFF
				} else if(faultType == NULL_FAULT){
					for(int i=4; i<data.length; i++){
						if(data[i]==0){
							data[i] = (byte)0xFF;
							break;
						}
					}
					packet.setData(data);
				}
			// Data and Ack have almost the same data cases
			} else if(packetType == DATA_PACKET || packetType == ACK_PACKET){
				//-------------------------------------------------
				// Set Opcode to 0xFFFF
				if(faultType == OPCODE_FAULT){
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					packet.setData(data);
				//-------------------------------------------------
				// Set Block Num to 0xFF
				} else if(faultType == BLOCK_FAULT){
					data[2] = (byte)0xFF;
					data[3] = (byte)0xFF;
					packet.setData(data);
				//-------------------------------------------------
				// Make a bigger packet and fill with 0xFF
				} else if(faultType == SIZE_FAULT){
					byte[] newData = new byte[Var.BLOCK_SIZE+100];
					for(int i=0; i<newData.length; i++){
						newData[i] = data[i];
					}
					for(int i=data.length; i<newData.length; i++){
						newData[i] = (byte)0xFF;
					}
					packet.setData(newData);
				}	
			}
		//-------------------------------------------------
		} else if(errorCode == 5){
			//TODO
		//-------------------------------------------------
		} else if(errorCode == 6) {
			//TODO
		}
		//-------------------------------------------------
		return packet;
	}
	
	
	/*************************************************************************/
	// Get Error Code From User
	/*************************************************************************/
	public int promptErrorCode(){
		
		Scanner scanner   = new Scanner(System.in);
		int     errorType = UNDEFINED;
		String  response  = null;
		
		while(errorType == UNDEFINED){
			Log.out(
				"Select a TFTP Error Code to Test:\n"
			  + " 1) File not Found\n"
			  + " 2) Access Violation\n"
			  + " 3) Disk full or allocation exceeded\n"
			  + " 4) Illegal TFTP operation\n"
			  + " 5) Unknown transfer ID\n"
			  + " 6) No such user\n"
			);
			response = scanner.next();
			switch(response.trim()){
				case "1": errorType = 1; break;
				case "2": errorType = 2; break;
				case "3": errorType = 3; break;
				case "4": errorType = 4; break;
				case "5": errorType = 5; break;
				case "6": errorType = 6; break;
				default:
					Log.out("ERROR - Invalid Entry");
					errorType = UNDEFINED;
					break;
			}
		}
		return errorType;
	}
	
	/*************************************************************************/
	// Get Packet Type for Error From User
	/*************************************************************************/
	public int promptPacketType(int errorCode){
		
		Scanner scanner = new Scanner(System.in);
		int packetType  = UNDEFINED;
		String response = null;
		
		//--------------------------------------------------------------------------
		if(errorCode == 1){
			packetType = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 2){
			packetType = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 3){
			packetType = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 4){
			while(packetType == UNDEFINED){
				Log.out(
					"Select an Message Type to  Error Case to Test:\n"
				  + " 1) RRQ\n"
				  + " 2) WRQ\n"
				  + " 3) DATA\n"
				  + " 4) ACK\n"
				);
				response = scanner.next();
				switch(response){
					case "1": packetType = READ_PACKET;  break;
					case "2": packetType = WRITE_PACKET; break;
					case "3": packetType = DATA_PACKET;  break;
					case "4": packetType = ACK_PACKET;   break;
			        default:
						Log.out("ERROR - Invalid Entry");
						packetType = UNDEFINED;
						break;
				}
			}
		//--------------------------------------------------------------------------
		} else if(errorCode == 5){
			while(packetType == UNDEFINED){
				Log.out(
					"Select an Message Type to  Error Case to Test:\n"
				  + " 1) DATA\n"
				  + " 2) ACK\n"
				);
				response = scanner.nextLine().trim();
				switch(response){
					case "1": packetType = DATA_PACKET;  break;
					case "2": packetType = ACK_PACKET;   break;
			        default:
						Log.out("ERROR - Invalid Entry");
						packetType = UNDEFINED;
						break;
				}
			}
		//--------------------------------------------------------------------------
		} else if(errorCode == 6){
			packetType = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else {
			packetType = IRRELEVANT;
		}
		//--------------------------------------------------------------------------
		return packetType;
	}
	
	/*************************************************************************/
	// Get Message Type for Error From User
	/*************************************************************************/
	public int promptFault(int errorCode, int packetType){
		
		Scanner scanner = new Scanner(System.in);
		int fault       = UNDEFINED;
		String response = null;
		
		//--------------------------------------------------------------------------
		if(errorCode == 1){
			fault = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 2){
			fault = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 3){
			fault = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 4){
			if( packetType == READ_PACKET || packetType == WRITE_PACKET){
				while(fault == UNDEFINED){
					Log.out(
						"Select a Fault Case to Test:\n"
					  + " 1) opcode\n"
					  + " 2) mode\n"
					  + " 3) null\n"
					);
					response = scanner.nextLine().trim();
					switch(response){
						case "1": fault = OPCODE_FAULT; break;
						case "2": fault = MODE_FAULT;   break;
						case "3": fault = NULL_FAULT;   break;
				        default:
							Log.out("ERROR - Invalid Entry");
							fault = UNDEFINED;
							break;
					}
				}
			} else if( packetType == ACK_PACKET){
				while(fault == UNDEFINED){
					Log.out(
						"Select a Fault Case to Test:\n"
					  + " 1) opcode\n"
					  + " 2) block\n"
					);
					response = scanner.nextLine().trim();
					switch(response){
						case "1": fault = OPCODE_FAULT; break;
						case "2": fault = BLOCK_FAULT;  break;
				        default:
							Log.out("ERROR - Invalid Entry");
							fault = UNDEFINED;
							break;
					}
				}
			}  else if( packetType == DATA_PACKET){
				while(fault == UNDEFINED){
					Log.out(
						"Select a Fault Case to Test:\n"
					  + " 1) opcode\n"
					  + " 2) block\n"
					  + " 3) size\n"
					);
					response = scanner.nextLine().trim();
					switch(response){
						case "1": fault = OPCODE_FAULT; break;
						case "2": fault = BLOCK_FAULT;  break;
						case "3": fault = SIZE_FAULT;  break;
				        default:
							Log.out("ERROR - Invalid Entry");
							fault = UNDEFINED;
							break;
					}
				}
			}
		//--------------------------------------------------------------------------
		} else if(errorCode == 5){
			fault = IRRELEVANT;
		//--------------------------------------------------------------------------
		} else if(errorCode == 6){
			fault = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else {
			fault = IRRELEVANT;
		}
		//--------------------------------------------------------------------------
		return fault;
	}
	
	/*************************************************************************/
	// Get Packet Type for BLock Num to Trigger the Error
	/*************************************************************************/
	public int promptBlockNum(int errorCode, int packetType){
		
		Scanner scanner = new Scanner(System.in);
		int blockNum    = UNDEFINED;
		String response = null;
		
		//--------------------------------------------------------------------------
		if(errorCode == 1){
			blockNum = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 2){
			blockNum = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 3){
			blockNum = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else if(errorCode == 4){
			if( packetType == READ_PACKET || packetType == WRITE_PACKET){
				blockNum = IRRELEVANT;
			} else {
				while(blockNum == UNDEFINED){
					Log.out(
						"Enter a packet number to trigger the fault:\n"
					);
					response = scanner.nextLine().trim();
					blockNum = Integer.parseInt(response);
				}
			}
		//--------------------------------------------------------------------------
		} else if(errorCode == 5){
			if( packetType == READ_PACKET || packetType == WRITE_PACKET){
				blockNum = IRRELEVANT;
			} else {
				while(blockNum == UNDEFINED){
					Log.out(
						"Enter a packet number to trigger the fault:\n"
					);
					response = scanner.nextLine().trim();
					blockNum = Integer.parseInt(response);
				}
			}
		//--------------------------------------------------------------------------
		} else if(errorCode == 6){
			blockNum = IRRELEVANT;
			// TODO
		//--------------------------------------------------------------------------
		} else {
			blockNum = IRRELEVANT;
		}
		//--------------------------------------------------------------------------
		return blockNum;
	}
	
}
