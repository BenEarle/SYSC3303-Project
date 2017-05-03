import java.util.Scanner;

public class Server {

	@SuppressWarnings({ "deprecation", "resource" })
	public static void main(String[] args) {
		boolean verbose = true;
		
		System.out.println("SERVER<main>: starting up control thread...");
		ControlThread ct = new ControlThread(verbose);
		ct.start();
		Scanner sc = new Scanner(System.in);
		
		boolean quit = false;
		//Loop until the user types in Quit
		while(!quit){
			System.out.print("SERVER<main>: ");
			//get user input
			//if user said quit quit = true
			String input = sc.next();
			if (input.equals("quit")) {
				quit = true;
			} else if(input.equals("verbose")){
				verbose = !verbose;
				ct.setVerbose(verbose);
			} else if (input.equals("help")) {
				System.out.println("SERVER<main>: List of available commands: quit, verbose, help");
			} else {
				System.out.println("SERVER<main>: Type in 'help' for a list of commands...");
			}
		}
		if(quit){
			System.out.println("SERVER<main>: closing the control thread...");
			ct.close();
			ct.stop();
		}
		
	}
}