import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.Log;
import util.Var;

public class TransferTest {
	public static final String FILE_LOCATION = "testFiles/";
	public static final String TEMP_LOCATION = "temp/";
	
	private static final String LANG_EXCEPTION = "Exception";
	private static final String LANG_DISK_FULL = "Disk full, cannot complete opperation.";
	private static final String LANG_PACKET_3 = "Error packet type 3 received.";
	private static final String LANG_ALREADY_EXISTS = "File already exists.";
	private static final String LANG_PACKET_6 = "Error packet type 6 received.";
	private static final String LANG_DENIED = "Access denied for c_jpg.jpg.";
	private static final String LANG_PACKET_2 = "Error packet type 2 received.";
	
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
	 * @param wait If sleep for the server to start
	 * @throws Exception
	 */
	public void runServer(String input, boolean wait) throws Exception {
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

		if (wait) {
			int i = 100;
			while (s.isClosed()) {
				Thread.sleep(10);
				if (i-- == 0) {
					fail("Server was never started.");
				}
			}
		}
	}
	public void runServer(String input) throws Exception {
		runServer(input, true);
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
		Thread.sleep(50);
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
		stop();
//		assertTrue(c.isClosed());
		assertTrue(s.isClosed());

		// Start client and server with closing commands.
//		runServer("quit\n", false);
		runClient("S\n");
//		Thread.sleep(50);

		// Wait then check both are closed.
//		int i = 100;
//		while (s == null || !s.isClosed() || c == null || !c.isClosed()) {
//			Thread.sleep(10);
//			if (i-- == 0) {
//				fail("Server or client was never started.");
//			}
//		}
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
		Var.CLIENT_ROOT = FILE_LOCATION;
		Var.SERVER_ROOT = TEMP_LOCATION;
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
		assertTrue("File copied to server does not exist", fileServer.exists());
		assertEquals(Log.bString(Files.readAllBytes(fileClient.toPath())), Log.bString(Files.readAllBytes(fileServer.toPath())));
	}
	
	/**
	 * Give a file to be read to the client and then compared.
	 * @param filename
	 * @throws Exception
	 */
	private void testReadFile(String filename) throws Exception {
		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = TEMP_LOCATION;
		Var.SERVER_ROOT = FILE_LOCATION;
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
		assertTrue("File copied to client does not exist", fileClient.exists());
		assertEquals(Log.bString(Files.readAllBytes(fileClient.toPath())), Log.bString(Files.readAllBytes(fileServer.toPath())));
	}

	@Test
	public void testFiles() throws Exception {
		final int INTERATIONS = 1;
		File folder = new File(FILE_LOCATION);
		Log.enable(false);

		// Run test on each file in the test directory.
		for (int i = 0; i < INTERATIONS; i++) {
			for (File f : folder.listFiles()) {
				if (f.isFile()) {
					String filename = f.getName();
					System.out.println("\n---------------------\nTesting WRITE '" + filename + "'\n---------------------\n");
					testWriteFile(filename);
					runServer(null);
					System.out.println("\n---------------------\nTesting READ '" + filename + "'\n---------------------\n");
					testReadFile(filename);
					runServer(null);
				}
			}
		}
		Thread.sleep(50);
	}

