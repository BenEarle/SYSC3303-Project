import java.io.InputStream;
import java.util.Scanner;
import util.Log;
import util.Var;

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
		running = false;
	}
	
	public void run() {
		Log.out("Server<Main>: Starting Server");
		Log.out("Server<Main>: starting up control thread...");
		System.out.println(Var.CMDStart);
		System.out.println("Server<Main>: Type 'help' to get a list of available commands.");
		ct = new ControlThread();
		ct.start();

		running = true;
		// Loop until the user types in Quit
		while (running) {
			System.out.print("Server<Main>: ");
			// get user input
			// if user said quit quit = true
			String input = sc.next();
			if (input.toLowerCase().equals("quit") || input.toLowerCase().equals("shutdown") || input.toLowerCase().equals("s") || input.toLowerCase().equals("q")) {
				close();
			} else if (input.toLowerCase().equals("verbose") || input.toLowerCase().equals("v")) {
				System.out.print("Server<Main>: Verbose mode is now " );
				if(Log.toggleEnable()) System.out.println("enabled.");
				else System.out.println("disabled.");
			} else if (input.toLowerCase().equals("help") || input.toLowerCase().equals("h")) {
				System.out.println("Server<Main>: List of available commands: quit, verbose, help, ls, cd.");
			} else if(input.toLowerCase().equals("ls") || input.toLowerCase().equals("dir")){
				System.out.println("Current server directory: " + Var.SERVER_ROOT);
			} else if(input.toLowerCase().equals("cd") || input.toLowerCase().equals("c")){
				System.out.println("New Path: ");
				String newPath = sc.next();
				if(newPath.charAt(newPath.length()-1) != '\\' || newPath.charAt(newPath.length()-1) != '/') {
					newPath += '\\';
				} 
				Var.SERVER_ROOT = newPath;
			} else {
				System.out.println("Server<Main>: Type in 'help' for a list of commands...");
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
		Log.enable(false);
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("v") || args[i].equals("V")) {
				Log.enable(true);
			}
		}	
		new Server(System.in).run();
		Log.out("Server<Main>: Closed main thread.");
	}
}
