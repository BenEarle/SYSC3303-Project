package util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TFTPErrorHelperTest {
	DatagramSocket socket;
	UDPHelper u;
	
	@Before
	public void setup() throws Exception {
		socket = new DatagramSocket();
		socket.setSoTimeout(100);
		
		u = new UDPHelper();
		u.setIP(InetAddress.getLocalHost());
		u.setPort(socket.getLocalPort());
	}
	
	@After
	public void teardown() {
		socket.close();
		u.close();
	}

	@Test
	public void testRequestPacketChecker() {
		requestPacketChecker(null, "", makePacket(Var.WRITE, "test".getBytes(), Var.ZERO, "octet".getBytes(), Var.ZERO));
		requestPacketChecker(null, "", makePacket(Var.READ, "a".getBytes(), Var.ZERO, "NetAscii".getBytes(), Var.ZERO));
		requestPacketChecker(null, "", makePacket(Var.WRITE, "a".getBytes(), Var.ZERO, "OCTET".getBytes(), Var.ZERO));
		
		requestPacketChecker(4, "Data packet not long enough", makePacket(Var.ZERO));
		
		requestPacketChecker(4, "Invalid OP code for request", makePacket(new byte[]{2,3}));
		requestPacketChecker(4, "Invalid OP code for request", makePacket(new byte[]{0,3}));
		
		requestPacketChecker(4, "Filename missing", makePacket(Var.READ, Var.ZERO, "a".getBytes(), Var.ZERO));
		
		requestPacketChecker(4, "Missing Null terminator after filename", makePacket(Var.READ, "test".getBytes()));
		requestPacketChecker(4, "Missing Null terminator after filename", makePacket(Var.READ));

		requestPacketChecker(4, "Mode missing", makePacket(Var.READ, "testing".getBytes(), Var.ZERO));
		
		requestPacketChecker(4, "Mode not an acceptable form", makePacket(Var.READ, "a".getBytes(), Var.ZERO, Var.ZERO));
		requestPacketChecker(4, "Mode not an acceptable form", makePacket(Var.READ, "a".getBytes(), Var.ZERO, "a".getBytes(), Var.ZERO));
		requestPacketChecker(4, "Mode not an acceptable form", makePacket(Var.READ, "a".getBytes(), Var.ZERO, "octete".getBytes(), Var.ZERO));
		
		requestPacketChecker(4, "Packet does not end with null", makePacket(Var.READ, "test".getBytes(), Var.ZERO, "octet".getBytes()));
	}

	public void requestPacketChecker(Integer expectedError, String expectedMsg, DatagramPacket p) {
		//System.out.println(Log.bString(p.getData()));
		//System.out.println(Log.bBytes(p.getData()));
		Integer actual = TFTPErrorHelper.requestPacketChecker(u, p);

		if (actual != null) {
			DatagramPacket pa = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			try {
				socket.receive(pa);
				assertEquals(expectedMsg, new String(pa.getData(), 4, pa.getLength() - 5));
			} catch (IOException e) {
				fail("Error receive timeout, return was " + actual);
			}
		}

		assertEquals(expectedError, actual);
	}
	
	private DatagramPacket makePacket(byte[]... bytes) {
		// Get the required length of the byte array.
		int length = 0;
		for (byte[] b : bytes) {
			length += b.length;
			if (length > Var.BUF_SIZE) {
				// If the length is too much then return;
				return null;
			}
		}
		
		// Create the buffer to hold the full array.
		byte[] buffer = new byte[length];
		
		// Copy each byte array into the buffer.
		int i = 0;
		for (byte[] b : bytes) {
			System.arraycopy(b, 0, buffer, i, b.length);
			i += b.length;
		}
		
		// Create a packet from the buffer (using the host address) and return it.
		return new DatagramPacket(buffer, buffer.length);
	}

}