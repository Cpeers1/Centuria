package org.asf.centuria.networking.chatserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.chat.ChatLoginEvent;
import org.asf.centuria.networking.chatserver.networking.moderator.ModeratorClient;
import org.asf.centuria.networking.chatserver.proxies.OcProxyInfo;
import org.asf.centuria.networking.chatserver.rooms.ChatRoom;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.persistentservice.BasePersistentServiceClient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChatClient extends BasePersistentServiceClient<ChatClient, ChatServer> {

	private CenturiaAccount player;
	private HashMap<String, ChatRoom> rooms = new HashMap<String, ChatRoom>();

	// Anti-hack
	public int banCounter = 0;

	// Room lock
	public boolean isReady = false;

	private ArrayList<OcProxyMetadata> proxies = new ArrayList<OcProxyMetadata>();

	public static class OcProxyMetadata {
		public String name;
		public String prefix;
		public String suffix;
	}

	/**
	 * Retrieves all OC proxy metadata
	 * 
	 * @return Array of OcProxyMetadata instances
	 */
	public OcProxyMetadata[] getOcProxyMetadata() {
		synchronized (proxies) {
			return proxies.toArray(t -> new OcProxyMetadata[t]);
		}
	}

	/**
	 * Reloads all OC proxies
	 */
	public void reloadProxies() {
		// Reload
		synchronized (proxies) {
			// Clear
			proxies.clear();

			// Retrieve all
			for (OcProxyInfo proxy : OcProxyInfo.allOfUser(getPlayer())) {
				OcProxyMetadata d = new OcProxyMetadata();
				d.name = proxy.displayName;
				d.prefix = proxy.triggerPrefix;
				d.suffix = proxy.triggerSuffix;
				proxies.add(d);
			}
		}
	}

	public ChatClient(Socket client, ChatServer server) {
		super(client, server);

		Thread th = new Thread(() -> {
			while (isConnected()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				banCounter = 0;
			}
		}, "Anti-hack thread: " + client);
		th.setDaemon(true);
		th.start();
	}

	@Override
	protected void stop() {
		synchronized (rooms) {
			rooms.clear();
		}

		// Send to moderator clients
		for (ChatClient client : getServer().getClients()) {
			if (client.getObject(ModeratorClient.class) == null)
				continue;

			// Check moderator perms
			String permLevel = "member";
			if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
				permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
						.get("permissionLevel").getAsString();
			}
			if (!GameServer.hasPerm(permLevel, "moderator"))
				continue;

			// Send packet
			JsonObject response = new JsonObject();
			response.addProperty("eventId", "centuria.moderatorclient.playerdisconnected");
			response.addProperty("success", true);
			response.addProperty("uuid", getPlayer().getAccountID());
			client.sendPacket(response);
		}
	}

	@Override
	protected void runClient() throws IOException {
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
		if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
			disconnect();
			return;
		}

		// Verify expiry
		JsonObject jwtPl = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();
		if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
			disconnect();
			return;
		}

		// Parse JWT
		JsonObject payload = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();

		// Verify access
		if (!payload.has("acs") || !payload.get("acs").getAsString().equals("gameplay")) {
			disconnect();
			return;
		}

		// Locate account
		CenturiaAccount acc = AccountManager.getInstance().getAccount(payload.get("uuid").getAsString());
		if (acc == null) {
			disconnect();
			return;
		}

		// Rename thread
		Thread.currentThread()
				.setName("Chat Client Thread: " + acc.getDisplayName() + " [ID " + acc.getAccountID() + ", Address "
						+ ((InetSocketAddress) getSocket().getRemoteSocketAddress()).getAddress().getHostAddress()
						+ "]");

		// Load DMs into memory
		if (acc.getSaveSharedInventory().containsItem("dms")) {
			// Load and sanitize dms
			JsonObject dms = acc.getSaveSharedInventory().getItem("dms").getAsJsonObject();
			ArrayList<String> toRemove = new ArrayList<String>();
			for (String user : dms.keySet()) {
				// Clean DM participants
				String dmID = dms.get(user).getAsString();
				int participantC = 0;
				if (DMManager.getInstance().dmExists(dmID)) {
					String[] participants = DMManager.getInstance().getDMParticipants(dmID);
					participantC = participants.length;
					for (String participant : participants) {
						if (!participant.startsWith("plaintext:")) {
							// Check account
							if (AccountManager.getInstance().getAccount(participant) == null) {
								participantC--;
								DMManager.getInstance().removeParticipant(dmID, participant);
							}
						}
					}
				}

				// Check validity
				if (AccountManager.getInstance().getAccount(user) == null || participantC <= 1) {
					toRemove.add(user);
					continue;
				}

				// Join room
				joinRoom(dms.get(user).getAsString(), ChatRoomTypes.PRIVATE_CHAT);
			}

			// Remove nonexistent and invalid dms
			for (String user : toRemove) {
				if (DMManager.getInstance().dmExists(dms.get(user).getAsString()))
					DMManager.getInstance().deleteDM(dms.get(user).getAsString());
				dms.remove(user);
			}

			// Save if needed
			if (toRemove.size() != 0)
				acc.getSaveSharedInventory().setItem("dms", dms);
		}

		// Remove sensitive info and fire event
		handshakeStart.remove("auth_token");
		ChatLoginEvent evt = new ChatLoginEvent(getServer(), acc, this, handshakeStart);
		EventBus.getInstance().dispatchEvent(evt);
		if (evt.isCancelled()) {
			disconnect(); // Cancelled
			return;
		}

		// Check moderator perms
		String permLevel = "member";
		if (acc.getSaveSharedInventory().containsItem("permissions")) {
			permLevel = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject().get("permissionLevel")
					.getAsString();
		}

		// Check maintenance mode
		if (Centuria.gameServer.maintenance) {
			boolean lockout = true;

			// Check permissions
			if (GameServer.hasPerm(permLevel, "admin")) {
				lockout = false;
			}

			if (lockout || Centuria.gameServer.shutdown) {
				disconnect();
				return;
			}
		}

		// Check bans
		if (acc.isBanned()) {
			disconnect();
			return;
		}

		// Check ip ban
		InetSocketAddress ip = (InetSocketAddress) getSocket().getRemoteSocketAddress();
		InetAddress addr = ip.getAddress();
		String ipaddr = addr.getHostAddress();
		if (GameServer.isIPBanned(ipaddr, acc, Centuria.gameServer.vpnIpsV4, Centuria.gameServer.vpnIpsV6,
				Centuria.gameServer.whitelistFile)) {
			disconnect();
			return;
		}

		// Check if the player is ingame
		Player plr = acc.getOnlinePlayerInstance();
		if (plr != null) {
			// Check if the player was in chat
			if (plr.wasInChat && plr.room != null)
				joinRoom(plr.room, ChatRoomTypes.ROOM_CHAT);
		} else {
			// Security checks
			// Check moderator perms
			if (!GameServer.hasPerm(permLevel, "moderator")) {
				// No access to chat when offline if not a mod
				disconnect();
				return;
			}
		}

		// Log the login attempt
		Centuria.logger.info("Chat login from IP: " + getSocket().getRemoteSocketAddress() + ": " + acc.getLoginName());

		// Save account in memory
		player = acc;
		Centuria.logger.info("Player " + getPlayer().getDisplayName() + " connected to the chat server.");

		// Send success
		JsonObject res = new JsonObject();
		res.addProperty("eventId", "sessions.start");
		res.addProperty("success", true);
		sendPacket(res);

		// Reload proxies
		reloadProxies();
	}

	/**
	 * Retrieves the connected player object
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getPlayer() {
		return player;
	}

	/**
	 * Checks if the client is in a specific chat room
	 * 
	 * @param room Chat room to check
	 * @return True if the client is in the specified room, false otherwise
	 */
	public boolean isInRoom(String room) {
		synchronized (rooms) {
			if (rooms.containsKey(room))
				return true; // Player is part of this chat room
			return false; // Player is not part of this chat room
		}
	}

	/**
	 * Leaves a chat room
	 * 
	 * @param room Room to leave
	 */
	public void leaveRoom(String room) {
		boolean left = false;
		String oldType = null;
		synchronized (rooms) {
			if (rooms.containsKey(room)) {
				oldType = rooms.get(room).getType();
				rooms.remove(room);
				left = true;
			}
		}
		if (left && !oldType.equals(ChatRoomTypes.PRIVATE_CHAT)) {
			// Send to moderator clients
			for (ChatClient client : getServer().getClients()) {
				if (client.getObject(ModeratorClient.class) == null)
					continue;

				// Check moderator perms
				String permLevel = "member";
				if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
					permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator"))
					continue;

				// Send packet
				JsonObject response = new JsonObject();
				response.addProperty("eventId", "centuria.moderatorclient.playerleftroom");
				response.addProperty("success", true);
				response.addProperty("conversationId", room);
				response.addProperty("conversationType", oldType);
				response.addProperty("uuid", getPlayer().getAccountID());
				client.sendPacket(response);
			}
		}
	}

	/**
	 * Joins a chat room
	 * 
	 * @param roomID Room ID to join
	 * @param type   Room type
	 */
	public void joinRoom(String roomID, String type) {
		boolean joined = false;
		synchronized (rooms) {
			if (!rooms.containsKey(roomID)) {
				rooms.put(roomID, new ChatRoom(roomID, type));
				joined = true;
			}
		}
		if (joined && !type.equals(ChatRoomTypes.PRIVATE_CHAT)) {
			// Send to moderator clients
			for (ChatClient client : getServer().getClients()) {
				if (client.getObject(ModeratorClient.class) == null)
					continue;

				// Check moderator perms
				String permLevel = "member";
				if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
					permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator"))
					continue;

				// Send packet
				JsonObject response = new JsonObject();
				response.addProperty("eventId", "centuria.moderatorclient.playerjoinedroom");
				response.addProperty("success", true);
				response.addProperty("conversationId", roomID);
				response.addProperty("conversationType", type);
				response.addProperty("uuid", getPlayer().getAccountID());
				client.sendPacket(response);
			}
		}
	}

	/**
	 * Retrieves an array of all chat rooms
	 * 
	 * @return Array of chat room IDs
	 */
	public String[] getRooms() {
		synchronized (rooms) {
			return rooms.keySet().toArray(t -> new String[t]);
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
