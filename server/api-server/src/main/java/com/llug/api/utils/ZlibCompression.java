package com.llug.api.utils;

import java.util.zip.*;
import java.io.*;

import com.wch.commons.utils.StreamUtils;


/**
 * Example program to demonstrate how to use zlib compression with
 * Java.
 * Inspired by http://stackoverflow.com/q/6173920/600500.
 * 
 * http://stackoverflow.com/questions/6173920/zlib-compression-using-deflate-and-inflate-classes-in-java
 */
public class ZlibCompression {

	/**
	 * Compresses a file with zlib compression.
	 */
	public static void compressFile(File raw, File compressed) throws IOException {
		InputStream in = new FileInputStream(raw);
		OutputStream out = new DeflaterOutputStream(new FileOutputStream(compressed));
		StreamUtils.writeStreamToStream(in, out);
		in.close();
		out.close();
	}

	/**
	 * Decompresses a zlib compressed file.
	 */
	public static void decompressFile(File compressed, File raw) throws IOException {
		InputStream in = new InflaterInputStream(new FileInputStream(compressed));
		OutputStream out = new FileOutputStream(raw);
		StreamUtils.writeStreamToStream(in, out);
		in.close();
		out.close();
	}

	public static byte[] decompress(byte[] srcBytes) {
		InputStream in = new InflaterInputStream(new ByteArrayInputStream(srcBytes));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			StreamUtils.writeStreamToStream(in, out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			StreamUtils.safeClose(in);
			StreamUtils.safeClose(out);
		}
		
		return out.toByteArray();
	}
	
	public static String decompress(String src) {
		byte[] bytes = decompress(src.getBytes());
		
		return new String(bytes);
	}

	/**
	 * Main method to test it all.
	 */
	public static void main(String[] args) throws IOException, DataFormatException {
		File compressed = new File("book1out.dfl");
		compressFile(new File("book1"), compressed);
		decompressFile(compressed, new File("decompressed.txt"));
	}
}
