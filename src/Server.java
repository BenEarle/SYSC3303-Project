import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import util.Log;
import util.Var;

public class Server {
	
	public static void main(String[] args) throws SocketException {
		ControlThread ct = new ControlThread();
		ct.start();
		boolean quit = false;
		//Loop until the user types in Quit
		while(!quit){
			//get user input
			//if user said quit quit = true
			if (args.length > 0) {
				int i;
				for (i = 0; i < args.length; i++) {
					if(args[i].equals("q") & args[i+1].equals("u") & args[i+1].equals("i") & args[i+1].equals("t")) {
						quit = true;
					}
				}
			}
			if (quit) ct.close();
		}
	}

}
