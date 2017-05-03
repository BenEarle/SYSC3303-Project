package util;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileReadWrite {
	private static final String FILENAME = "test.txt";
	private static final int ITERATIONS = 512;
	
	private FileReader r;
	private FileWriter w;

	@Before
	public void setUp() throws Exception {
		r = new FileReader(FILENAME);
		w = new FileWriter(FILENAME);
	}

	@After
	public void tearDown() throws Exception {
		r.close();
		w.close();
	}

	@Test
	public void testFilename() {
		assertEquals(FILENAME, r.getFilename());
		assertEquals(FILENAME, w.getFilename());
	}

	@Test
	public void testFileNoExist() throws FileNotFoundException {
		try {
			r = new FileReader("thisfiledoesnotexist.zip.txt");
			fail("FileNotFoundException did not fire.");
		} catch (FileNotFoundException e) {
			assertEquals("thisfiledoesnotexist.zip.txt (The system cannot find the file specified)", e.getMessage());
		}
	}

	@Test
	public void test1() throws IOException {
		Random rand = new Random();
		byte[][] bytes = new byte[ITERATIONS][512];
		byte[] end = new byte[400];
		
		for (int i = 0; i < ITERATIONS; i++) {
			rand.nextBytes(bytes[i]);
			w.write(bytes[i]);
		}
		rand.nextBytes(end);
		w.write(end);
		w.close();
		
		for (int i = 0; i < ITERATIONS; i++) {
			assertEquals(Log.bBytes(bytes[i]), Log.bBytes(r.read()));
		}
		assertEquals(Log.bBytes(end), Log.bBytes(r.read()));
	}

	@Test
	public void test2() throws IOException {
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
		
		String actual = "";
		int i = 0;
		do {
			bytes = r.read();
			actual += Log.bString(bytes);
		}
		while (bytes.length == 512 && i++ < ITERATIONS);
		
		assertEquals(expected, actual);
	}

}
