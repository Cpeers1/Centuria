package org.asf.centuria.networking.smartfox;

import java.io.IOException;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

public abstract class SmartfoxClient {

	/**
	 * Field for storing, eg. a player instance object
	 */
	public Object container;

	/**
	 * Retrieves the client address
	 * 
	 * @return Client address string
	 */
	public abstract String getAddress();
	
	/**
	 * Cleanly stops the client
	 */
	protected abstract void stop();

	/**
	 * Checks if the client is still connected
	 * 
	 * @return True if the client is connected, false otherwise
	 */
	public abstract boolean isConnected();

	/**
	 * Disconnects the client
	 */
	public abstract void disconnect();

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Packet to send
	 */
	public abstract void sendPacket(ISmartfoxPacket packet);

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Raw packet to send
	 */
	public abstract void sendPacket(String packet);

	/**
	 * Reads a single packet
	 * 
	 * @param <T>        Packet return type
	 * @param packetType Expected packet class
	 * @throws IOException If reading fails
	 * @return ISmartfoxPacket instance or null
	 */
	public abstract <T extends ISmartfoxPacket> T readPacket(Class<T> packetType) throws IOException;

	/**
	 * Reads a single raw packet
	 * 
	 * @throws IOException If reading fails
	 * @return Packet string
	 * @throws IOException If reading fails
	 */
	public abstract String readRawPacket() throws IOException;

	/**
	 * Retrieves the server object
	 * 
	 * @return BaseSmartfoxServer instance
	 */
	public abstract BaseSmartfoxServer getServer();

	/**
	 * Called to close the client
	 */
	protected abstract void closeClient();

}
