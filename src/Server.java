import java.io.InputStream;
import java.util.Scanner;
import util.Log;

/*************************************************************************/
// This class is the main class for the server. When run it will open a 
// command line interface for the user, typing help will show them the 
// available commands. On start up it will launch the ControlThread, 
// which will open port 69 and start waiting for requests.
/*************************************************************************/
public class Server {
	private boolean running;
	private ControlThread ct;
	private Scanner sc;

	public Server(InputStream in) {
		sc = new Scanner(in);
	}
	
	public void run() {
		System.out.println("SERVER<Main>: Starting Server");
		Log.out("SERVER<Main>: starting up control thread...");
		Log.enable(false);
		ct = new ControlThread();
		ct.start();

		running = true;
		// Loop until the user types in Quit
		while (running) {
			System.out.print("SERVER<Main>: ");
			// get user input
			// if user said quit quit = true
			String input = sc.next();
			if (input.equals("quit")) {
				close();
			} else if (input.equals("verbose")) {
				Log.enable(true);
			} else if (input.equals("help")) {
				System.out.println("SERVER<Main>: List of available commands: quit, verbose, help");
			} else {
				System.out.println("SERVER<Main>: Type in 'help' for a list of commands...");
			}
		}
	}

	public boolean isClosed() {
		return !running;
	}

	public void close() {
		if (running) {
			// This will stop the IO loop
			running = false;
			// This will interrupt and kill the server control thread.
			ct.close(); 
		}
	}

	public static void main(String[] args) {
		new Server(System.in).run();
	}
}
