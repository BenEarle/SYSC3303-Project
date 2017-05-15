import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Random;

import util.*;

public class FillItUp {
	private static final int BLOCK_SIZE = 1024*1024 * 10;

	public static void main(String[] args) throws IOException {
//		makeGiant("temp/GIANT", 50);
		fillMyDrive(6);
//		lockIt(Var.SERVER_ROOT + "s_1221.txt");
//		lockIt(Var.CLIENT_ROOT + "c_1221.txt");
	}
	
	public static void fillMyDrive(int remaining) throws IOException {
		File massive = new File("temp/MASSIVE");
		if (massive.exists()) {
			massive.delete();
		} else {
			massive.getParentFile().mkdirs();
		}
		long size = new File("/").getUsableSpace();
        RandomAccessFile f = new RandomAccessFile(massive, "rw");
        f.setLength(size - remaining*1024);
        f.close();
	}
	
	public static void makeGiant(String filename, long size) throws IOException {
		size *= 1024 * 1024;
		byte[] b = new byte[BLOCK_SIZE];
		Random r = new Random();
		FileWriter fw = new FileWriter(filename);

		while (size > BLOCK_SIZE) {
			r.nextBytes(b);
			fw.write(b);
			
			size -= BLOCK_SIZE;
		}
		b = new byte[(int) size];
		fw.write(b);
		
		fw.close();
	}
	
	public static void lockIt(String filename) throws IOException {
		FileOutputStream in = new FileOutputStream(filename);
		try {
		    FileLock lock = in.getChannel().lock();
		    try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        lock.release();
		} finally {
		    in.close();
		}
	}

}
