import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.Log;
import util.Var;

public class TransferTest {
	Client c;
	Server s;

	@Before
	public void setUp() throws Exception {
		runServer(null);
	}

	@After
	public void tearDown() throws Exception {
		stop();
	}
	
	/**
	 * Start up the server on a new thread.
	 * @param in InputStream
	 * @throws Exception
	 */
	public void runServer(String input) throws Exception {
		if (input == null) {
			s = new Server(System.in);
		} else {
			s = new Server(new ByteArrayInputStream(input.getBytes()));
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				s.run();
			}
		}, "Server").start();
	}
	
	/**
	 * Startup a client on this thread.
	 * @param in InputStream
	 * @throws Exception
	 */
	public void runClient(String input) throws Exception {
		if (input == null) {
			c = new Client(System.in);
		} else {
			c = new Client(new ByteArrayInputStream(input.getBytes()));
		}
		c.run();
	}
	
	/**
	 * Stop the client and server.
	 * @throws InterruptedException 
	 */
	public void stop() throws InterruptedException {
		if (c != null && !c.isClosed()) {
			c.close();
		}
		if (s != null && !s.isClosed()) {
			s.close();
		}
		Thread.sleep(10);
	}

	@Test
	public void testClose() throws Exception {
		// Start client on another thread.
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					runClient("");
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}).start();

		// Wait then close both client and server, then check.
		Thread.sleep(100);
		stop();
//		assertTrue(c.isClosed());
		assertTrue(s.isClosed());

		// Start client and server with closing commands.
		runServer("quit\n");
		runClient("S\n");

		// Wait then check both are closed.
		Thread.sleep(100);
		assertTrue(c.isClosed());
		assertTrue(s.isClosed());
	}
	
	/**
	 * Give a file to be written out by the client and then compared.
	 * @param filename
	 * @throws Exception
	 */
	private void testWriteFile(String filename) throws Exception {
		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = "src/testFile/";
		Var.SERVER_ROOT = "temp/";
		File fileClient = new File(Var.CLIENT_ROOT + filename);
		File fileServer = new File(Var.SERVER_ROOT + filename);
		if (fileServer.exists()) {
			fileServer.delete();
		}
		
		// Run the client with the given file, then close the server.
		runClient(
				"W\n" + 
				filename + "\n" + 
				"S\n");
		stop();
		
		// Make sure the files match.
		assertTrue(fileServer.exists());
		assertEquals(Log.bString(Files.readAllBytes(fileClient.toPath())), Log.bString(Files.readAllBytes(fileServer.toPath())));
	}
	
	/**
	 * Give a file to be read to the client and then compared.
	 * @param filename
	 * @throws Exception
	 */
	private void testReadFile(String filename) throws Exception {
		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = "temp/";
		Var.SERVER_ROOT = "src/testFile/";
		File fileClient = new File(Var.CLIENT_ROOT + filename);
		File fileServer = new File(Var.SERVER_ROOT + filename);
		if (fileClient.exists()) {
			fileClient.delete();
		}

		// Run the client with the given file, then close the server.
		runClient(
				"R\n" + 
				filename + "\n" + 
				"S\n");
		stop();

		// Make sure the files match.
		assertTrue(fileClient.exists());
		assertEquals(Log.bString(Files.readAllBytes(fileClient.toPath())), Log.bString(Files.readAllBytes(fileServer.toPath())));
	}

	@Test
	public void testFiles() throws Exception {
		File folder = new File("src/testFile");
		Log.enable(false);

		// Run test on each file in the test directory.
		for (File f : folder.listFiles()) {
			if (f.isFile()) {
				String filename = f.getName();
				System.out.println("\n---------------------\nTesting '" + filename + "'\n---------------------\n");
				testWriteFile(filename);
				runServer(null);
				testReadFile(filename);
				runServer(null);
			}
		}
	}

}
