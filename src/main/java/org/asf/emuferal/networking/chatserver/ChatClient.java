package org.asf.emuferal.networking.chatserver;

import java.io.IOException;
import java.io.Reader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.networking.chatserver.packets.AbstractChatPacket;
import org.asf.emuferal.util.TaskThread;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class ChatClient {

	private Socket client;
	private ChatServer server;
	private JsonReader reader;
	private EmuFeralAccount player;
	private ArrayList<String> rooms = new ArrayList<String>();
	private HashMap<String, Boolean> privateChat = new HashMap<String, Boolean>();

	private TaskThread taskThread;

	/**
	 * Field for storing, eg. a player instance object
	 */
	public Object container;

	public ChatClient(Socket client, ChatServer server) {
		this.client = client;
		this.server = server;

		taskThread = new TaskThread(client.toString());
		taskThread.start();

		reader = new JsonReader(new Reader() {

			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				byte[] data = new byte[cbuf.length];
				int l = client.getInputStream().read(data);
				for (int i = 0; i < l; i++) {
					cbuf[off + i] = (char) data[i];
				}
				return l;
			}

			@Override
			public void close() throws IOException {
			}

		});
	}

	void stop() {
		taskThread.stopCleanly();
		rooms.clear();
		privateChat.clear();
	}

	// Client init
	void runClient() throws IOException {
		// Read initial packet
		JsonObject handshakeStart = readRawPacket();

		// Check validity
		if (!handshakeStart.get("cmd").getAsString().equals("sessions.start")) {
			// Invalid request, too early to send different packet
			disconnect();
			return;
		}

		// Parse payload
		String token = handshakeStart.get("auth_token").getAsString();

		// Verify signature
		String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
		String sig = token.split("\\.")[2];
		if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
			disconnect();
			return;
		}

		// Parse JWT
		JsonObject payload = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();

		// Locate account
		EmuFeralAccount acc = AccountManager.getInstance().getAccount(payload.get("uuid").getAsString());
		if (acc == null) {
			disconnect();
			return;
		}

		// Save account in memory
		player = acc;

		// Send success
		JsonObject res = new JsonObject();
		res.addProperty("eventId", "sessions.start");
		res.addProperty("success", true);
		sendPacket(res);
	}

	// Packet handling code
	void handle(JsonObject packet) {
		if (!handlePacket(packet)) {
			// Packet not found
			// Allow debug mode to re-register packets
			if (System.getProperty("debugMode") != null) {
				server.registry.clear();
				server.registerPackets();
			}

			System.err.println("Unhandled packet: client " + client + " sent: " + packet.toString());
		}
	}

	private boolean handlePacket(JsonObject packet) {
		// Find packet in registry
		for (AbstractChatPacket pkt : server.registry) {
			if (pkt.id().equals(packet.get("cmd").getAsString())) {
				// Found a compatible packet, instantiate it
				AbstractChatPacket res = pkt.instantiate();

				// Parse packet
				res.parse(packet);

				// Handle packet
				if (res.handle(this))
					return true; // Packet was handled, lets end the loop
			}
		}

		// Packet was not handled
		return false;
	}

	/**
	 * Retrieves the client socket
	 * 
	 * @return Socket instance
	 */
	public Socket getSocket() {
		return client;
	}

	/**
	 * Disconnects the client
	 */
	public void disconnect() {
		try {
			if (client != null)
				client.close();
		} catch (IOException e) {
		}
		stop();
		client = null;
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Raw packet to send
	 */
	public void sendPacket(JsonObject packet) {
		taskThread.schedule(() -> {
			try {
				// Send packet
				if (getSocket() == null)
					return;
				char[] d = packet.toString().toCharArray();
				for (char ch : d) {
					client.getOutputStream().write((byte) ch);
				}
				client.getOutputStream().write(0);
			} catch (Exception e) {
			}
		});
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Packet to send
	 */
	public void sendPacket(AbstractChatPacket packet) {
		JsonObject data = new JsonObject();
		data.addProperty("eventId", packet.id());
		packet.build(data);
		sendPacket(data);
	}

	/**
	 * Reads a single raw packet
	 * 
	 * @return JsonObject instance
	 * @throws IOException If reading fails
	 */
	public JsonObject readRawPacket() throws IOException {
		return JsonParser.parseReader(reader).getAsJsonObject();
	}

	/**
	 * Retrieves the server object
	 * 
	 * @return ChatServer instance
	 */
	public ChatServer getServer() {
		return server;
	}

	/**
	 * Checks if the client is still connected
	 * 
	 * @return True if connected, false otherwise
	 */
	public boolean isConnected() {
		return client != null;
	}

	/**
	 * Retrieves the connected player object
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getPlayer() {
		return player;
	}

	/**
	 * Checks if the client is in a specific chat room
	 * 
	 * @param room Chat room to check
	 * @return True if the client is in the specified room, false otherwise
	 */
	public boolean isInRoom(String room) {
		ArrayList<String> rooms;

		while (true) {
			try {
				rooms = new ArrayList<String>(this.rooms);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		if (rooms.contains(room))
			return true; // Player is part of this chat room

		return false; // Player is not part of this chat room
	}

	/**
	 * Checks if a room is private or not
	 * 
	 * @param room Room ID
	 * @return True if private, false otherwise
	 */
	public boolean isRoomPrivate(String room) {
		while (true) {
			try {
				return privateChat.getOrDefault(room, false);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	/**
	 * Leaves a chat room
	 * 
	 * @param room Room to leave
	 */
	public void leaveRoom(String room) {
		while (true) {
			try {
				if (rooms.contains(room)) {
					rooms.remove(room);
					privateChat.remove(room);
				}
				break;
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	/**
	 * Joins a chat room
	 * 
	 * @param room      Room to join
	 * @param isPrivate True if the room is a private room, false otherwise
	 */
	public void joinRoom(String room, boolean isPrivate) {
		while (true) {
			try {
				if (!rooms.contains(room)) {
					rooms.add(room);
					privateChat.put(room, isPrivate);
				}
				break;
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	/**
	 * Retrieves an array of all chat rooms
	 * 
	 * @return Array of chat room IDs
	 */
	public String[] getRooms() {
		ArrayList<String> rooms;

		while (true) {
			try {
				rooms = new ArrayList<String>(this.rooms);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		return rooms.toArray(t -> new String[t]);
	}

}
