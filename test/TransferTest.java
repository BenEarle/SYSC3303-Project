import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.Log;
import util.Var;

public class TransferTest {
	Client c;
	Server s;
	Thread t;

	@Before
	public void setUp() throws Exception {
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				s = new Server(System.in);
			}
		}, "Server");
		t.start();
	}

	@After
	public void tearDown() throws Exception {
		stop();
	}
	
	public void runClient(InputStream in) throws Exception {
		c = new Client(in);
		c.run();
	}
	
	public void stop() {
		if (c != null && !c.isClosed()) {
			c.close();
		}
		if (s != null && !s.isClosed()) {
			s.close();
		}
	}

	@Test
	public void testClose() throws Exception {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					runClient(new ByteArrayInputStream((
							"S\n"
									).getBytes()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}).start();
		
		Thread.sleep(100);
		
		assertTrue(c.isClosed());
	}
	
	private void testWriteFile(String filename) throws Exception {
		Var.CLIENT_ROOT = "src/testFile/";
		Var.SERVER_ROOT = "temp/";
		File fileClient = new File(Var.CLIENT_ROOT + filename);
		File fileServer = new File(Var.SERVER_ROOT + filename);
		if (fileServer.exists()) {
			fileServer.delete();
		}
		
		runClient(new ByteArrayInputStream((
				"W\n" + 
				filename + "\n" + 
				"S\n"
						).getBytes()));
		stop();
		
		assertEquals(Log.bString(Files.readAllBytes(fileClient.toPath())), Log.bString(Files.readAllBytes(fileServer.toPath())));
	}
	
	private void testReadFile(String filename) throws Exception {
		Var.CLIENT_ROOT = "temp/";
		Var.SERVER_ROOT = "src/testFile/";
		File fileClient = new File(Var.CLIENT_ROOT + filename);
		File fileServer = new File(Var.SERVER_ROOT + filename);
		if (fileClient.exists()) {
			fileClient.delete();
		}
		
		runClient(new ByteArrayInputStream((
				"R\n" + 
				filename + "\n" + 
				"S\n"
						).getBytes()));
		stop();
		
		assertEquals(Log.bString(Files.readAllBytes(fileClient.toPath())), Log.bString(Files.readAllBytes(fileServer.toPath())));
	}

	@Test
	public void test0() throws Exception {
		testWriteFile("0.txt");
		testReadFile("0.txt");
	}

	@Test
	public void test512() throws Exception {
		testWriteFile("512.txt");
		testReadFile("512.txt");
	}

	@Test
	public void test1221() throws Exception {
		testWriteFile("1221.txt");
		testReadFile("1221.txt");
	}

	@Test
	public void testTest() throws Exception {
		testWriteFile("test.txt");
		testReadFile("test.txt");
	}

}
