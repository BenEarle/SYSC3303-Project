import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.Log;
import util.Var;

public class ServerTest {
	private static final SocketAddress ADDR = new InetSocketAddress("localhost", Var.PORT_SERVER);
	
	private Server server;
	private DatagramSocket socket;

	@Before
	public void setUp() throws Exception {
		Log.enable(false);
		
		socket = new DatagramSocket();
		socket.setSoTimeout(1000);
		
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
		Thread.sleep(10);
	}

	@After
	public void tearDown() throws Exception {
		socket.close();
		server.close();
	}

	@Test
	public void testRead1() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		
		byte[] buf = new byte[] {Var.READ[0],Var.READ[1],'t','e','s','t',0,'o','c','t','e','t',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		socket.receive(packet);
		assertEquals(Log.bBytes(Var.S_READ), Log.bBytes(packet.getData(), Var.S_READ.length));
		assertTrue(!server.isClosed());
	}

	@Test
	public void testRead2() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		
		byte[] buf = new byte[] {Var.READ[0],Var.READ[1],'t',0,'N','e','t','a','s','c','I','I',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		socket.receive(packet);
		assertEquals(Log.bBytes(Var.S_READ), Log.bBytes(packet.getData(), Var.S_READ.length));
		assertTrue(!server.isClosed());
	}

	@Test
	public void testWrite1() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		
		byte[] buf = new byte[] {Var.WRITE[0],Var.WRITE[1],'t','e','s','t',0,'n','e','t','a','s','c','i','i',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		socket.receive(packet);
		assertEquals(Log.bBytes(Var.S_WRITE), Log.bBytes(packet.getData(), Var.S_WRITE.length));
		assertTrue(!server.isClosed());
	}

	@Test
	public void testWrite2() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		
		byte[] buf = new byte[] {Var.WRITE[0],Var.WRITE[1],'e',0,'o','C','T','e','t',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		socket.receive(packet);
		assertEquals(Log.bBytes(Var.S_WRITE), Log.bBytes(packet.getData(), Var.S_WRITE.length));
		assertTrue(!server.isClosed());
	}

	@Test
	public void testMultiple() throws IOException {	
		DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
		byte[] buf;

		buf = new byte[] {Var.READ[0],Var.READ[1],'t','e','s','t',0,'o','c','t','e','t',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		buf = new byte[] {Var.READ[0],Var.READ[1],'t',0,'N','e','t','a','s','c','I','I',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		buf = new byte[] {Var.WRITE[0],Var.WRITE[1],'t','e','s','t',0,'n','e','t','a','s','c','i','i',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		buf = new byte[] {Var.WRITE[0],Var.WRITE[1],'e',0,'o','C','T','e','t',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));

		socket.receive(packet);
		socket.receive(packet);
		socket.receive(packet);
		socket.receive(packet);
		assertTrue(!server.isClosed());
	}

	@Test
	public void testBad1() throws IOException, InterruptedException {		
		byte[] buf = new byte[] {Var.WRITE[0],Var.WRITE[1],'t','e','s','t',0,'o','t','e','t',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		Thread.sleep(10);
		assertTrue(server.isClosed());
	}

	@Test
	public void testBad2() throws IOException, InterruptedException {		
		byte[] buf = new byte[] {Var.WRITE[0],Var.WRITE[1],'t','e','s','t','o','c','t','e','t',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		Thread.sleep(10);
		assertTrue(server.isClosed());
	}

	@Test
	public void testBad3() throws IOException, InterruptedException {		
		byte[] buf = new byte[] {Var.WRITE[0],'t','e','s','t',0,'o','c','t','e','t',0};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		Thread.sleep(10);
		assertTrue(server.isClosed());
	}

	@Test
	public void testBad4() throws IOException, InterruptedException {		
		byte[] buf = new byte[] {Var.READ[0],Var.READ[1],'t','e','s','t',0,'o','c','t','e','t'};
		socket.send(new DatagramPacket(buf, buf.length, ADDR));
		Thread.sleep(10);
		assertTrue(server.isClosed());
	}

}
