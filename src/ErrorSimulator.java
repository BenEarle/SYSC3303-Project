import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import util.Log;
import util.Var;

public class ErrorSimulator {
	private DatagramSocket socClient, socServer;
	private SocketAddress addrClient, addrServer;
	private boolean running;

	public ErrorSimulator() throws SocketException {
		socClient = new DatagramSocket(Var.PORT_CLIENT);
		socServer = new DatagramSocket();
		addrServer = new InetSocketAddress("localhost", Var.PORT_SERVER);
	}

	public void run() throws IOException {

		running = true;
		while (running) {
			DatagramPacket packet = new DatagramPacket(new byte[Var.BUF_SIZE], Var.BUF_SIZE);
			
			// Receive from client, then send off the packet to the server.
			socClient.receive(packet);
			Log.packet("Host Client -> Server", packet);
			addrClient = packet.getSocketAddress();
			packet.setSocketAddress(addrServer);
			socServer.send(packet);

			// Receive back from server, then send off the packet to the client from a new socket.
			socServer.receive(packet);
			Log.packet("Host Server -> Client", packet);
			packet.setSocketAddress(addrClient);
			DatagramSocket socSend = new DatagramSocket();
			socSend.send(packet);
			socSend.close();
		}
	}

	public void close() {
		if (running) {
			running = false;
			socClient.close();
			socServer.close();
		}
	}

	public boolean isClosed() {
		return !running;
	}

	public static void main(String[] args) throws SocketException, IOException {
		new ErrorSimulator().run();
	}

}
