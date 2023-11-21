package org.asf.centuria.networking.persistentservice.networking;

import org.asf.centuria.networking.persistentservice.BasePersistentServiceClient;
import org.asf.centuria.networking.persistentservice.BasePersistentServiceServer;

import com.google.gson.JsonObject;

public abstract class AbstractPersistentServicePacket<T extends BasePersistentServiceClient<T, T2>, T2 extends BasePersistentServiceServer<T, T2>> {

	/**
	 * Defines the packet ID
	 * 
	 * @return Packet ID string
	 */
	public abstract String id();

	/**
	 * Creates a new packet instance
	 * 
	 * @return AbstractPersistentServicePacket instance
	 */
	public abstract AbstractPersistentServicePacket<T, T2> instantiate();

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
	public abstract boolean handle(T client);

}
