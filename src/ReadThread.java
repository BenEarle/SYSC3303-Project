import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;

import util.FileReader;
import util.FileWriter;
import util.Log;
import util.Var;

public class ReadThread extends ClientResponseThread {

	public ReadThread(DatagramPacket initialPacket, boolean verbose) {
		super(initialPacket, verbose);
	}

	/*
	 * RRQ
	 * Open FR
	 * do
	 * 	Read data from file
	 * 	Send data
	 * 	Receive ack 
	 * while(data.len == 512)
	 * 
	 */
	
	public void run() {
		DatagramPacket packet;
		FileReader fr = null;
		try {
			fr = new FileReader(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] blockNum = new byte[2];
		blockNum[1] = 0x01;
		blockNum[0] = 0x00;
		byte[] data = null;
		try {
			data = fr.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int dataLength = data.length;
		super.sendPacket(data);
		if(verbose) Log.out("SERVER<ReadThread>: Sending READ Data:" + data.toString());	
		while (dataLength % Var.BLOCK_SIZE == 0) {
			packet = super.receivePacket();
			if (packet.getData()[1] == Var.ACK[1]) {
				if (packet.getData()[2] == blockNum[0] && packet.getData()[3] == blockNum[1]) {
					try {
						data = fr.read();
					} catch (IOException e) {
						e.printStackTrace();
					}
					dataLength = data.length;
					blockNum = bytesIncrement(blockNum);
					super.sendPacket(data);
					if(verbose) Log.packet("SERVER<ReadThread>: Sending READ Data", packet);
				}
			}
		}
		packet = super.receivePacket();
		if (packet.getData()[1] == Var.ACK[1]) {
			if (packet.getData()[2] == blockNum[0] && packet.getData()[3] == blockNum[1] && verbose) {
				Log.out("SERVER<ReadThread>: Read completed successfully.");
			}
		}
	}
	
	private byte[] bytesIncrement(byte[] data) {
		if (data[1] == 0xff) {
			data[0]++;
			data[1] = 0x00;
		} else {
			data[1]++;
		}
		return data;
	}
	
}
