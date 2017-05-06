import java.io.InputStream;
import java.util.Scanner;

import util.Log;

public class Server {
	private boolean running;
	private ControlThread ct;
	private Scanner sc;
	
	public Server(InputStream in) {
		System.out.println("SERVER<Main>:Starting Server");
		Log.enable(true);
		Log.out("SERVER<Main>: starting up control thread...");
		Log.enable(false);
		ct = new ControlThread();
		ct.start();
		sc = new Scanner(in);
		
		running = true;
		//Loop until the user types in Quit
		while(running){
			Log.out("SERVER<Main>: ");
			//get user input
			//if user said quit quit = true
			String input = sc.next();
			if (input.equals("quit")) {
				close();
			} else if(input.equals("verbose")){
				Log.enable(true);
			} else if (input.equals("help")) {
				System.out.println("SERVER<Main>: List of available commands: quit, verbose, help");
			} else {
				System.out.println("SERVER<Main>: Type in 'help' for a list of commands...");
			}
		}
	}
	
	public boolean isClosed() {
		return running;
	}
	
	public void close() {
		running = false;
		sc.close();
		ct.close();
		ct.interrupt();
	}

	public static void main(String[] args) {
		new Server(System.in);
	}
}
