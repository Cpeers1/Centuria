package org.asf.emuferal.networking.smartfox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;
import org.asf.emuferal.util.TaskThread;

public class SmartfoxClient {

	private Socket client;
	private BaseSmartfoxServer server;
	InputStream input;
	OutputStream output;

	private TaskThread taskThread;

	/**
	 * Field for storing, eg. a player instance object
	 */
	public Object container;

	public SmartfoxClient(Socket client, BaseSmartfoxServer server) {
		this.client = client;
		this.server = server;

		taskThread = new TaskThread(client.toString());
		taskThread.start();

		try {
			input = client.getInputStream();
			output = client.getOutputStream();
		} catch (IOException e) {
		}
	}

	void stop() {
		taskThread.stopCleanly();
		client = null;
	}

	/**
	 * Retrieves the client socket
	 * 
	 * @return Socket instance
	 */
	public Socket getSocket() {
		return client;
	}

	/**
	 * Checks if the client is still connected
	 * 
	 * @return True if the client is connected, false otherwise
	 */
	public boolean isConnected() {
		return client != null;
	}

	/**
	 * Disconnects the client
	 */
	public void disconnect() {
		taskThread.flush(3);
		try {
			if (client != null)
				client.close();
		} catch (IOException e) {
		}
		server.clientDisconnect(this);
		stop();
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Packet to send
	 */
	public void sendPacket(ISmartfoxPacket packet) {
		taskThread.schedule(() -> {
			try {
				// Instantiate the packet and build
				String content = packet.build();

				// Send packet
				byte[] payload = content.getBytes("UTF-8");
				output.write(payload);
				output.write(0);
				output.flush();
			} catch (Exception e) {
			}
		});
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Raw packet to send
	 */
	public void sendPacket(String packet) {
		taskThread.schedule(() -> {
			try {
				// Send packet
				byte[] payload = packet.getBytes("UTF-8");
				if (getSocket() == null)
					return;
				output.write(payload);
				output.write(0);
				output.flush();
			} catch (Exception e) {
			}
		});
	}

	/**
	 * Reads a single packet
	 * 
	 * @param <T>        Packet return type
	 * @param packetType Expected packet class
	 * @throws IOException If reading fails
	 * @return ISmartfoxPacket instance or null
	 */
	public <T extends ISmartfoxPacket> T readPacket(Class<T> packetType) throws IOException {
		return (T) server.<T>readPacket(this, packetType);
	}

	/**
	 * Retrieves the server object
	 * 
	 * @return BaseSmartfoxServer instance
	 */
	public BaseSmartfoxServer getServer() {
		return server;
	}

	/**
	 * Reads a single raw packet
	 * 
	 * @throws IOException If reading fails
	 * @return Packet string
	 */
	public String readRawPacket() throws IOException {
		return server.readRawPacket(this);
	}

}
