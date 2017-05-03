package util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader {
	private File file;
	private BufferedInputStream in;
	private boolean closed;

	public FileReader(String filename) throws FileNotFoundException {
		file = new File(filename);
		in = new BufferedInputStream(new FileInputStream(file));
		closed = false;
	}
	
	public String getFilename() {
		return file.getName();
	}
	
	public byte[] read() throws IOException {
		if (closed)
			throw new IOException("File has already been closed.");
		
		byte[] buf = new byte[512];
		int bytesRead = in.read(buf);
		if (bytesRead < 512) {
			byte[] data = new byte[bytesRead];
			System.arraycopy(buf, 0, data, 0, bytesRead);
			return data;
		} else {
			return buf;
		}
	}
	
	public void close() throws IOException {
		in.close();
		closed = true;
	}
	
	public static void main(String[] args) throws IOException {
		FileReader r = new FileReader("test.txt");
		System.out.println(r.read().length);
		System.out.println(r.read().length);
		System.out.println(r.read().length);
	}
}
