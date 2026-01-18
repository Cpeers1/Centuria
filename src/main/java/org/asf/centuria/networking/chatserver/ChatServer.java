package org.asf.centuria.networking.chatserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.stream.Stream;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.dms.PrivateChatMessage;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.servers.ChatServerStartupEvent;
import org.asf.centuria.networking.chatserver.networking.CreateConversationPacket;
import org.asf.centuria.networking.chatserver.networking.GetConversation;
import org.asf.centuria.networking.chatserver.networking.HistoryPacket;
import org.asf.centuria.networking.chatserver.networking.JoinRoomPacket;
import org.asf.centuria.networking.chatserver.networking.OpenDMPacket;
import org.asf.centuria.networking.chatserver.networking.PingPacket;
import org.asf.centuria.networking.chatserver.networking.SendMessage;
import org.asf.centuria.networking.chatserver.networking.UserConversations;
import org.asf.centuria.networking.chatserver.networking.moderator.GetChatRoomList;
import org.asf.centuria.networking.chatserver.networking.moderator.GetPlayerList;
import org.asf.centuria.networking.chatserver.networking.moderator.InitModeratorClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoom;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.asf.centuria.networking.persistentservice.BasePersistentServiceServer;

public class ChatServer extends BasePersistentServiceServer<ChatClient, ChatServer> {

	private HashMap<String, ChatRoom> rooms = new HashMap<String, ChatRoom>();

	public ChatServer(ServerSocket socket) {
		super(socket, ChatClient.class);
		rooms.put("SYSTEM", new ChatRoom(true, "private", "SYSTEM", this));
	}

	ChatRoom joinRoom(String type, String id) {
		synchronized (rooms) {
			if (!rooms.containsKey(id)) {
				ChatRoom room = new ChatRoom(false, type, id, this);
				rooms.put(id, room);
				return room;
			} else {
				return rooms.get(id);
			}
		}
	}

	/**
	 * Creates a permanent chat room
	 * 
	 * @param id   Chat room ID
	 * @param type Chat room type
	 * @return ChatRoom instance
	 */
	public ChatRoom createPermanentRoom(String id, String type) {
		synchronized (rooms) {
			if (rooms.containsKey(id)) {
				return rooms.get(id);
			}

			ChatRoom room = new ChatRoom(true, type, id, this);
			rooms.put(id, room);
			return room;
		}
	}

	void leaveRoom(String id) {
		synchronized (rooms) {
			if (rooms.containsKey(id)) {
				ChatRoom room = rooms.get(id);

				// Check players in room
				if (room.getConnectedClients().length == 0 && !room.shouldRetainIfEmpty()) {
					// Remove
					rooms.remove(id);
				}
			}
		}
	}

	@Override
	protected void registerPackets() {
		// Allow modules to register packets and to override existing packets
		ChatServerStartupEvent ev = new ChatServerStartupEvent(this, t -> registerPacket(t));
		EventBus.getInstance().dispatchEvent(ev);

		// Packet registry
		registerPacket(new PingPacket());
		registerPacket(new JoinRoomPacket());
		registerPacket(new UserConversations());
		registerPacket(new GetConversation());
		registerPacket(new HistoryPacket());
		registerPacket(new SendMessage());
		registerPacket(new OpenDMPacket());
		registerPacket(new CreateConversationPacket());
		registerPacket(new GetPlayerList());
		registerPacket(new InitModeratorClient());
		registerPacket(new GetChatRoomList());
	}

	/**
	 * Generates a room info object
	 * 
	 * @param room      Room ID
	 * @param type      Room type
	 * @param requester Player making the request
	 * @return JsonObject instance
	 */
	public JsonObject roomObject(String room, String type, String requester) {
		// Build object
		JsonObject roomData = new JsonObject();
		roomData.addProperty("conversation_id", room);
		roomData.addProperty("title", "");

		// Load DM manager
		DMManager manager = DMManager.getInstance();

		// Check type and validity
		if (type.equalsIgnoreCase(ChatRoomTypes.ROOM_CHAT) || !manager.dmExists(room)) {
			// Build participants object
			JsonArray members = new JsonArray();
			for (ChatClient cl : getClients()) {
				if (cl.isInRoom(room))
					members.add(cl.getPlayer().getAccountID());
			}
			roomData.add("participants", members);
		} else {
			// Build participants object
			JsonArray members = new JsonArray();
			String[] participants = manager.getDMParticipants(room);
			if (!Stream.of(participants).anyMatch(t -> t.equalsIgnoreCase(requester)))
				return null;
			for (String participant : participants) {
				if (participant.startsWith("plaintext:")
						|| AccountManager.getInstance().getAccount(participant) != null)
					members.add(participant);
			}

			// if its only one, the client will bug, bc if its only one its likely only the
			// person thats requesting the dm
			if (members.size() <= 1)
				return null;
			roomData.add("participants", members);

			// Find recent message
			PrivateChatMessage[] msgs = manager.getDMHistory(room, requester);
			if (msgs.length != 0) {
				// Add most recent
				PrivateChatMessage recent = msgs[msgs.length - 1];
				JsonObject msg = new JsonObject();
				msg.addProperty("body", recent.content);
				msg.addProperty("sent_at", recent.sentAt);
				msg.addProperty("source", recent.source);
				roomData.add("recent_message", msg);
			}
		}

		roomData.addProperty("conversationType", type);
		return roomData;
	}

	/**
	 * Retrieves chat clients by ID
	 * 
	 * @param accountID Account ID
	 * @return ChatClient instance or null
	 */
	public ChatClient getClient(String accountID) {
		for (ChatClient cl : getClients())
			if (cl.getPlayer().getAccountID().equals(accountID))
				return cl;
		return null;
	}

	/**
	 * Backported variant of start() for old server modules
	 */
	public void start() {
		super.start();
	}

	/**
	 * Backported variant of stop() for old server modules
	 */
	public void stop() {
		super.stop();
	}

	/**
	 * Backported variant of getServerSocket() for old server modules
	 * 
	 * @return ServerSocket instance
	 */
	public ServerSocket getServerSocket() {
		return super.getServerSocket();
	}

	/**
	 * Backported variant of getClients() for old server modules
	 * 
	 * @return Array of ChatClient instances
	 */
	public ChatClient[] getClients() {
		return super.getClients();
	}

	@Override
	protected ChatClient createClient(Socket clientSocket) {
		return new ChatClient(clientSocket, this);
	}

	@Override
	protected void logDisconnect(ChatClient client) {
		Centuria.logger.info("Player " + client.getPlayer().getDisplayName() + " disconnected from the chat server.");
	}

	/**
	 * Retrieves an array of all chat rooms
	 * 
	 * @return Array of chat room instances
	 */
	public ChatRoom[] getRoomInstances() {
		synchronized (rooms) {
			return rooms.values().toArray(t -> new ChatRoom[t]);
		}
	}

	/**
	 * Retrieves chat rooms by ID
	 * 
	 * @param id Room ID
	 * @return ChatRoom instance or null
	 */
	public ChatRoom getRoom(String id) {
		synchronized (rooms) {
			return rooms.get(id);
		}
	}

}