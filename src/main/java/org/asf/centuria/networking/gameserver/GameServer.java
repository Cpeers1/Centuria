package org.asf.centuria.networking.gameserver;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Random;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.players.OnlineStatus;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.ipbans.IpBanManager;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.accounts.AccountLoginEvent;
import org.asf.centuria.modules.events.maintenance.MaintenanceEndEvent;
import org.asf.centuria.modules.events.players.PlayerJoinEvent;
import org.asf.centuria.modules.events.players.PlayerLeaveEvent;
import org.asf.centuria.modules.events.servers.GameServerStartupEvent;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.smartfox.BaseSmartfoxServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;
import org.asf.centuria.packets.xml.handshake.auth.ClientToServerAuthPacket;
import org.asf.centuria.packets.xml.handshake.version.ClientToServerHandshake;
import org.asf.centuria.packets.xml.handshake.version.ServerToClientOK;
import org.asf.centuria.packets.xt.*;
import org.asf.centuria.packets.xt.gameserver.*;
import org.asf.centuria.packets.xt.gameserver.avatareditor.*;
import org.asf.centuria.packets.xt.gameserver.interactions.*;
import org.asf.centuria.packets.xt.gameserver.inventory.*;
import org.asf.centuria.packets.xt.gameserver.minigames.*;
import org.asf.centuria.packets.xt.gameserver.players.*;
import org.asf.centuria.packets.xt.gameserver.room.*;
import org.asf.centuria.packets.xt.gameserver.sanctuaries.*;
import org.asf.centuria.packets.xt.gameserver.settings.*;
import org.asf.centuria.packets.xt.gameserver.shops.*;
import org.asf.centuria.packets.xt.gameserver.social.*;
import org.asf.centuria.packets.xt.gameserver.trade.*;
import org.asf.centuria.packets.xt.gameserver.world.*;
import org.asf.centuria.security.AddressChecker;
import org.asf.centuria.security.IpAddressMatcher;
import org.asf.centuria.social.SocialEntry;
import org.asf.centuria.social.SocialManager;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameServer extends BaseSmartfoxServer {

	public GameServer(ServerSocket socket) {
		super(socket);
	}

	public boolean maintenance = false;
	public boolean shutdown = false;
	private Random rnd = new Random();
	private XmlMapper mapper = new XmlMapper();
	private HashMap<String, Player> players = new HashMap<String, Player>();

	public ArrayList<String> vpnIpsV4 = new ArrayList<String>();
	public ArrayList<String> vpnIpsV6 = new ArrayList<String>();

	public String whitelistFile = null;

	public Player[] getPlayers() {
		while (true) {
			try {
				return players.values().toArray(t -> new Player[t]);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	@Override
	protected void registerPackets() {
		mapper = new XmlMapper();

		// Handshake
		registerPacket(new ClientToServerHandshake(mapper));
		registerPacket(new ClientToServerAuthPacket(mapper));

		// Game
		registerPacket(new KeepAlive());
		registerPacket(new PrefixedPacket());
		registerPacket(new InventoryItemDownloadPacket());
		registerPacket(new InventoryItemUseDye());
		registerPacket(new TradeListUpdatePacket());
		registerPacket(new RoomJoin());
		registerPacket(new RoomJoinPrevious());
		registerPacket(new RoomJoinTutorial());
		registerPacket(new MinigameJoin());
		registerPacket(new MinigameMessage());
		registerPacket(new ShopList());
		registerPacket(new ShopItemBuy());
		registerPacket(new ItemUncraftPacket());
		registerPacket(new WorldReadyPacket());
		registerPacket(new WorldObjectUpdatePacket());
		registerPacket(new WorldObjectRespawnRequestPacket());
		registerPacket(new WorldObjectRespawnSetPacket());
		registerPacket(new WorldObjectGlidePacket());
		registerPacket(new AvatarLookGet());
		registerPacket(new PlayerOnlineStatus());
		registerPacket(new JumpToPlayer());
		registerPacket(new FindPlayer());
		registerPacket(new AvatarAction());
		registerPacket(new WorldObjectActionStartPacket());
		registerPacket(new WorldObjectActionCancelPacket());
		registerPacket(new WorldObjectActionFinishPacket());
		registerPacket(new WorldObjectAskResponsePacket());
		registerPacket(new UserTutorialCompleted());
		registerPacket(new AvatarEditorSelectLook());
		registerPacket(new UserAvatarSave());
		registerPacket(new SanctuaryJoinPacket());
		registerPacket(new SanctuaryLookSwitchPacket());
		registerPacket(new SanctuaryLookLoadPacket());
		registerPacket(new SanctuaryUpdatePacket());
		registerPacket(new SanctuaryLookSavePacket());
		registerPacket(new SanctuaryUpgradeStartPacket());
		registerPacket(new SanctuaryUpgradeCompletePacket());
		registerPacket(new UserVarSetPacket());
		registerPacket(new InventoryItemInspirationCombinePacket());
		
		// Trading Packets
		registerPacket(new TradeListPacket());
		registerPacket(new TradeInitiatePacket());
		registerPacket(new TradeInitiateCancelPacket());
		registerPacket(new TradeInitiateFailPacket());
		registerPacket(new TradeInitiateRejectPacket());
		registerPacket(new TradeInitiateAcceptPacket());
		registerPacket(new TradeExitPacket());
		registerPacket(new TradeAddRemoveItemPacket());
		registerPacket(new TradeReadyPacket());
		registerPacket(new TradeReadyRejectPacket());
		registerPacket(new TradeReadyAcceptPacket());
		
		// Allow modules to register packets
		GameServerStartupEvent ev = new GameServerStartupEvent(this, t -> registerPacket(t));
		EventBus.getInstance().dispatchEvent(ev);
	}

	@Override
	protected void startClient(SmartfoxClient client) throws IOException {
		// Read first handshake packet
		ClientToServerHandshake pk = client.readPacket(ClientToServerHandshake.class);

		// Check version
		boolean badClient = false;
		if (!pk.actionField.equals("verChk") || !pk.clientBuild.equals("165")) {
			badClient = true; // Ok, bad version, lets make sure the client disconnects
		}

		// Send response so that the client moves on
		client.sendPacket(new ServerToClientOK(mapper));

		// Random key
		client.readPacket(ISmartfoxPacket.class);
		String key = Long.toString(rnd.nextLong(), 16);
		client.sendPacket("<msg t='sys'><body action='rndK' r='-1'><k>" + key + "</k></body></msg>");

		// Authenticate the player
		ClientToServerAuthPacket auth = client.readPacket(ClientToServerAuthPacket.class);

		// Load token
		String token = auth.pword;

		// Verify signature
		String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
		String sig = token.split("\\.")[2];
		if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
			client.disconnect();
			return;
		}

		// Verify expiry
		JsonObject jwtPl = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();
		if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
			client.disconnect();
			return;
		}

		// Parse JWT
		JsonObject payload = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();

		// Locate account
		CenturiaAccount acc = AccountManager.getInstance().getAccount(payload.get("uuid").getAsString());
		if (acc == null) {
			client.disconnect();
			return;
		}

		// If under maintenance, send error
		if (maintenance) {
			boolean lockout = true;

			// Check permissions
			if (acc.getPlayerInventory().containsItem("permissions")) {
				String permLevel = acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
						.get("permissionLevel").getAsString();
				if (hasPerm(permLevel, "moderator")) {
					lockout = false;
				}
			}

			if (lockout || shutdown) {
				sendLoginResponse(client, auth, acc, 
				-16, 
				0);

				client.disconnect();
				return;
			}
		}

		// If the client is out of date, send error
		if (badClient) {
			sendLoginResponse(client, auth, acc, 
			-24, 
			0);

			client.disconnect();
			return;
		}

		// Check ban
		if (isBanned(acc)) {
			System.out.println("User '" + acc.getDisplayName() + "' could not connect: user is banned.");

			// Disconnect with error
			sendLoginResponse(client, auth, acc,
			-15,
			0);

			client.disconnect();
			return;
		}

		// Check ip ban
		if (isIPBanned(client.getSocket(), acc, vpnIpsV4, vpnIpsV6, whitelistFile)) {
			System.out.println("User '" + acc.getDisplayName() + "' could not connect: user is IP or VPN banned.");

			// Disconnect silently
			sendLoginResponse(client, auth, acc, 
			1, 
			0);
			
			client.disconnect();
			return;
		}

		// Disconnect an already connected instance
		Player ePlr = getPlayer(acc.getAccountID());
		if (ePlr != null)
			ePlr.client.disconnect();

		// Log the login attempt
		System.out.println("Login from IP: " + client.getSocket().getRemoteSocketAddress() + ": " + acc.getLoginName());

		// Run module handshake code
		AccountLoginEvent ev = new AccountLoginEvent(this, acc, client);
		EventBus.getInstance().dispatchEvent(ev);
		if (ev.isHandled() && ev.getStatus() != 1) {
			sendLoginResponse(client, auth, acc, 
			ev.getStatus(), 
			0);
			
			System.out.println("Login failure: " + acc.getLoginName() + ": module terminated login process with code "
					+ ev.getStatus());
			client.disconnect();
			return;
		}

		// Build Player object
		Player plr = new Player();
		plr.client = client;
		plr.account = acc;
		plr.activeLook = acc.getActiveLook();
		plr.activeSanctuaryLook = acc.getActiveSanctuaryLook();

		// Update login timestamp
		acc.login();

		// Save player in the client object
		client.container = plr;

		// Send response
		sendLoginResponse(client, auth, acc, 
		1, 
		plr.account.isPlayerNew() ? 2 : 3);

		// Initial login
		System.out.println(
				"Player connected: " + plr.account.getLoginName() + " (as " + plr.account.getDisplayName() + ")");
		sendPacket(client, "%xt%ulc%-1%");

		// Notify followers
		if (SocialManager.getInstance().socialListExists(plr.account.getAccountID())) {
			// Find all followers
			for (SocialEntry ent : SocialManager.getInstance().getFollowerPlayers(plr.account.getAccountID())) {
				// Send online status update
				Player player = getPlayer(ent.playerID);
				if (player != null) {
					RelationshipFollowOnlineStatusUpdate res = new RelationshipFollowOnlineStatusUpdate();
					res.userUUID = plr.account.getAccountID();
					res.playerOnlineStatus = OnlineStatus.LoggedInToRoom;
					client.sendPacket(res);
				}
			}
		}

		// Add player
		players.put(plr.account.getAccountID(), plr);

		// Dispatch join event
		EventBus.getInstance().dispatchEvent(new PlayerJoinEvent(this, plr, acc, client));

	}

	public void sendLoginResponse(SmartfoxClient client, ClientToServerAuthPacket auth, CenturiaAccount acc, int statusCode, int pendingFlags){
		JsonObject response = new JsonObject();
		JsonObject b = new JsonObject();
		b.addProperty("r", auth.rField);
		JsonObject o = new JsonObject();
		o.addProperty("statusId", statusCode);
		o.addProperty("_cmd", "login");
		JsonObject params = new JsonObject();
		params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
		params.addProperty("pendingFlags", pendingFlags);
		params.addProperty("activeLookId", acc.getActiveLook());
		params.addProperty("sanctuaryLookId", acc.getActiveSanctuaryLook());
		params.addProperty("sessionId", acc.getAccountID());
		params.addProperty("userId", acc.getAccountNumericID());
		params.addProperty("avatarInvId", 0);
		o.add("params", params);
		o.addProperty("status", statusCode);
		b.add("o", o);
		response.add("b", b);
		response.addProperty("t", "xt");
		
		try {
			sendPacket(client, response.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}

	// IP ban checks (both vpn block and ip banning)
	public static boolean isIPBanned(Socket socket, CenturiaAccount acc, ArrayList<String> vpnIpsV4,
			ArrayList<String> vpnIpsV6, String whitelistFile) throws IOException {

		// Check ip ban
		if (isIPBanned(socket))
			return true;

		// Check whitelisted accounts
		if (whitelistFile != null) {
			File whitelistData = new File(whitelistFile);
			if (whitelistData.exists()) {
				for (String login : Files.readAllLines(whitelistData.toPath())) {
					if (acc.getLoginName().equals(login))
						return false;
				}
			}
		}

		// Check VPN ban
		if (isEndpointVPN(socket, vpnIpsV4, vpnIpsV6))
			return true;

		return false;
	}

	// VPN check
	private static boolean isEndpointVPN(Socket socket, ArrayList<String> vpnIpsV4, ArrayList<String> vpnIpsV6) {
		// get client's remote socket address
		String address = socket.getRemoteSocketAddress().toString().substring(1).split(":")[0];
		// String address = "185.62.206.20";

		ArrayList<String> vpnIpsToCheck = new ArrayList<String>();

		// is the address ipv4, or ipv6? we don't need to loop through both
		if (AddressChecker.isIPv4(address)) {
			vpnIpsToCheck.addAll(vpnIpsV4);
		} else if (AddressChecker.isIPv6(address)) {
			vpnIpsToCheck.addAll(vpnIpsV6);
		}

		for (String ipToCheckAgainst : vpnIpsToCheck) {
			IpAddressMatcher matcher = new IpAddressMatcher(ipToCheckAgainst);

			if (matcher.matches(address)) {
				// VPN detected
				return true;
			}
		}

		// no VPN
		return false;
	}

	// IP ban check
	private static boolean isIPBanned(Socket socket) {
		InetSocketAddress ip = (InetSocketAddress) socket.getRemoteSocketAddress();
		InetAddress addr = ip.getAddress();
		String ipaddr = addr.getHostAddress();

		return IpBanManager.getInstance().isIPBanned(ipaddr);
	}

	public boolean isBanned(CenturiaAccount acc) {
		if (acc.getPlayerInventory().containsItem("penalty") && acc.getPlayerInventory().getItem("penalty")
				.getAsJsonObject().get("type").getAsString().equals("ban")) {
			JsonObject banInfo = acc.getPlayerInventory().getItem("penalty").getAsJsonObject();
			if (banInfo.get("unbanTimestamp").getAsLong() == -1
					|| banInfo.get("unbanTimestamp").getAsLong() > System.currentTimeMillis()) {
				return true;
			} else
				acc.getPlayerInventory().deleteItem("penalty");
		}

		return false;
	}

	@Override
	protected void clientDisconnect(SmartfoxClient client) {
		if (client.container != null && client.container instanceof Player) {
			Player plr = (Player) client.container;
			if (players.containsKey(plr.account.getAccountID())) {
				players.remove(plr.account.getAccountID());
				System.out.println("Player disconnected: " + plr.account.getLoginName() + " (was "
						+ plr.account.getDisplayName() + ")");

				// Dispatch leave event
				EventBus.getInstance().dispatchEvent(new PlayerLeaveEvent(this, plr, plr.account, client));
			}

			// Clear objects
			plr.respawnItems.clear();

			// Remove player character from all clients
			for (Player player : getPlayers()) {
				if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
					plr.destroyAt(player);
				}
			}

			// Disconnect from chat server
			for (ChatClient cl : Centuria.chatServer.getClients()) {
				if (cl.getPlayer().getAccountID().equals(plr.account.getAccountID())) {
					Thread th = new Thread(() -> {
						int i = 0;
						while (cl.isConnected()) {
							if (i == 3)
								break;
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
							i++;
						}

						if (cl.isConnected())
							cl.disconnect();
					}, "Chat Client Cleanup: " + cl.getPlayer().getAccountID());
					th.setDaemon(true);
					th.start();
					break;
				}
			}

			// Check maintenance, exit server if noone is online during maintenance
			if (maintenance && players.size() == 0) {
				if (!shutdown) {
					// Dispatch maintenance end event
					EventBus.getInstance().dispatchEvent(new MaintenanceEndEvent());
				}

				// Exit
				System.exit(0);
			} else {
				// Notify followers
				if (SocialManager.getInstance().socialListExists(plr.account.getAccountID())) {
					// Find all followers
					for (SocialEntry ent : SocialManager.getInstance().getFollowerPlayers(plr.account.getAccountID())) {
						// Send online status update
						Player player = getPlayer(ent.playerID);
						if (player != null) {
							RelationshipFollowOnlineStatusUpdate res = new RelationshipFollowOnlineStatusUpdate();
							res.userUUID = plr.account.getAccountID();
							res.playerOnlineStatus = OnlineStatus.Offline;
							client.sendPacket(res);
						}
					}
				}
			}
		}
	}

	@Override
	protected void onStart() {
		// Anti-expiry (kicks players who go past token expiry)
		Thread th = new Thread(() -> {
			while (Centuria.directorServer.isActive()) {
				// Find players who are logged in for longer than two days
				for (Player plr : getPlayers()) {
					long loginTimestamp = plr.account.getLastLoginTime();
					if ((System.currentTimeMillis() / 1000) - (2 * 24 * 60 * 60) >= loginTimestamp) {
						// Kick players that are ingame for wayyy to long
						plr.account.kickDirect("SYSTEM", "Session expired");
					}
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		}, "Player API Access Watchdog");
		th.setDaemon(true);
		th.start();

		// Resource respawn
		th = new Thread(() -> {
			while (Centuria.directorServer.isActive()) {
				// Loop through all players
				for (Player plr : getPlayers()) {
					if (!plr.roomReady)
						continue;

					// Loop through the resources
					String[] itms;
					while (true) {
						try {
							itms = plr.respawnItems.keySet().toArray(t -> new String[t]);
							break;
						} catch (ConcurrentModificationException e) {
						}
					}
					for (String id : itms) {
						if (!plr.respawnItems.containsKey(id))
							continue;

						long respawn = plr.respawnItems.get(id);
						if (!plr.roomReady)
							break;

						if (System.currentTimeMillis() >= respawn) {
							plr.respawnItems.remove(id);

							// Find object
							NetworkedObject ent = NetworkedObjects.getObject(id);

							// Respawn
							XtWriter wr = new XtWriter();
							wr.writeString("oi");
							wr.writeInt(-1); // data prefix

							// Object creation parameters
							wr.writeString(id); // World object ID
							wr.writeInt(978);
							wr.writeString(""); // Owner ID

							// Object info
							wr.writeInt(0);
							wr.writeLong(System.currentTimeMillis() / 1000);
							wr.writeDouble(ent.locationInfo.position.x);
							wr.writeDouble(ent.locationInfo.position.y);
							wr.writeDouble(ent.locationInfo.position.z);
							wr.writeDouble(ent.locationInfo.rotation.x);
							wr.writeDouble(ent.locationInfo.rotation.y);
							wr.writeDouble(ent.locationInfo.rotation.z);
							wr.writeDouble(ent.locationInfo.rotation.w);
							wr.add("0%0%0%0.0%0%0%0");
							wr.writeString(""); // data suffix

							if (plr.client != null && plr.client.isConnected())
								plr.client.sendPacket(wr.encode());
						}
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}, "Resource respawn thread");
		th.setDaemon(true);
		th.start();
	}

	@Override
	protected void onStop() {
	}

	// Used to check permissions
	public static boolean hasPerm(String level, String perm) {
		switch (level) {
		case "moderator":
			return perm.equals("moderator");

		case "admin":
			switch (perm) {
			case "moderator":
				return true;
			case "admin":
				return true;
			default:
				return false;
			}

		case "developer":
			switch (perm) {
			case "moderator":
				return true;
			case "admin":
				return true;
			case "developer":
				return true;
			default:
				return false;
			}
		}

		return false;
	}

	/**
	 * Retrieves a online player by ID
	 * 
	 * @param accountID Player ID
	 * @return Player instance or null if offline
	 */
	public Player getPlayer(String accountID) {
		if (players.containsKey(accountID))
			return players.get(accountID);
		return null;
	}

}
