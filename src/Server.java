import java.io.InputStream;
import java.util.Scanner;

public class Server {
	private boolean running;
	private ControlThread ct;
	private Scanner sc;
	
	public Server(InputStream in) {
		boolean verbose = true;
		
		System.out.println("SERVER<Main>: starting up control thread...");
		ct = new ControlThread(verbose);
		ct.start();
		sc = new Scanner(in);
		
		running = true;
		//Loop until the user types in Quit
		while(running){
			System.out.print("SERVER<Main>: ");
			//get user input
			//if user said quit quit = true
			String input = sc.next();
			if (input.equals("quit")) {
				close();
			} else if(input.equals("verbose")){
				verbose = !verbose;
				ct.setVerbose(verbose);
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
