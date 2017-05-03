import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FullTest {
	private Server server;
	private ErrorSimulator host;
	private Client client;

	@Before
	public void setUp() throws Exception {
		server = new Server();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					server.run();
				} catch (Exception e) {
					// EAT.
				}
			}
		}).start();

		host = new ErrorSimulator();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					host.run();
				} catch (Exception e) {
					// EAT.
				}
			}
		}).start();

		client = new Client();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					client.run();
				} catch (Exception e) {
					// EAT.
				}
			}
		}).start();
		
	}

	@After
	public void tearDown() throws Exception {
		client.close();
		host.close();
		server.close();
	}

	@Test
	public void test() throws InterruptedException {
		Thread.sleep(100);
		assertTrue(server.isClosed());
	}

}
