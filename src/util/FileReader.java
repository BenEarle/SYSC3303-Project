package util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/*************************************************************************/
//This class is used by both the client and the server to read byte 
//arrays from files. It will read and return one block at a time.
/*************************************************************************/

public class FileReader {
	private File file;
	private BufferedInputStream in;
	private boolean closed;

	/**
	 * Create a reader to a file.
	 * 
	 * @param filename
	 *            Path to the file
	 * @throws FileNotFoundException
	 *             If the file does not exist
	 */
	public FileReader(String filename) throws FileNotFoundException {
		file = new File(filename);
		in = new BufferedInputStream(new FileInputStream(file));
		closed = false;
	}

	/**
	 * Get the name of the file.
	 * 
	 * @return
	 */
	public String getFilename() {
		return file.getName();
	}

	/**
	 * Read BLOCK_SIZE bytes into a byte buffer.
	 * 
	 * @return
	 * @throws IOException
	 */
	public synchronized byte[] read() throws IOException {
		if (closed)
			throw new IOException("File has already been closed.");

		byte[] buf = new byte[Var.BLOCK_SIZE];
		int bytesRead = in.read(buf);
		if (bytesRead < Var.BLOCK_SIZE) {
			byte[] data = new byte[bytesRead];
			System.arraycopy(buf, 0, data, 0, bytesRead);
			return data;
		} else {
			return buf;
		}
	}

	/**
	 * Close the file reader.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		in.close();
		closed = true;
	}
}
