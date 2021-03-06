package util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Test;

public class FileReadWrite {
	private static final String FILENAME = "test.txt";
	private static final String DIR1 = "temp/";
	private static final String DIR2 = "some/boDY\\Once/tOLd/ME///";
	private static final int ITERATIONS = 420;
	
	private FileReader r;
	private FileWriter w;
	
	private void createFile(String filename) throws IOException {
		File f = new File(filename);
		if (!f.exists()) {
			if (f.getParentFile() != null) {
				f.getParentFile().mkdirs();
			}
			f.createNewFile();
		}
	}
	
	private void deleteFile(String filename) {
		File f = new File(filename);
		if (f.exists()) {
			f.delete();
		}
	}

	@After
	public void tearDown() throws Exception {
		if (w != null) {
			w.close();
		}
		if (r != null) {
			r.close();
		}
	}

	@Test
	public void testFilename() throws IOException {
		String filename = DIR1 + FILENAME;
		deleteFile(filename);
		w = new FileWriter(filename);
		r = new FileReader(filename);
		
		assertEquals(FILENAME, r.getFilename());
		assertEquals(FILENAME, w.getFilename());
	}

	@Test
	public void testFileNoExist() throws FileNotFoundException {
		deleteFile(FILENAME);
		
		try {
			r = new FileReader("thisfiledoesnotexist.zip.txt");
			fail("FileNotFoundException did not fire.");
		} catch (FileNotFoundException e) {
			assertEquals("thisfiledoesnotexist.zip.txt (The system cannot find the file specified)", e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testBytes() throws IOException {
		final String filename = DIR1 + FILENAME;
		deleteFile(filename);
		
		w = new FileWriter(filename);
		Random rand = new Random();
		byte[][] bytes = new byte[ITERATIONS][Var.BLOCK_SIZE];
		byte[] end = new byte[ITERATIONS];
		
		for (int i = 0; i < ITERATIONS; i++) {
			rand.nextBytes(bytes[i]);
			w.write(bytes[i]);
		}
		rand.nextBytes(end);
		w.write(end);
		w.close();

		r = new FileReader(filename);
		byte[] buf;
		for (int i = 0; i < ITERATIONS; i++) {
			buf = r.read();
			assertEquals(Var.BLOCK_SIZE, buf.length);
			assertEquals(Log.bBytes(bytes[i]), Log.bBytes(buf));
		}
		buf = r.read();
		assertEquals(ITERATIONS, buf.length);
		assertEquals(Log.bBytes(end), Log.bBytes(buf));
	}

	@Test
	public void testData() throws IOException {
		final String filename = DIR1 + DIR2 + FILENAME;
		deleteFile(filename);
		
		w = new FileWriter(filename);
		Random rand = new Random();
		String expected = "";
		byte[] bytes;
		
		for (int i = 0; i < ITERATIONS; i++) {
			bytes = new byte[rand.nextInt(1000)];
			rand.nextBytes(bytes);
			w.write(bytes);
			expected += Log.bString(bytes);
		}
		w.close();

		r = new FileReader(filename);
		String actual = "";
		int i = 0;
		do {
			bytes = r.read();
			actual += Log.bString(bytes);
		}
		while (!r.isClosed() && i++ < ITERATIONS * 1000);
		
		assertEquals(expected, actual);
	}

	@Test
	public void testOffset() throws IOException {
		final String filename = DIR1 + DIR2 + FILENAME;
		deleteFile(filename);
		
		w = new FileWriter(filename);
		Random rand = new Random();
		String expected = "";
		byte[] bytes, out;
		
		for (int i = 0; i < ITERATIONS; i++) {
			int size = rand.nextInt(1000), offset;
			if (size == 0) {
				offset = 0;
			} else {
				offset = rand.nextInt(size);
			}
			bytes = new byte[size];
			rand.nextBytes(bytes);
			w.write(bytes, offset);
			out = new byte[size - offset];
			System.arraycopy(bytes, offset, out, 0, size - offset);
			expected += Log.bString(out);
		}
		w.close();

		r = new FileReader(filename);
		String actual = "";
		int i = 0, offset;
		do {
			offset = rand.nextInt(100);
			bytes = r.read(offset);
			out = new byte[bytes.length - offset];
			System.arraycopy(bytes, offset, out, 0, bytes.length - offset);
			actual += Log.bString(out);
		}
		while (!r.isClosed() && i++ < ITERATIONS * 1000);
		
		assertEquals(expected, actual);
	}

	@Test
	public void testAbort() throws IOException {
		final String filename = DIR1 + DIR2 + FILENAME;
		deleteFile(filename);
		
		w = new FileWriter(filename);
		w.write("test".getBytes());
		assertTrue(new File(filename).exists());
		w.abort();
		assertFalse(new File(filename).exists());
		
		createFile(filename);
		r = new FileReader(filename);
		assertTrue(new File(filename).exists());
		r.abort();
		assertFalse(new File(filename).exists());
	}

}
