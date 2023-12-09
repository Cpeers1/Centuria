package org.asf.centuria.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 
 * Data Reader
 * 
 * @author Sky Swimmer
 *
 */
public class DataReader {

	private InputStream input;
	private int pendingByte = -1;

	public DataReader(InputStream input) {
		this.input = input;
	}

	public InputStream getStream() {
		return input;
	}

	/**
	 * Checks if a binary entry is present for reading
	 * 
	 * @return True if a binary entry is ready to be read, false if there is no data
	 * @throws IOException If the stream is closed
	 */
	public boolean hasNext() throws IOException {
		if (pendingByte != -1)
			return true;
		try {
			pendingByte = input.read();
		} catch (IOException e) {
			return false;
		}
		return pendingByte != -1;
	}

	/**
	 * Reads all bytes
	 * 
	 * @return Array of bytes
	 * @throws IOException If reading fails
	 */
	public byte[] readAllBytes() throws IOException {
		byte[] res = input.readAllBytes();
		if (pendingByte != -1) {
			byte[] nRes = new byte[res.length + 1];
			for (int i = 0; i < res.length; i++)
				nRes[i + 1] = res[i];
			nRes[0] = (byte) pendingByte;
			res = nRes;
			pendingByte = -1;
		}
		return res;
	}

	/**
	 * Reads a number bytes
	 * 
	 * @param num Number of bytes to read
	 * @return Array of bytes
	 * @throws IOException If reading fails
	 */
	public byte[] readNBytes(int num) throws IOException {
		byte[] res = new byte[num];
		int c = 0;
		while (true) {
			if (pendingByte != -1) {
				int p = pendingByte;
				pendingByte = -1;
				res[c++] = (byte) p;
			}
			try {
				int r = input.read(res, c, num - c);
				if (r == -1)
					break;
				c += r;
			} catch (Exception e) {
				int b = input.read();
				if (b == -1)
					break;
				res[c++] = (byte) b;
			}
			if (c >= num)
				break;
		}
		return Arrays.copyOfRange(res, 0, c);
	}

	/**
	 * Reads a single byte
	 * 
	 * @return Byte value
	 * @throws IOException If reading fails
	 */
	public byte readRawByte() throws IOException {
		if (pendingByte != -1) {
			int p = pendingByte;
			pendingByte = -1;
			return (byte) p;
		}
		int i = input.read();
		if (i == -1)
			throw new IOException("Stream closed");
		return (byte) i;
	}

	/**
	 * Reads a single integer
	 * 
	 * @return Integer value
	 * @throws IOException If reading fails
	 */
	public int readInt() throws IOException {
		return ByteBuffer.wrap(readNBytes(4)).getInt();
	}

	/**
	 * Reads a single short integer
	 * 
	 * @return Short value
	 * @throws IOException If reading fails
	 */
	public short readShort() throws IOException {
		return ByteBuffer.wrap(readNBytes(2)).getShort();
	}

	/**
	 * Reads a single long integer
	 * 
	 * @return Long value
	 * @throws IOException If reading fails
	 */
	public long readLong() throws IOException {
		return ByteBuffer.wrap(readNBytes(8)).getLong();
	}

	/**
	 * Reads a single floating-point
	 * 
	 * @return Float value
	 * @throws IOException If reading fails
	 */
	public float readFloat() throws IOException {
		return ByteBuffer.wrap(readNBytes(4)).getFloat();
	}

	/**
	 * Reads a single double-precision floating-point
	 * 
	 * @return Double value
	 * @throws IOException If reading fails
	 */
	public double readDouble() throws IOException {
		return ByteBuffer.wrap(readNBytes(4)).getDouble();
	}

	/**
	 * Reads a single character
	 * 
	 * @return Char value
	 * @throws IOException If reading fails
	 */
	public char readChar() throws IOException {
		return ByteBuffer.wrap(readNBytes(2)).getChar();
	}

	/**
	 * Reads a single boolean
	 * 
	 * @return Boolean value
	 * @throws IOException If reading fails
	 */
	public boolean readBoolean() throws IOException {
		int data = readRawByte();
		if (data != 0)
			return true;
		else
			return false;
	}

	/**
	 * Reads a single length-prefixed byte array
	 * 
	 * @return Array of bytes
	 * @throws IOException If reading fails
	 */
	public byte[] readBytes() throws IOException {
		return readNBytes(readInt());
	}

	/**
	 * Reads a string
	 * 
	 * @return String value
	 * @throws IOException If reading fails
	 */
	public String readString() throws IOException {
		return new String(readBytes(), "UTF-8");
	}

}
