package org.asf.centuria.networking.chatserver.rooms;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.ChatServer;

/**
 *
 * Chat Room Object
 *
 * @author Sky Swimmer
 * @since b1.7.4
 *
 */
public class ChatRoom {
	private String roomId;
	private ChatServer server;
	private String type;

	private ArrayList<Object> objects = new ArrayList<Object>();

	public ChatRoom(String type, String roomId, ChatServer server) {
		this.type = type;
		this.roomId = roomId;
		this.server = server;
	}

	/**
	 * Retrieves objects from the connection container, used to store information in
	 * clients.
	 * 
	 * @param type Object type
	 * @return Object instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T1> T1 getObject(Class<T1> type) {
		for (Object obj : objects) {
			if (type.isAssignableFrom(obj.getClass()))
				return (T1) obj;
		}
		return null;
	}

	/**
	 * Adds objects to the connection container, used to store information in
	 * clients.
	 * 
	 * @param obj Object to add
	 */
	public void addObject(Object obj) {
		if (getObject(obj.getClass()) == null)
			objects.add(obj);
	}

	/**
	 * Retrieves the room ID
	 * 
	 * @return Room ID string
	 */
	public String getRoomID() {
		return roomId;
	}

	/**
	 * Retrieves the chat room type
	 * 
	 * @return Chat room type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Retrieves the list of chat clients connected to this room
	 * 
	 * @return Array of ChatClient instances
	 */
	public ChatClient[] getConnectedClients() {
		return Stream.of(server.getClients()).filter(t -> t.isInRoom(roomId)).toArray(t -> new ChatClient[t]);
	}

}