	@Test
	public void testConcurrent() throws Exception {
		File folder = new File(FILE_LOCATION);
		Log.enable(false);
		Var.CLIENT_ROOT = TEMP_LOCATION;
		Var.SERVER_ROOT = FILE_LOCATION;

		// Run test on each file in the test directory.
		ArrayList<Thread> t = new ArrayList<>();
		for (File f : folder.listFiles()) {
			if (f.isFile()) {
				String filename = f.getName();
				Thread aaa = new Thread(new Runnable() {

					@Override
					public void run() {
						File fileClient = new File(Var.CLIENT_ROOT + filename);
						if (fileClient.exists()) {
							fileClient.delete();
						}
						try {
							runClient(
									"R\n" + 
									filename + "\n" + 
									"S\n");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
				});
				aaa.start();
				t.add(aaa);
			}
		}
		
		do {
			Thread.sleep(50);
		    Iterator<Thread> itr = t.iterator();
		    while (itr.hasNext()) {
		    	Thread element = itr.next();
		    	if (!element.isAlive()) {
		    		itr.remove();
		    	}
		    }
		} while (!t.isEmpty());
		stop();
		
		for (File f : folder.listFiles()) {
			if (f.isFile()) {
				File fileClient = new File(Var.CLIENT_ROOT + f.getName());
				File fileServer = new File(Var.SERVER_ROOT + f.getName());
				assertTrue("File copied to client does not exist", fileClient.exists());
				assertEquals(Log.bString(Files.readAllBytes(fileClient.toPath())), Log.bString(Files.readAllBytes(fileServer.toPath())));
			}
		}
	}
	
	public void alreadyExists(String filename, String type) throws Exception {
		Log.enable(false);
		Log.saveLog(true);
		// Run the client with the given file, then close the server.
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					runClient(
							type + "\n" + 
							filename + "\n" + 
							"S\n");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		Thread.sleep(100);
		runClient(
				type + "\n" + 
				filename + "\n" + 
				"S\n");
		stop();

		String log = Log.getLog();
		assertFalse("There was an exception in the log", log.contains(LANG_EXCEPTION));
		assertTrue("Already exists error was not found in log", log.contains(LANG_ALREADY_EXISTS));
		log = log.substring(log.indexOf(LANG_ALREADY_EXISTS) + LANG_ALREADY_EXISTS.length());
		assertTrue("packet type 3 not received in log", log.contains(LANG_PACKET_6));
		assertTrue("Already exists error was not found in log", log.contains(LANG_ALREADY_EXISTS));
	}

	@Test
	public void testAlreadyExistsClient() throws Exception {
		String filename = "c_jpg.jpg";
		
		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = TEMP_LOCATION;
		Var.SERVER_ROOT = FILE_LOCATION;
		File fileClient = new File(Var.CLIENT_ROOT + filename);
		if (fileClient.exists()) {
			fileClient.delete();
		}
		
		alreadyExists(filename, "R");
	}

	@Test
	public void testAlreadyExistsServer() throws Exception {
		String filename = "c_jpg.jpg";
		
		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = FILE_LOCATION;
		Var.SERVER_ROOT = TEMP_LOCATION;
		File fileServer = new File(Var.SERVER_ROOT + filename);
		if (fileServer.exists()) {
			fileServer.delete();
		}
		
		alreadyExists(filename, "W");
	}
	
	@Test
	public void testOutOfSpaceWrite() throws Exception {
		String filename = "50mb.zip";

		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = FILE_LOCATION;
		Var.SERVER_ROOT = TEMP_LOCATION;
		File fileServer = new File(Var.SERVER_ROOT + filename);
		if (fileServer.exists()) {
			fileServer.delete();
		}
		
		FillItUp.fillMyDrive(1);

		Log.enable(false);
		Log.saveLog(true);
		// Run the client with the given file, then close the server.
		runClient(
				"W\n" + 
				filename + "\n" + 
				"S\n");
		stop();
		
		FillItUp.fillMyDrive(-1);
		
		// Check the log.
		String log = Log.getLog();
		assertFalse("There was an exception in the log", log.contains(LANG_EXCEPTION));
		assertTrue("Disk full error was not found in log", log.contains(LANG_DISK_FULL));
		log = log.substring(log.indexOf(LANG_DISK_FULL) + LANG_DISK_FULL.length());
		assertTrue("packet type 3 not received in log", log.contains(LANG_PACKET_3));
		assertTrue("Disk full error was not found in log", log.contains(LANG_DISK_FULL));
		// Make sure the doesn't exist.
		assertFalse("File copied to server exists", fileServer.exists());
	}
	
	@Test
	public void testOutOfSpaceRead() throws Exception {
		String filename = "50mb.zip";

		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = TEMP_LOCATION;
		Var.SERVER_ROOT = FILE_LOCATION;
		File fileClient = new File(Var.CLIENT_ROOT + filename);
		if (fileClient.exists()) {
			fileClient.delete();
		}
		
		FillItUp.fillMyDrive(1);

		Log.enable(false);
		Log.saveLog(true);
		// Run the client with the given file, then close the server.
		runClient(
				"R\n" + 
				filename + "\n" + 
				"S\n");
		stop();
		
		FillItUp.fillMyDrive(-1);

		// Check the log.
		String log = Log.getLog();
		assertFalse("There was an exception in the log", log.contains(LANG_EXCEPTION));
		assertTrue("Disk full error was not found in log", log.contains(LANG_DISK_FULL));
		log = log.substring(log.indexOf(LANG_DISK_FULL) + LANG_DISK_FULL.length());
		assertTrue("packet type 3 not received in log", log.contains(LANG_PACKET_3));
		assertTrue("Disk full error was not found in log", log.contains(LANG_DISK_FULL));
		// Make sure the doesn't exist.
		assertFalse("File copied to server exists", fileClient.exists());
	}
	
	private void locked(String filename, String type) throws Exception {
		Log.enable(false);
		Log.saveLog(true);
		// Run the client with the given file, then close the server.
		runClient(
				type + "\n" + 
				filename + "\n" + 
				"S\n");
		stop();
		
		FillItUp.unlock();
		
		// Check log.
		String log = Log.getLog();
		assertFalse("There was an exception in the log", log.contains(LANG_EXCEPTION));
		assertTrue("Disk full error was not found in log", log.contains(LANG_DENIED));
		log = log.substring(log.indexOf(LANG_DENIED) + LANG_DENIED.length());
		assertTrue("packet type 3 not received in log", log.contains(LANG_PACKET_2));
		assertTrue("Disk full error was not found in log", log.contains(LANG_DENIED));
	}
	
	@Test
	public void testLockedClient() throws Exception {
		String filename = "c_jpg.jpg";

		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = FILE_LOCATION;
		Var.SERVER_ROOT = TEMP_LOCATION;
		File fileServer = new File(Var.SERVER_ROOT + filename);
		if (fileServer.exists()) {
			fileServer.delete();
		}
		FillItUp.lockIt(Var.CLIENT_ROOT + filename);

		
		locked(filename, "W");
		// Make sure the doesn't exist.
		assertFalse("File copied to client exists", fileServer.exists());
	}
	
	@Test
	public void testLockedServer() throws Exception {
		String filename = "c_jpg.jpg";

		// Set test roots and build files to be used.
		Var.CLIENT_ROOT = TEMP_LOCATION;
		Var.SERVER_ROOT = FILE_LOCATION;
		File fileClient = new File(Var.CLIENT_ROOT + filename);
		if (fileClient.exists()) {
			fileClient.delete();
		}
		FillItUp.lockIt(Var.SERVER_ROOT + filename);

		
		locked(filename, "R");
		// Make sure the doesn't exist.
		assertFalse("File copied to client exists", fileClient.exists());
	}

}
