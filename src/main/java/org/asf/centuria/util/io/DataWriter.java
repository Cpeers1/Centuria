package org.asf.centuria.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 
 * Data Writer
 * 
 * @author Sky Swimmer
 *
 */
public class DataWriter {

	private OutputStream output;

	public DataWriter(OutputStream output) {
		this.output = output;
	}

	public OutputStream getStream() {
		return output;
	}

	/**
	 * Writes a single boolean
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeBoolean(boolean value) throws IOException {
		output.write(value ? 1 : 0);
	}

	/**
	 * Writes a single byte
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeRawByte(byte value) throws IOException {
		output.write(value & 0xff);
	}

	/**
	 * Writes an array of bytes
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeRawBytes(byte[] value) throws IOException {
		output.write(value);
	}

	/**
	 * Writes a single short integer
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeShort(short value) throws IOException {
		writeRawBytes(ByteBuffer.allocate(2).putShort(value).array());
	}

	/**
	 * Writes a single integer
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeInt(int value) throws IOException {
		writeRawBytes(ByteBuffer.allocate(4).putInt(value).array());
	}

	/**
	 * Writes a single long integer
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeLong(long value) throws IOException {
		writeRawBytes(ByteBuffer.allocate(8).putLong(value).array());
	}

	/**
	 * Writes a single float
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeFloat(float value) throws IOException {
		writeRawBytes(ByteBuffer.allocate(4).putFloat(value).array());
	}

	/**
	 * Writes a single double-precision floating-point
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeDouble(double value) throws IOException {
		writeRawBytes(ByteBuffer.allocate(8).putDouble(value).array());
	}

	/**
	 * Writes an array of bytes
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeBytes(byte[] value) throws IOException {
		writeInt(value.length);
		writeRawBytes(value);
	}

	/**
	 * Writes a string
	 * 
	 * @param value Value to write
	 * @throws IOException If writing fails
	 */
	public void writeString(String value) throws IOException {
		writeBytes(value.getBytes("UTF-8"));
	}
}
