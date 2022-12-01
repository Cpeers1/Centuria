package org.asf.centuria.networking.chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.asf.centuria.Centuria;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.dms.PrivateChatMessage;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.servers.ChatServerStartupEvent;
import org.asf.centuria.networking.chatserver.networking.AbstractChatPacket;
import org.asf.centuria.networking.chatserver.networking.CreateConversationPacket;
import org.asf.centuria.networking.chatserver.networking.GetConversation;
import org.asf.centuria.networking.chatserver.networking.HistoryPacket;
import org.asf.centuria.networking.chatserver.networking.JoinRoomPacket;
import org.asf.centuria.networking.chatserver.networking.OpenDMPacket;
import org.asf.centuria.networking.chatserver.networking.PingPacket;
import org.asf.centuria.networking.chatserver.networking.SendMessage;
import org.asf.centuria.networking.chatserver.networking.UserConversations;

import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

public class ChatServer {

	private ServerSocket server;
	private ArrayList<ChatClient> clients = new ArrayList<ChatClient>();
	ArrayList<AbstractChatPacket> registry = new ArrayList<AbstractChatPacket>();

	public ChatServer(ServerSocket socket) {
		server = socket;
		registerPackets();
	}

	protected void registerPackets() {
		// Packet registry
		registerPacket(new PingPacket());
		registerPacket(new JoinRoomPacket());
		registerPacket(new UserConversations());
		registerPacket(new GetConversation());
		registerPacket(new HistoryPacket());
		registerPacket(new SendMessage());
		registerPacket(new OpenDMPacket());
		registerPacket(new CreateConversationPacket());

		// Allow modules to register packets
		ChatServerStartupEvent ev = new ChatServerStartupEvent(this, t -> registerPacket(t));
		EventBus.getInstance().dispatchEvent(ev);
	}

	public ChatClient[] getClients() {
		while (true) {
			try {
				ArrayList<ChatClient> clients = new ArrayList<ChatClient>(this.clients);
				var cls = clients;
				for (ChatClient cl : cls) {
					if (cl == null || cl.getPlayer() == null) {
						clients.remove(cl);
					}
					if (cl == null) {
						this.clients.remove(cl);
					}
				}
				return clients.toArray(t -> new ChatClient[t]);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	/**
	 * Runs the server
	 */
	public void start() {
		Thread serverProcessor = new Thread(() -> {
			// Server loop
			while (server != null) {
				try {
					Socket client = server.accept();
					runClient(client);
				} catch (IOException ex) {
					server = null;
					break;
				}
			}
		}, "Chat Server Thread: " + this.getClass().getSimpleName());
		serverProcessor.setDaemon(true);
		serverProcessor.start();
	}

	// Adds packets to the registry
	protected void registerPacket(AbstractChatPacket packet) {
		registry.add(packet);
	}

	// Client system
	private void runClient(Socket clientSocket) throws IOException {
		ChatClient client = new ChatClient(clientSocket, this);

		// Start the client thread
		Thread th = new Thread(() -> {
			try {
				// Run start code
				client.runClient();

				// Add client
				if (client.isConnected())
					clients.add(client);

				// Client loop
				while (client.getSocket() != null) {
					JsonObject obj;
					try {
						obj = client.readRawPacket();
					} catch (IllegalArgumentException | JsonIOException e) {
						throw new IOException("Read failure");
					}

					try {
						client.handle(obj);
					} catch (Exception e) {
					}
				}

				// Remove client
				if (clients.contains(client)) {
					Centuria.logger.info(
							"Player " + client.getPlayer().getDisplayName() + " disconnected from the chat server.");
					clients.remove(client);
				}

				// Mark disconnected
				if (client.isConnected())
					client.disconnect();
			} catch (Exception e) {
				// Close connection
				try {
					if (client.getSocket() != null)
						client.getSocket().close();
				} catch (IOException e2) {
				}

				// Remove client
				if (clients.contains(client)) {
					Centuria.logger.info(
							"Player " + client.getPlayer().getDisplayName() + " disconnected from the chat server.");
					clients.remove(client);
				}

				// Log disconnect
				if (!(e instanceof IOException) && !(e instanceof SocketException)) {
					System.err.println("Chat connection died! Error: " + e.getClass().getName()
							+ (e.getMessage() != null ? ": " + e.getMessage() : ""));
					e.printStackTrace();
				}

				// Mark disconnected
				if (client.isConnected())
					client.disconnect();
			}
		}, "Chat Client Thread: " + client);
		th.setDaemon(true);
		th.start();
	}

	/**
	 * Stops the server
	 */
	public void stop() {
		try {
			server.close();
		} catch (IOException e) {
		}
		server = null;
	}

	/**
	 * Retrieves the server socket
	 * 
	 * @return ServerSocket instance or null
	 */
	public ServerSocket getServerSocket() {
		return server;
	}

	/**
	 * Generates a room info object
	 * 
	 * @param room      Room ID
	 * @param isPrivate True if the room is private, false otherwise
	 * @param requester Player making the request
	 * @return JsonObject instance
	 */
	public JsonObject roomObject(String room, boolean isPrivate, String requester) {
		// Build object
		JsonObject roomData = new JsonObject();
		roomData.addProperty("conversation_id", room);
		roomData.addProperty("title", "");

		// Load DM manager
		DMManager manager = DMManager.getInstance();

		// Check type and validity
		if (!isPrivate || !manager.dmExists(room)) {
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
			for (String participant : manager.getDMParticipants(room)) {
				members.add(participant);
			}
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

		roomData.addProperty("conversationType", isPrivate ? "private" : "room");
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

}
