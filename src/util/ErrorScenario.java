package util;

import java.util.Scanner;
import util.Log;


public class ErrorScenario {
	/*************************************************************************/
	// Constants
	/*************************************************************************/
	public static final int IRRELEVANT   = -1;
	public static final int UNDEFINED    = -2;
	
	public static final String[] PACKET  = {"READ","WRITE","DATA","ACK","ERROR"};
	public static final int READ_PACKET  = 1;
	public static final int WRITE_PACKET = 2;
	public static final int DATA_PACKET  = 3;
	public static final int ACK_PACKET   = 4;
	public static final int ERR_PACKET   = 5;

	public static final String[] FAULT   = {"NONE","OPCODE","MODE","NULL","BLOCK","SIZE","SOURCE", "LOSS", "DELAY", "DUPLICATE", "ERRCODE"};
	public static final int OPCODE_FAULT    = 1;
	public static final int MODE_FAULT      = 2;
	public static final int NULL_FAULT      = 3;
	public static final int BLOCK_FAULT     = 4;
	public static final int SIZE_FAULT      = 5;
	public static final int SOURCE_FAULT    = 6;
	public static final int LOSS_FAULT      = 7;
	public static final int DELAY_FAULT     = 8;
	public static final int DUPLICATE_FAULT = 9;
	public static final int ERRCODE_FAULT   = 10;
	
	/*************************************************************************/
	// Instance Variables
	/*************************************************************************/
	private int errorCode;
	private int packetType;
	private int faultType;
	private int blockNum;
	private int packetDelay;
	
	private Scanner scanner;
	
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
	public int getDelay(){
		return packetDelay;
	}
	/*************************************************************************/
	// Constructor
	/*************************************************************************/
	public ErrorScenario(){
		scanner = new Scanner(System.in);
		this.errorCode  = promptErrorCode ();
		this.packetType = promptPacketType(errorCode);
		this.faultType  = promptFault     (errorCode, packetType); //sets packet delay if relevant
		this.blockNum   = promptBlockNum  (errorCode, packetType);
		
		/*System.out.println("Case Summary:\n"
				+"ERR: "+this.errorCode+"\n"
				+"PAC: "+this.packetType+"\n"
				+"FLT: "+this.faultType+"\n"
				+"BLK: "+this.blockNum+"\n"
				+"DEL: "+this.packetDelay+"\n"
		);*/
	}
	
