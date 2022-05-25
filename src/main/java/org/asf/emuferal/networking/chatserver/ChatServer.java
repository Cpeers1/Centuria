package org.asf.emuferal.networking.chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.asf.emuferal.networking.chatserver.packets.AbstractChatPacket;
import org.asf.emuferal.networking.chatserver.packets.ConvoHistoryDummy;
import org.asf.emuferal.networking.chatserver.packets.GetConversation;
import org.asf.emuferal.networking.chatserver.packets.JoinRoomPacket;
import org.asf.emuferal.networking.chatserver.packets.PingPacket;
import org.asf.emuferal.networking.chatserver.packets.SendMessage;
import org.asf.emuferal.networking.chatserver.packets.UserConversations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ChatServer {

	private ServerSocket server;
	private ArrayList<ChatClient> clients = new ArrayList<ChatClient>();
	ArrayList<AbstractChatPacket> registry = new ArrayList<AbstractChatPacket>();

	public ChatServer(ServerSocket socket) {
		server = socket;
		registerPackets();
	}

	void registerPackets() {
		// Packet registry
		registerPacket(new PingPacket());
		registerPacket(new JoinRoomPacket());
		registerPacket(new UserConversations());
		registerPacket(new GetConversation());
		registerPacket(new ConvoHistoryDummy());
		registerPacket(new SendMessage());
	}

	public ChatClient[] getClients() {
		while (true) {
			try {
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
	private void registerPacket(AbstractChatPacket packet) {
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
					try {
						client.handle(client.readRawPacket());
					} catch (Exception e) {
						if (e instanceof IOException || e instanceof IllegalArgumentException)
							throw e;
					}
				}

				// Remove client
				if (clients.contains(client))
					clients.remove(client);
				client.stop();
			} catch (Exception e) {
				// Close connection
				try {
					if (client.getSocket() != null)
						client.getSocket().close();
				} catch (IOException e2) {
				}

				// Remove client
				if (clients.contains(client))
					clients.remove(client);
				client.stop();
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
	 * @return JsonObject instance
	 */
	public JsonObject roomObject(String room, Boolean isPrivate) {
		// Build object
		JsonObject roomData = new JsonObject();
		roomData.addProperty("conversation_id", room);
		roomData.addProperty("title", "Room");
		// Build participants object
		JsonArray members = new JsonArray();
		for (ChatClient cl : getClients()) {
			if (cl.isInRoom(room))
				members.add(cl.getPlayer().getAccountID());
		}
		roomData.add("participants", members);
		roomData.addProperty("conversationType", isPrivate ? "private" : "room");
		return roomData;
	}

}
