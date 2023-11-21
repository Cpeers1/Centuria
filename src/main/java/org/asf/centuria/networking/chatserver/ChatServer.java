package org.asf.centuria.networking.chatserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
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
import org.asf.centuria.networking.chatserver.networking.moderator.InitModeratorClient;
import org.asf.centuria.networking.chatserver.networking.moderator.GetChatRoomList;
import org.asf.centuria.networking.chatserver.networking.moderator.GetPlayerList;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.persistentservice.BasePersistentServiceServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ChatServer extends BasePersistentServiceServer<ChatClient, ChatServer> {

	public ChatServer(ServerSocket socket) {
		super(socket, ChatClient.class);
	}

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

			// Time format
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

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
				msg.addProperty("sent_at", fmt.format(new Date(recent.sentAt)));
				msg.addProperty("source", recent.source);
				roomData.add("recent_message", msg);
			}
		}

		// Add type
		roomData.addProperty("conversationType", type);

		// Return
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

	@Override
	protected ChatClient createClient(Socket clientSocket) {
		return new ChatClient(clientSocket, this);
	}

	@Override
	protected void logDisconnect(ChatClient client) {
		Centuria.logger.info("Player " + client.getPlayer().getDisplayName() + " disconnected from the chat server.");
	}

}
