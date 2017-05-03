package util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class FileWriter {
	private File file;
	private BufferedOutputStream out;
	private boolean closed;

	public FileWriter(String filename) throws IOException {
		file = new File(filename);
		file.createNewFile();
		out = new BufferedOutputStream(new FileOutputStream(file));
		closed = false;
	}
	
	public String getFilename() {
		return file.getName();
	}
	
	public void write(byte[] data) throws IOException {
		if (closed)
			throw new IOException("File has already been closed.");
		
		out.write(data);
	}
	
	public void close() throws IOException {
		out.close();
		closed = true;
	}
	
	public static void main(String[] args) throws IOException {
		FileWriter r = new FileWriter("test.txt");
		byte[] b = new byte[512];
		Random rand = new Random();
		rand.nextBytes(b);
		r.write(b);
		rand.nextBytes(b);
		r.write(b);
		r.close();
	}

}
