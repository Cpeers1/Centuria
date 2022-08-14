package org.asf.centuria.packets.smartfox;

import java.io.IOException;

import org.asf.centuria.networking.smartfox.SmartfoxClient;

public interface ISmartfoxPacket {

	/**
	 * Instantiates the packet
	 * 
	 * @return ISmartfoxPacket instance
	 */
	public ISmartfoxPacket instantiate();

	/**
	 * Checks if the content given can be parsed into this packet
	 * 
	 * @param content Packet content
	 */
	public boolean canParse(String content);

	/**
	 * Parses the Smartfox packet
	 * 
	 * @param content Packet content
	 */
	public boolean parse(String content) throws IOException;

	/**
	 * Builds the Smartfox packet
	 * 
	 * @return Packet content
	 */
	public String build() throws IOException;

	/**
	 * Called to handle the packet
	 * 
	 * @return True if successful, false otherwise
	 */
	public boolean handle(SmartfoxClient client) throws IOException;

}
