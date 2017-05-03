import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.Log;
import util.Var;

public class ClientTest {
	private Client client;
	private DatagramSocket socket;

	@Before
	public void setUp() throws Exception {
		Log.enable(false);
		
		socket = new DatagramSocket(Var.PORT_CLIENT);
		socket.setSoTimeout(1000);
		
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
		Thread.sleep(10);
	}

	@After
	public void tearDown() throws Exception {
		socket.close();
		client.close();
	}

	@Test
	public void test() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		
		for (int i = 0; i < 5; i++) {
			socket.receive(packet);
			assertEquals(packet.getData()[0], Var.WRITE[0]);
			assertEquals(packet.getData()[1], Var.WRITE[1]);
			socket.send(new DatagramPacket(new byte[] {0,4,0,0}, 4, packet.getSocketAddress()));
			
			socket.receive(packet);
			assertEquals(packet.getData()[0], Var.READ[0]);
			assertEquals(packet.getData()[1], Var.READ[1]);
			socket.send(new DatagramPacket(new byte[] {0,3,0,1}, 4, packet.getSocketAddress()));
		}
		
		socket.receive(packet);
	}

}
