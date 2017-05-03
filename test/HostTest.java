import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.Log;
import util.Var;

public class HostTest {
	private static final SocketAddress ADDR = new InetSocketAddress("localhost", Var.PORT_CLIENT);
	
	private Host host;
	private DatagramSocket socClient, socServer;

	@Before
	public void setUp() throws Exception {
		Log.enable(false);

		socClient = new DatagramSocket();
		socServer = new DatagramSocket(Var.PORT_SERVER);
		socClient.setSoTimeout(1000);
		socServer.setSoTimeout(1000);
		
		host = new Host();
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
		Thread.sleep(10);
	}

	@After
	public void tearDown() throws Exception {
		socClient.close();
		socServer.close();
		host.close();
	}

	@Test
	public void test() throws IOException {
		DatagramPacket packet;
		Random r = new Random();
		SocketAddress addrHost;
		
		for (int x = 0; x <= Var.BUF_SIZE*2; x++) {
			byte[] buffer;
			byte[] bytes;
			if (x <= Var.BUF_SIZE) {
				buffer = new byte[x];
				bytes = new byte[x];
			} else {
				buffer = new byte[Var.BUF_SIZE*2 - x];
				bytes = new byte[Var.BUF_SIZE*2 - x];
			}
			
			
			for (int y = 0; y < 20; y++) {
				/**
				 * 1. Get random bytes
				 * 2. Send packet from client to host
				 * 3. Wait to receive packet from host to server
				 * 4. Check the data passed along matches
				 * 5. Record the host to server inet to be used later
				 */
				r.nextBytes(buffer);
				packet = new DatagramPacket(buffer, buffer.length, ADDR);
				socClient.send(packet);
				packet = new DatagramPacket(bytes, bytes.length);
				socServer.receive(packet);
				assertEquals(Log.bBytes(buffer), Log.bBytes(bytes));
				addrHost = packet.getSocketAddress();

				/**
				 * 1. Get more random bytes
				 * 2. Send packet from server to host
				 * 3. Wait to receive packet from host to client
				 * 4. Check the data passed along matches
				 * 5. Check the packet is from the correct port on the host
				 */
				r.nextBytes(buffer);
				packet = new DatagramPacket(buffer, buffer.length, addrHost);
				socServer.send(packet);
				packet = new DatagramPacket(bytes, bytes.length);
				socClient.receive(packet);
				assertEquals(Log.bBytes(buffer), Log.bBytes(bytes));
				assertNotEquals(packet.getPort(), Var.PORT_CLIENT);
			}
		}
	}

}
