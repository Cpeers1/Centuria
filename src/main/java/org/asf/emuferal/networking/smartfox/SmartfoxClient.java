package org.asf.emuferal.networking.smartfox;

import java.io.IOException;
import java.net.Socket;

import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;

public class SmartfoxClient {

	private Socket client;
	private BaseSmartfoxServer server;

	/**
	 * Field for storing, eg. a player instance object
	 */
	public Object container;

	public SmartfoxClient(Socket client, BaseSmartfoxServer server) {
		this.client = client;
		this.server = server;
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
		client = null;
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Packet to send
	 * @throws IOException If transmission fails
	 */
	public void sendPacket(ISmartfoxPacket packet) throws IOException {
		server.sendPacket(this, packet);
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Raw packet to send
	 * @throws IOException If transmission fails
	 */
	public void sendPacket(String packet) throws IOException {
		server.sendPacket(this, packet);
	}

	/**
	 * 
	 * Reads a single packet
	 * 
	 * @param <T>        Packet return type
	 * @param packetType Expected packet class
	 * @throws IOException If reading fails
	 * @return ISmartfoxPacket instance or null
	 */
	public synchronized <T extends ISmartfoxPacket> T readPacket(Class<T> packetType) throws IOException {
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

}
