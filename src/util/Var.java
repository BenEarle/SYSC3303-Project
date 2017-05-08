package util;

/*************************************************************************/
// This class is used to store constants that are used throughout our 
// project.
/*************************************************************************/

public class Var {
	public static final int PORT_CLIENT = 23;
	public static final int PORT_SERVER = 69;
	public static final int BUF_SIZE    = 516;
	public static final int BLOCK_SIZE  = 512;
	public static final int TIMEOUT = 1000;
	
	public static String CLIENT_ROOT = "src/testFile/";
	public static String SERVER_ROOT = "src/testFile/server/";

	public static final byte[] READ  = new byte[] { 0, 1 };
	public static final byte[] WRITE = new byte[] { 0, 2 };
	public static final byte[] DATA = new byte[] { 0, 3 };
	public static final byte[] ACK = new byte[] { 0, 4 };
	public static final byte[] ERROR = new byte[] { 0, 5 };
	public static final byte[] ZERO  = new byte[] { 0 };

	public static final byte[] ACK_READ  = new byte[] { 0, 3, 0, 1 };
	public static final byte[] ACK_WRITE = new byte[] { 0, 4, 0, 0 };
}
