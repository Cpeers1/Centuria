package org.asf.emuferal.networking.smartfox;

import java.io.IOException;
import java.net.Socket;

import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;
import org.asf.emuferal.util.TaskThread;

public class SmartfoxClient {

	private Socket client;
	private BaseSmartfoxServer server;
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
	}

	void stop() {
		taskThread.stopCleanly();
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
	 * Disconnects the client
	 */
	public void disconnect() {
		try {
			client.close();
		} catch (IOException e) {
		}
		server.clientDisconnect(this);
		stop();
		client = null;
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Packet to send
	 * @throws IOException If transmission fails
	 */
	public void sendPacket(ISmartfoxPacket packet) throws IOException {
		taskThread.schedule(() -> {
			try {
				// Instantiate the packet and build
				String content = packet.build();

				// Send packet
				byte[] payload = content.getBytes("UTF-8");
				getSocket().getOutputStream().write(payload);
				getSocket().getOutputStream().write(0);
				getSocket().getOutputStream().flush();
			} catch (IOException e) {
			}
		});
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Raw packet to send
	 * @throws IOException If transmission fails
	 */
	public void sendPacket(String packet) throws IOException {
		taskThread.schedule(() -> {
			try {
				// Send packet
				byte[] payload = packet.getBytes("UTF-8");
				if (getSocket() == null)
					return;
				getSocket().getOutputStream().write(payload);
				getSocket().getOutputStream().write(0);
				getSocket().getOutputStream().flush();
			} catch (IOException e) {
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
