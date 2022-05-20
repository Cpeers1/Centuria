package org.asf.emuferal.packets.smartfox;

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
	public boolean parse(String content);

	/**
	 * Builds the Smartfox packet
	 * 
	 * @return Packet content
	 */
	public String build();

	/**
	 * Called to handle the packet
	 * 
	 * @return True if successful, false otherwise
	 */
	public boolean handle();

}
