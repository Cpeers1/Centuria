package org.asf.emuferal.networking.chatserver.packets;

import org.asf.emuferal.networking.chatserver.ChatClient;

import com.google.gson.JsonObject;

public abstract class AbstractChatPacket {

	/**
	 * Defines the packet ID
	 * 
	 * @return Packet ID string
	 */
	public abstract String id();

	/**
	 * Creates a new packet instance
	 * 
	 * @return AbstractChatPacket instance
	 */
	public abstract AbstractChatPacket instantiate();

	/**
	 * Parses the packet content
	 * 
	 * @param data Packet content
	 */
	public abstract void parse(JsonObject data);

	/**
	 * Builds the packet content
	 * 
	 * @param data Output packet content
	 */
	public abstract void build(JsonObject data);

	/**
	 * Handles the packet
	 */
	public abstract boolean handle(ChatClient client);

}