	/*************************************************************************/
	// Prompt user for Error Code
	/*************************************************************************/
	public int promptErrorCode(){
		int errorType = UNDEFINED;		
		while(errorType == UNDEFINED){
			Log.out(
				"Select a TFTP Error Category:\n"
			  + " [0] None\n"
			  + " [1] Lose Packet\n"
			  + " [2] Delay Packet\n"
			  + " [3] Duplicate Packet\n"
			  + " [4] Illegal TFTP operation (CODE 4)\n"
			  + " [5] Unknown transfer ID (CODE 5)"
			);
			switch(scanner.next().trim()){
				case "0": errorType = 0; break;
				case "1": errorType = 1; break;
				case "2": errorType = 2; break;
				case "3": errorType = 3; break;
				case "4": errorType = 4; break;
				case "5": errorType = 5; break;
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
		// 0 - No Error
		if (errorCode == 0) {
			packetType = IRRELEVANT;
		//-------------------------------------------------
		// 1,2,3,4 - Multiple Cases
		} else if(1 <= errorCode && errorCode <= 4){
			while(packetType == UNDEFINED){
				Log.out(
					"Select a Message Type to Test:\n"
				  + " [1] RRQ\n"
				  + " [2] WRQ\n"
				  + " [3] DATA\n"
				  + " [4] ACK\n"
				  + " [5] ERR"
				);
				scanner = new Scanner(System.in);
				switch(scanner.next().trim()){
					case "1": packetType = READ_PACKET;  break;
					case "2": packetType = WRITE_PACKET; break;
					case "3": packetType = DATA_PACKET;  break;
					case "4": packetType = ACK_PACKET;   break;
					case "5": packetType = ERR_PACKET;   break;
			        default: Log.out("ERROR - Invalid Entry"); break;
				}
			}
		//-------------------------------------------------
		// 5 - Unknown Transfer ID Error
		} else if(errorCode == 5){
			while(packetType == UNDEFINED){
				Log.out(
					"Select a Message Type to Test:\n"
				  + " [1] DATA\n"
				  + " [2] ACK"
				);
				scanner = new Scanner(System.in);
				switch(scanner.nextLine().trim()){
					case "1": packetType = DATA_PACKET;  break;
					case "2": packetType = ACK_PACKET;   break;
			        default: Log.out("ERROR - Invalid Entry"); break;
				}
			}
		//-------------------------------------------------
		} return packetType;
	}
	
	/*************************************************************************/
	// Get Message Type for Error From User
	/*************************************************************************/
	public int promptFault(int errorCode, int packetType){
		int faultType = UNDEFINED;
		packetDelay   = IRRELEVANT; // irrelevant by default
		//-------------------------------------------------
		// 0 - No Error
		if (errorCode == 0) {
			faultType = IRRELEVANT;
		//-------------------------------------------------
		// 1 - Lose Packet
		} else if (errorCode == 1) {
			faultType = LOSS_FAULT;
		//-------------------------------------------------
		// 2 - Delay Packet
		} else if(errorCode == 2) {
			faultType = DELAY_FAULT;
			while(packetDelay == IRRELEVANT){
				Log.out(
					"Enter a delay time in ms:"
				);
				try {
					packetDelay = Integer.parseInt(scanner.nextLine().trim());
				} catch( Exception e) {
					packetDelay = Integer.parseInt(scanner.nextLine().trim());
				}
			}
		//-------------------------------------------------
		// 3 - Duplicate Packet
		} else if(errorCode == 3) {
			faultType = DUPLICATE_FAULT;
			while(packetDelay == IRRELEVANT){
				Log.out(
					"Enter a delay time between duplicates in ms:"
				);
				try {
					packetDelay = Integer.parseInt(scanner.nextLine().trim());
				} catch( Exception e) {
					packetDelay = Integer.parseInt(scanner.nextLine().trim());
				}
			}
		//-------------------------------------------------
		// 4 - Illegal TFTP operation Error
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
			} else if( packetType == ACK_PACKET || packetType == DATA_PACKET ){
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
			} else if( packetType == ERR_PACKET ){
				while(faultType == UNDEFINED){
					Log.out(
						"Select a Fault Case to Test:\n"
					  + " 1) opcode\n"
					  + " 2) Errcode"
					);
					scanner = new Scanner(System.in);
					switch(scanner.nextLine().trim()){
						case "1": faultType = OPCODE_FAULT; break;
						case "2": faultType = ERRCODE_FAULT;  break;
				        default: Log.out("ERROR - Invalid Entry"); break;
					}
				}
			}
		//-------------------------------------------------
		// 5 - Unknown Transfer ID Error
		} else if(errorCode == 5){
			faultType = SOURCE_FAULT;
		//-------------------------------------------------
		} return faultType;
	}
	
	/*************************************************************************/
	// Get Packet Type for BLock Num to Trigger the Error
	/*************************************************************************/
	public int promptBlockNum(int errorCode, int packetType){
		int blockNum = UNDEFINED;	
		//-------------------------------------------------
		// 0 - No Error
		if (errorCode == 0) {
			blockNum = IRRELEVANT;
		//-------------------------------------------------
		// 1-5 - All Cases
		} else if ( 1 <= errorCode && errorCode <= 5) {
			if( packetType == READ_PACKET || packetType == WRITE_PACKET || packetType == ERR_PACKET) { 
				blockNum = IRRELEVANT;
			} else { // Block Number only relevant for ack and data packets 
				while(blockNum == UNDEFINED){
					Log.out(
						"Enter a packet number to trigger the fault:"
					);
					try {
						blockNum = Integer.parseInt(scanner.nextLine().trim());
					} catch(Exception e) {
						blockNum = Integer.parseInt(scanner.nextLine().trim());
					}
				}
			} 
		//-------------------------------------------------
		} return blockNum;
	}
	/*************************************************************************/
}
