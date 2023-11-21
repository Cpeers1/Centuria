package org.asf.centuria.networking.chatserver.rooms;

/**
 * 
 * Chat Room Container
 * 
 * @author Sky Swimmer
 * 
 */
public class ChatRoom {

	private String id;
	private String type;

	public ChatRoom(String id, String type) {
		this.id = id;
		this.type = type;
	}

	/**
	 * Retrieves the chat room ID
	 * 
	 * @return Chat room ID
	 */
	public String getID() {
		return id;
	}

	/**
	 * Retrieves the chat room type
	 * 
	 * @return Chat room type
	 */
	public String getType() {
		return type;
	}

}
