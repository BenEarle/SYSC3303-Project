import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import util.Log;
import util.Var;

public class Server {
	
	public static void main(String[] args) throws SocketException {
		Thread ct = new ControlThread();
		ct.start();
		boolean quit = false;
		//Loop until the user types in Quit
		while(!quit){
			//get user input
			//if user said quit quit = true
		}
	}

}
