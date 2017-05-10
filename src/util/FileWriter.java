package util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/*************************************************************************/
// This class is used by both the client and the server to write byte 
// arrays out to a file. It takes a file name as input and must be closed
// to ensure that the data is written correctly.
/*************************************************************************/

public class FileWriter {
	private File file;
	private BufferedOutputStream out;
	private boolean closed;

	/**
	 * Create a writer to a file.
	 * 
	 * @param filename
	 *            Path to the file
	 * @throws IOException
	 *             If the file is locked
	 */
	public FileWriter(String filename) throws IOException {
		file = new File(filename);
		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		file.createNewFile();
		out = new BufferedOutputStream(new FileOutputStream(file));
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
	 * Write a byte buffer into the file.
	 * 
	 * @param data
	 * @throws IOException
	 */
	public synchronized void write(byte[] data) throws IOException {
		if (closed)
			throw new IOException("File has already been closed.");

		out.write(data);
	}

	/**
	 * Write a byte buffer into the file, using an offset on the array.
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void write(byte[] data, int offset) throws IOException {
		if (closed)
			throw new IOException("File has already been closed.");

		out.write(data, offset, data.length - offset);
	}

	/**
	 * Close the file writer.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		out.close();
		closed = true;
	}
}
