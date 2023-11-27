package org.asf.centuria.networking.smartfox;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

public abstract class SmartfoxClient {

	private ArrayList<Object> objects = new ArrayList<Object>();

	// Rates
	private int ppsHighest;
	private long ppsHighestLastChange;
	private int ppsCurrent;
	private int ppsLast;

	/**
	 * Retrieves the amount of packets received in the last second
	 * 
	 * @return Amount of packets received in the last second
	 */
	public int getPacketsPerSecondRate() {
		return ppsLast;
	}

	/**
	 * Retrieves the highest amount of packets that were received in the last PPS
	 * cycle (cycles are around 30 seconds of no increase in highest PPS)
	 * 
	 * @return Amount of packets received in the last cycle
	 */
	public int getHighestPacketsPerSecondRate() {
		return ppsHighest;
	}

	void updatePPS() {
		// Update
		int cPPS = ppsCurrent;
		ppsLast = cPPS;
		ppsCurrent = 0;

		// Check highest count and if its been long enough since the last time the
		// packet rate was high
		if (cPPS > ppsHighest || (System.currentTimeMillis() - ppsHighestLastChange) >= 30000) {
			// Increase highest rate
			ppsHighest = cPPS;
			ppsHighestLastChange = System.currentTimeMillis();
		} else if ((ppsHighest < 50 && ppsHighest >= 30 && cPPS + 10 >= ppsHighest)
				|| (ppsHighest >= 50 && cPPS + (ppsHighest / 10) >= ppsHighest)) {
			// Close to the last number, dont remove the highest count until the amount of
			// packets drops more significantly
			ppsHighestLastChange = System.currentTimeMillis();
		}
	}

	/**
	 * Should be called whenever a packet is read
	 */
	protected void onPacketReceived() {
		// Increase PPS
		ppsCurrent++;
	}

	/**
	 * Retrieves objects from the connection container, used to store information in
	 * clients.
	 * 
	 * @since Beta 1.5.3
	 * @param type Object type
	 * @return Object instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObject(Class<T> type) {
		for (Object obj : objects) {
			if (type.isAssignableFrom(obj.getClass()))
				return (T) obj;
		}
		return null;
	}

	/**
	 * Adds objects to the connection container, used to store information in
	 * clients.
	 * 
	 * @since Beta 1.5.3
	 * @param obj Object to add
	 */
	public void addObject(Object obj) {
		if (getObject(obj.getClass()) == null)
			objects.add(obj);
	}

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
