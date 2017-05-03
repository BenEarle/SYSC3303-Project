package util;

public class Var {
	public static final int PORT_CLIENT = 23;
	public static final int PORT_SERVER = 69;
	public static final int BUF_SIZE    = 420;

	public static final byte[] READ  = new byte[] { 0, 1 };
	public static final byte[] WRITE = new byte[] { 0, 2 };
	public static final byte[] ZERO  = new byte[] { 0 };

	public static final byte[] S_READ  = new byte[] { 0, 3, 0, 1 };
	public static final byte[] S_WRITE = new byte[] { 0, 4, 0, 0 };
}
