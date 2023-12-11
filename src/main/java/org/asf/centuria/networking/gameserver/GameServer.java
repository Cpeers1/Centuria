package org.asf.centuria.networking.gameserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.SaveMode;
import org.asf.centuria.accounts.SaveSettings;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.players.OnlineStatus;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.ipbans.IpBanManager;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.accounts.AccountDisconnectEvent;
import org.asf.centuria.modules.events.accounts.AccountLoginEvent;
import org.asf.centuria.modules.events.accounts.AccountPreloginEvent;
import org.asf.centuria.modules.events.accounts.AccountDisconnectEvent.DisconnectType;
import org.asf.centuria.modules.events.players.PlayerJoinEvent;
import org.asf.centuria.modules.events.players.PlayerLeaveEvent;
import org.asf.centuria.modules.events.servers.GameServerStartupEvent;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.smartfox.BaseSmartfoxServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.networking.smartfox.SocketSmartfoxClient;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;
import org.asf.centuria.packets.xml.handshake.auth.ClientToServerAuthPacket;
import org.asf.centuria.packets.xml.handshake.version.ClientToServerHandshake;
import org.asf.centuria.packets.xml.handshake.version.ServerToClientOK;
import org.asf.centuria.packets.xt.*;
import org.asf.centuria.packets.xt.gameserver.*;
import org.asf.centuria.packets.xt.gameserver.avatar.*;
import org.asf.centuria.packets.xt.gameserver.inventory.*;
import org.asf.centuria.packets.xt.gameserver.item.*;
import org.asf.centuria.packets.xt.gameserver.messages.GenericMessagePacket;
import org.asf.centuria.packets.xt.gameserver.minigame.*;
import org.asf.centuria.packets.xt.gameserver.object.*;
import org.asf.centuria.packets.xt.gameserver.relationship.*;
import org.asf.centuria.packets.xt.gameserver.room.*;
import org.asf.centuria.packets.xt.gameserver.sanctuary.*;
import org.asf.centuria.packets.xt.gameserver.setting.*;
import org.asf.centuria.packets.xt.gameserver.shop.*;
import org.asf.centuria.packets.xt.gameserver.trade.*;
import org.asf.centuria.packets.xt.gameserver.user.*;
import org.asf.centuria.packets.xt.gameserver.world.*;
import org.asf.centuria.rooms.GameRoomManager;
import org.asf.centuria.security.AddressChecker;
import org.asf.centuria.security.IpAddressMatcher;
import org.asf.centuria.social.SocialEntry;
import org.asf.centuria.social.SocialManager;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
	private GameRoomManager roomManager = new GameRoomManager(this);

	public ArrayList<String> vpnIpsV4 = new ArrayList<String>();
	public ArrayList<String> vpnIpsV6 = new ArrayList<String>();

	public String whitelistFile = null;

	/**
	 * Retrieves all connected players
	 * 
	 * @return Array of Player instances
	 */
	public Player[] getPlayers() {
		synchronized (players) {
			return players.values().toArray(t -> new Player[t]);
		}
	}

	@Override
	protected void registerServerPackets() {
		mapper = new XmlMapper();

		// Handshake
		registerPacket(new ClientToServerHandshake(mapper));
		registerPacket(new ClientToServerAuthPacket(mapper));

		// Allow modules to register packets and override existing packets
		GameServerStartupEvent ev = new GameServerStartupEvent(this, t -> registerPacket(t));
		EventBus.getInstance().dispatchEvent(ev);

		// Game
		registerPacket(new KeepAlive());
		registerPacket(new PrefixedPacket());
		registerPacket(new InventoryItemDownloadPacket());
		registerPacket(new InventoryItemUseDye());
		registerPacket(new TradeListUpdatePacket());
		registerPacket(new RoomJoinPacket());
		registerPacket(new RoomJoinPreviousPacket());
		registerPacket(new RoomJoinTutorialPacket());
		registerPacket(new MinigameJoinPacket());
		registerPacket(new MinigameMessagePacket());
		registerPacket(new ShopListPacket());
		registerPacket(new ShopItemBuyRequestPacket());
		registerPacket(new ItemUncraftPacket());
		registerPacket(new WorldReadyPacket());
		registerPacket(new ObjectUpdatePacket());
		registerPacket(new ObjectRespawnRequestPacket());
		registerPacket(new ObjectRespawnSetPacket());
		registerPacket(new ObjectGlidePacket());
		registerPacket(new AvatarLookGetPacket());
		registerPacket(new RelationshipPlayerOnlineStatusPacket());
		registerPacket(new RelationshipJumpToPlayerPacket());
		registerPacket(new RelationshipFindPlayerPacket());
		registerPacket(new AvatarActionPacket());
		registerPacket(new ObjectActionStartPacket());
		registerPacket(new ObjectActionCancelPacket());
		registerPacket(new ObjectActionFinishPacket());
		registerPacket(new ObjectAskResponsePacket());
		registerPacket(new UserTutorialCompletedPacket());
		registerPacket(new AvatarSelectLookPacket());
		registerPacket(new AvatarLookSavePacket());
		registerPacket(new SanctuaryJoinPacket());
		registerPacket(new SanctuaryLookSwitchPacket());
		registerPacket(new SanctuaryLookLoadPacket());
		registerPacket(new SanctuaryUpdatePacket());
		registerPacket(new SanctuaryLookSavePacket());
		registerPacket(new SanctuaryUpgradeStartPacket());
		registerPacket(new SanctuaryUpgradeCompletePacket());
		registerPacket(new SettingsSetPacket());
		registerPacket(new InventoryItemInspirationCombinePacket());
		registerPacket(new GiftRedeemPacket());
		registerPacket(new GenericMessagePacket());

		// Trading Packets
		registerPacket(new TradeListPacket());
		registerPacket(new TradeInitiatePacket());
		registerPacket(new TradeInitiateCancelPacket());
		registerPacket(new TradeInitiateRejectPacket());
		registerPacket(new TradeInitiateAcceptPacket());
		registerPacket(new TradeExitPacket());
		registerPacket(new TradeAddRemoveItemPacket());
		registerPacket(new TradeReadyPacket());
		registerPacket(new TradeReadyRejectPacket());
		registerPacket(new TradeReadyAcceptPacket());
	}

	@Override
	protected void startClient(SmartfoxClient client) throws IOException {
		// Protocol switch possible
		client.allowProtocolSwitch();

		// Read first handshake packet
		ClientToServerHandshake pk = client.readPacket(ClientToServerHandshake.class);

		// Check EFGL support
		if (pk.supportsEfgl) {
			// Switch protocol
			client.switchToEfgl();
		}

		// Disable protocol switching
		client.disableProtocolSwitch();

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
		auth.pword = null; // Keep it away from modules

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

		// Verify access
		if (!payload.has("acs") || !payload.get("acs").getAsString().equals("gameplay")) {
			client.disconnect();
			return;
		}

		// Locate account
		CenturiaAccount acc = AccountManager.getInstance().getAccount(payload.get("uuid").getAsString());
		if (acc == null) {
			client.disconnect();
			return;
		}

		// Rename thread
		Thread.currentThread().setName("Game Client Thread: " + acc.getDisplayName() + " [ID " + acc.getAccountID()
				+ ", Address " + client.getAddress() + "]");

		// Allow modules to add parameter fields
		JsonObject params = new JsonObject();
		AccountPreloginEvent evt = new AccountPreloginEvent(this, acc, client, params, auth);
		EventBus.getInstance().dispatchEvent(evt);
		if (evt.isHandled() && evt.getStatus() != 1) {
			sendLoginResponse(client, auth, acc, evt.getStatus(), 0, evt.getLoginResponseParameters());

			Centuria.logger.info("Login failure: " + acc.getLoginName() + ": module terminated login process with code "
					+ evt.getStatus());
			client.disconnect();
			return;
		}

		// If under maintenance, send error
		if (maintenance) {
			boolean lockout = true;

			// Check permissions
			if (acc.getSaveSharedInventory().containsItem("permissions")) {
				String permLevel = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
						.get("permissionLevel").getAsString();
				if (hasPerm(permLevel, "admin")) {
					lockout = false;
				}
			}

			if (lockout || shutdown) {
				sendLoginResponse(client, auth, acc, -16, 0, params);

				client.disconnect();
				return;
			}
		}

		// If the client is out of date, send error
		if (badClient) {
			sendLoginResponse(client, auth, acc, -24, 0, params);

			client.disconnect();
			return;
		}

		// Check ban
		if (acc.isBanned()) {
			JsonObject banInfo = acc.getSaveSharedInventory().getItem("penalty").getAsJsonObject();
			if (banInfo.get("unbanTimestamp").getAsLong() != -1) {
				SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				f.setTimeZone(TimeZone.getTimeZone("UTC"));
				if (!banInfo.has("reason")) {
					params.addProperty("errorMessage",
							"\nThis account is currently suspended and may not log in at this time.\n\n"
									+ "Note that this is a temporary ban, you can play again at: "
									+ f.format(new Timestamp(banInfo.get("unbanTimestamp").getAsLong())));
				} else {
					params.addProperty("errorMessage",
							"\nTemporarily suspended until "
									+ f.format(new Timestamp(banInfo.get("unbanTimestamp").getAsLong())) + "\n\n"
									+ "Reason: " + banInfo.get("reason").getAsString());
				}
			} else if (banInfo.has("reason"))
				params.addProperty("errorMessage",
						"\nThis account is currently suspended and may not log in at this time.\n\n" + "Reason: "
								+ banInfo.get("reason").getAsString());
			Centuria.logger.info("User '" + acc.getDisplayName() + "' could not connect: user is banned.");

			// Disconnect with error
			sendLoginResponse(client, auth, acc, -15, 0, params);

			client.disconnect();
			return;
		}

		// Check ip ban
		if (isIPBanned(client.getAddress(), acc, vpnIpsV4, vpnIpsV6, whitelistFile)) {
			params.addProperty("errorMessage",
					"This server does not allow the use of a VPN, please disable it and try again.");
			Centuria.logger.info("User '" + acc.getDisplayName() + "' could not connect: user is IP or VPN banned.");

			// Disconnect silently
			sendLoginResponse(client, auth, acc, 1, 0, params);

			client.disconnect();
			return;
		}

		// Disconnect an already connected instance
		Player ePlr = getPlayer(acc.getAccountID());
		if (ePlr != null) {
			EventBus.getInstance().dispatchEvent(new AccountDisconnectEvent(ePlr.account,
					"Logged in from another location.", DisconnectType.DUPLICATE_LOGIN));
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
			ePlr.client.disconnect();
		}

		// Log the login attempt
		Centuria.logger.info("Login from IP: " + client.getAddress() + ": " + acc.getLoginName());

		// Run module handshake code
		AccountLoginEvent ev = new AccountLoginEvent(this, acc, client, params);
		EventBus.getInstance().dispatchEvent(ev);
		if (ev.isHandled() && ev.getStatus() != 1) {
			sendLoginResponse(client, auth, acc, ev.getStatus(), 0, ev.getLoginResponseParameters());

			Centuria.logger.info("Login failure: " + acc.getLoginName() + ": module terminated login process with code "
					+ ev.getStatus());
			client.disconnect();
			return;
		}

		// Send response
		sendLoginResponse(client, auth, acc, 1, acc.isPlayerNew() ? 2 : 3, params);
		sendPacket(client, "%xt%ulc%-1%");

		// Add player
		loginPlayer(acc, client);
	}

	/**
	 * Adds players to the server (NOTE: if you are using this from a custom server
	 * extension, make sure to have a custom account manager too, also note that
	 * this might bug everything out if its not a player on disk, these setups are
	 * allowed but NOT supported)
	 * 
	 * @param acc    Player account
	 * @param client Player client
	 * @return Player instance
	 */
	protected Player loginPlayer(CenturiaAccount acc, SmartfoxClient client) {
		Player plr = new Player();

		// Build Player object
		plr.client = client;
		plr.account = acc;
		plr.activeLook = acc.getActiveLook();
		plr.activeSanctuaryLook = acc.getActiveSanctuaryLook();

		// Avatar look failsafe, check if the active look is actually a primary look
		// If not, switch to the first primary look of the same species
		//
		// This is to resolve those currently being affected by the avatar look
		// overwrite bug thats been plaguing EmuFeral online
		if (acc.getSaveSpecificInventory().containsItem("avatars")) {
			JsonArray avatars = acc.getSaveSpecificInventory().getItem("avatars").getAsJsonArray();
			for (JsonElement ele : avatars) {
				// Read avatar
				JsonObject ava = ele.getAsJsonObject();
				String dID = ava.get("defId").getAsString();
				String lID = ava.get("id").getAsString();

				// Check if active look
				if (lID.equals(plr.activeLook)) {
					// Check if its a primary look
					if (!ava.get("components").getAsJsonObject().has("PrimaryLook")) {
						// Find first primary look
						for (JsonElement ele2 : avatars) {
							JsonObject ava2 = ele2.getAsJsonObject();
							String dID2 = ava2.get("defId").getAsString();
							String lID2 = ava2.get("id").getAsString();
							if (ava2.get("components").getAsJsonObject().has("PrimaryLook") && dID.equals(dID2)) {
								// Found the primary look
								// Set as active look
								plr.activeLook = lID2;
								plr.account.setActiveLook(lID2);
								break;
							}
						}
					}

					// Found the active look so we can end the loop
					break;
				}
			}
		}

		// Assign permissions
		if (acc.getSaveSharedInventory().containsItem("permissions")) {
			String permLevel = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
			plr.hasModPerms = GameServer.hasPerm(permLevel, "moderator");
		}

		// Update login timestamp
		acc.login();

		// Save player in the client object
		client.container = plr;

		// Initial login
		Centuria.logger.info("Player connected: " + plr.account.getLoginName() + " (as " + plr.account.getDisplayName()
				+ ", uuid: " + acc.getAccountID() + ")");

		// Init with ghost mode if previously enabled
		if (plr.hasModPerms) {
			// Load last ghost mode setting
			if (acc.getSaveSharedInventory().containsItem("ghostmode"))
				plr.ghostMode = true;
		} else if (acc.getSaveSharedInventory().containsItem("ghostmode"))
			acc.getSaveSharedInventory().deleteItem("ghostmode");

		// Notify followers
		if (!plr.ghostMode && SocialManager.getInstance().socialListExists(plr.account.getAccountID())) {
			// Find all followings, notify the current player of which friends are online
			for (SocialEntry ent : SocialManager.getInstance().getFollowingPlayers(plr.account.getAccountID())) {
				// Send online status update
				Player player = getPlayer(ent.playerID);
				if (player != null) {
					RelationshipFollowOnlineStatusUpdatePacket res = new RelationshipFollowOnlineStatusUpdatePacket();
					res.userUUID = plr.account.getAccountID();
					res.playerOnlineStatus = OnlineStatus.LoggedInToRoom;
					client.sendPacket(res);
				}
			}

			// Notify followers
			for (SocialEntry ent : SocialManager.getInstance().getFollowerPlayers(plr.account.getAccountID())) {
				// Send online status update
				Player player = getPlayer(ent.playerID);
				if (player != null) {
					RelationshipFollowOnlineStatusUpdatePacket res = new RelationshipFollowOnlineStatusUpdatePacket();
					res.userUUID = plr.account.getAccountID();
					res.playerOnlineStatus = OnlineStatus.LoggedInToRoom;
					player.client.sendPacket(res);
				}
			}

			// Add blocked players
			for (String id : SocialManager.getInstance().getBlockedPlayers(plr.account.getAccountID())) {
				// Check permissions
				CenturiaAccount blockedPlayer = AccountManager.getInstance().getAccount(id);

				// Load permission level
				String permLevel = "member";
				if (blockedPlayer.getSaveSharedInventory().containsItem("permissions")) {
					permLevel = blockedPlayer.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator")) {
					// Block sync
					plr.syncBlockedPlayers.add(id);
				}
			}
		}

		// Add player
		synchronized (players) {
			players.put(plr.account.getAccountID(), plr);
		}

		// Dispatch join event
		EventBus.getInstance().dispatchEvent(new PlayerJoinEvent(this, plr, acc, client));
		return plr;
	}

	/**
	 * Called to remove a player from the server after they logged out, not intended
	 * for kicking
	 * 
	 * @param plr Player instance
	 */
	protected void playerLeft(Player plr) {
		boolean wasPresent = false;
		synchronized (players) {
			if (players.containsKey(plr.account.getAccountID())) {
				players.remove(plr.account.getAccountID());
				Centuria.logger.info("Player disconnected: " + plr.account.getLoginName() + " (was "
						+ plr.account.getDisplayName() + ")");
				wasPresent = true;
			}
		}
		if (wasPresent) {
			// Dispatch leave event
			EventBus.getInstance().dispatchEvent(new PlayerLeaveEvent(this, plr, plr.account, plr.client));
		}

		// Clear objects
		plr.respawnItems.clear();

		// End current game
		if (plr.currentGame != null) {
			plr.currentGame.onExit(plr);
			plr.currentGame = null;
		}

		// Remove player character from all clients
		for (Player player : getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		// Disconnect from chat server if not
		// Check moderator perms
		String permLevel = "member";
		if (plr.account.getSaveSharedInventory().containsItem("permissions")) {
			permLevel = plr.account.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
		}

		// Security checks
		// Check moderator perms
		if (!GameServer.hasPerm(permLevel, "moderator")) {
			for (ChatClient cl : Centuria.chatServer.getClients()) {
				if (cl.getPlayer().getAccountID().equals(plr.account.getAccountID())) {
					Thread th = new Thread(() -> {
						// Try letting it disconnect on its own
						int i = 0;
						while (cl.isConnected()) {
							if (i == 5)
								break;
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
							i++;
						}

						// Terminate the connection instead
						if (cl.isConnected())
							cl.disconnect();
					}, "Chat Client Cleanup: " + cl.getPlayer().getAccountID());
					th.setDaemon(true);
					th.start();
					break;
				}
			}
		}

		// Notify followers
		if (SocialManager.getInstance().socialListExists(plr.account.getAccountID())) {
			// Find all followers
			for (SocialEntry ent : SocialManager.getInstance().getFollowerPlayers(plr.account.getAccountID())) {
				// Send online status update
				Player player = getPlayer(ent.playerID);
				if (player != null) {
					RelationshipFollowOnlineStatusUpdatePacket res = new RelationshipFollowOnlineStatusUpdatePacket();
					res.userUUID = plr.account.getAccountID();
					res.playerOnlineStatus = OnlineStatus.Offline;
					player.client.sendPacket(res);
				}
			}
		}

	}

	/**
	 * Sends a login response
	 * 
	 * @param client       Client
	 * @param auth         Authentication packet
	 * @param acc          Account thats logging in
	 * @param statusCode   Status code
	 * @param pendingFlags Pending flags value
	 * @param params       Parameter object
	 */
	public void sendLoginResponse(SmartfoxClient client, ClientToServerAuthPacket auth, CenturiaAccount acc,
			int statusCode, int pendingFlags, JsonObject params) {
		JsonObject response = new JsonObject();
		JsonObject b = new JsonObject();
		b.addProperty("r", auth.rField);
		JsonObject o = new JsonObject();
		o.addProperty("statusId", statusCode);
		o.addProperty("_cmd", "login");

		// Jamaa time is seconds since 1-1-2015
		params.addProperty("jamaaTime", (System.currentTimeMillis() / 1000)
				- ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toEpochSecond());

		// Add login flags
		params.addProperty("pendingFlags", pendingFlags);

		// Look IDs
		params.addProperty("activeLookId", acc.getActiveLook());
		params.addProperty("sanctuaryLookId", acc.getActiveSanctuaryLook());

		// Session ids etc
		params.addProperty("sessionId", acc.getAccountID());
		params.addProperty("userId", acc.getAccountNumericID());
		params.addProperty("avatarInvId", 0);
		o.add("params", params);
		o.addProperty("status", statusCode);
		b.add("o", o);
		response.add("b", b);
		response.addProperty("t", "xt");
		sendPacket(client, response.toString());
		return;
	}

	// IP ban checks (both vpn block and ip banning)
	public static boolean isIPBanned(String address, CenturiaAccount acc, ArrayList<String> vpnIpsV4,
			ArrayList<String> vpnIpsV6, String whitelistFile) throws IOException {

		// Check ip ban
		if (isIPBanned(address))
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
		if (isEndpointVPN(address, vpnIpsV4, vpnIpsV6))
			return true;

		return false;
	}

	// VPN check
	private static boolean isEndpointVPN(String address, ArrayList<String> vpnIpsV4, ArrayList<String> vpnIpsV6) {
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
	private static boolean isIPBanned(String ipaddr) {
		return IpBanManager.getInstance().isIPBanned(ipaddr);
	}

	@Override
	protected void clientDisconnect(SmartfoxClient client) {
		if (client.container != null && client.container instanceof Player) {
			Player plr = (Player) client.container;
			playerLeft(plr);
		}
	}

	@Override
	protected void onStart() {
		// Anti-expiry (kicks players who go past token expiry)
		Thread th = new Thread(() -> {
			while (Centuria.directorServer.isRunning()) {
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
			while (Centuria.directorServer.isRunning()) {
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
		// Check if player
		if (perm.equals("player") || perm.equals("member"))
			return true;

		// Check other levels
		// Check by player permission level
		// level = level of permissions in player save data
		// perm = permission to verify
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

	private static class PrefixedNameCacheContainer {
		public String playerNameWithPrefix;
	}

	// Used to generate names with permission/save prefixes
	public static String getPlayerNameWithPrefix(CenturiaAccount account) {
		// Check if disabled
		if (account.getSaveSharedInventory().containsItem("prefixdisabled")) {
			// Build player prefix
			String prefix = "";

			// Load save color settings
			String color = "default";
			SaveSettings saveSettings = account.getSaveSpecificInventory().getSaveSettings();
			if (saveSettings != null && saveSettings.saveColors != null) {
				if (saveSettings.saveColors.has("member"))
					color = saveSettings.saveColors.get("member").getAsString();
				else if (saveSettings.saveColors.has("player"))
					color = saveSettings.saveColors.get("player").getAsString();
			}

			// Check color
			if (color.equals("default") && account.getSaveMode() == SaveMode.MANAGED) {
				if (saveSettings.giveAllAvatars && saveSettings.giveAllMods && saveSettings.giveAllWings
						&& saveSettings.giveAllSanctuaryTypes) {
					// Creative
					color = "yellow";
				} else if (!saveSettings.giveAllAvatars && !saveSettings.giveAllMods && !saveSettings.giveAllClothes
						&& !saveSettings.giveAllWings && !saveSettings.giveAllSanctuaryTypes
						&& !saveSettings.giveAllFurnitureItems && !saveSettings.giveAllResources
						&& !saveSettings.giveAllCurrency) {
					// Experience
					color = "green";
				}
			}

			// Add color to prefix
			if (!color.equals("default")) {
				prefix = "<color=" + color + ">";
			}

			// Build prefix
			String customPrefix = "";
			if (saveSettings != null && saveSettings.saveNamePrefixes != null) {
				if (saveSettings.saveNamePrefixes.has("member"))
					customPrefix = saveSettings.saveNamePrefixes.get("member").getAsString();
				else if (saveSettings.saveNamePrefixes.has("player"))
					customPrefix = saveSettings.saveNamePrefixes.get("player").getAsString();
			}
			if (!customPrefix.isEmpty())
				prefix += customPrefix + " ";

			// Return
			return prefix + account.getDisplayName() + (color.equals("default") ? "" : "</color>");
		}

		// Find ingame player
		Player plr = account.getOnlinePlayerInstance();
		if (plr != null) {
			// From cache
			PrefixedNameCacheContainer cache = plr.getObject(PrefixedNameCacheContainer.class);
			if (cache != null)
				return cache.playerNameWithPrefix;
		}

		// Load permission level
		String permLevel = "member";
		if (account.getSaveSharedInventory().containsItem("permissions")) {
			permLevel = account.getSaveSharedInventory().getItem("permissions").getAsJsonObject().get("permissionLevel")
					.getAsString();
		}

		// Build prefix
		String prefix = "";

		// Get tags
		String[] tags = Stream.of(account.getAccountTags()).map(t -> t.getTagID()).toArray(t -> new String[t]);

		// Load save color settings
		String color = "default";
		if (GameServer.hasPerm(permLevel, "developer"))
			color = "#ff00e6";
		else if (GameServer.hasPerm(permLevel, "admin"))
			color = "red";
		else if (GameServer.hasPerm(permLevel, "moderator"))
			color = "orange";

		SaveSettings saveSettings = account.getSaveSpecificInventory().getSaveSettings();
		if (saveSettings != null && saveSettings.saveColors != null) {
			if (GameServer.hasPerm(permLevel, "developer") && saveSettings.saveColors.has("developer"))
				color = saveSettings.saveColors.get("developer").getAsString();
			else if (GameServer.hasPerm(permLevel, "admin") && saveSettings.saveColors.has("admin"))
				color = saveSettings.saveColors.get("admin").getAsString();
			else if (GameServer.hasPerm(permLevel, "moderator") && saveSettings.saveColors.has("moderator"))
				color = saveSettings.saveColors.get("moderator").getAsString();
			else {
				// By tag
				boolean foundTag = false;
				for (String tag : tags) {
					if (saveSettings.saveColors.has("#" + tag)) {
						// Update
						color = saveSettings.saveColors.get("#" + tag).getAsString();
						foundTag = true;
						break;
					}
				}

				// Find by tag configuration
				if (!foundTag) {
					// Go through tags
					for (String tag : tags) {
						// Find tag globals
						try {
							File tagGlobals = new File("tags/" + tag + ".json");
							if (tagGlobals.exists()) {
								JsonObject globals = JsonParser.parseString(Files.readString(tagGlobals.toPath()))
										.getAsJsonObject();

								// Verify globals
								if (globals.has("playerNameColor")) {
									// Update
									color = globals.get("playerNameColor").getAsString();
									foundTag = true;
									break;
								}
							}
						} catch (Exception e) {
						}
					}

					// Player fallback
					if (!foundTag) {
						if (GameServer.hasPerm(permLevel, "member") && saveSettings.saveColors.has("member"))
							color = saveSettings.saveColors.get("member").getAsString();
						else if (GameServer.hasPerm(permLevel, "player") && saveSettings.saveColors.has("player"))
							color = saveSettings.saveColors.get("player").getAsString();
					}
				}
			}
		} else {
			// Go through tags
			boolean foundTag = false;
			for (String tag : tags) {
				// Find tag globals
				try {
					File tagGlobals = new File("tags/" + tag + ".json");
					if (tagGlobals.exists()) {
						JsonObject globals = JsonParser.parseString(Files.readString(tagGlobals.toPath()))
								.getAsJsonObject();

						// Verify globals
						if (globals.has("playerNameColor")) {
							// Update
							color = globals.get("playerNameColor").getAsString();
							foundTag = true;
							break;
						}
					}
				} catch (Exception e) {
				}
			}

			// Player fallback
			if (!foundTag && saveSettings.saveColors != null) {
				if (GameServer.hasPerm(permLevel, "member") && saveSettings.saveColors.has("member"))
					color = saveSettings.saveColors.get("member").getAsString();
				else if (GameServer.hasPerm(permLevel, "player") && saveSettings.saveColors.has("player"))
					color = saveSettings.saveColors.get("player").getAsString();
			}
		}

		// Check color
		if (color.equals("default") && account.getSaveMode() == SaveMode.MANAGED) {
			if (saveSettings.giveAllAvatars && saveSettings.giveAllMods && saveSettings.giveAllWings
					&& saveSettings.giveAllSanctuaryTypes) {
				// Creative
				color = "yellow";
			} else if (!saveSettings.giveAllAvatars && !saveSettings.giveAllMods && !saveSettings.giveAllClothes
					&& !saveSettings.giveAllWings && !saveSettings.giveAllSanctuaryTypes
					&& !saveSettings.giveAllFurnitureItems && !saveSettings.giveAllResources
					&& !saveSettings.giveAllCurrency) {
				// Experience
				color = "green";
			}
		}

		// Add color to prefix
		if (!color.equals("default")) {
			prefix = "<color=" + color + ">";
		}

		// Build prefix
		String customPrefix = "";
		if (saveSettings != null && saveSettings.saveNamePrefixes != null) {
			if (GameServer.hasPerm(permLevel, "developer") && saveSettings.saveNamePrefixes.has("developer"))
				customPrefix = saveSettings.saveNamePrefixes.get("developer").getAsString();
			else if (GameServer.hasPerm(permLevel, "admin") && saveSettings.saveNamePrefixes.has("admin"))
				customPrefix = saveSettings.saveNamePrefixes.get("admin").getAsString();
			else if (GameServer.hasPerm(permLevel, "moderator") && saveSettings.saveNamePrefixes.has("moderator"))
				customPrefix = saveSettings.saveNamePrefixes.get("moderator").getAsString();
			else {
				// By tag
				boolean foundTag = false;
				for (String tag : tags) {
					if (saveSettings.saveNamePrefixes.has("#" + tag)) {
						// Update
						customPrefix = saveSettings.saveNamePrefixes.get("#" + tag).getAsString();
						foundTag = true;
						break;
					}
				}

				// Find by tag configuration
				if (!foundTag) {
					// Go through tags
					for (String tag : tags) {
						// Find tag globals
						try {
							File tagGlobals = new File("tags/" + tag + ".json");
							if (tagGlobals.exists()) {
								JsonObject globals = JsonParser.parseString(Files.readString(tagGlobals.toPath()))
										.getAsJsonObject();

								// Verify globals
								if (globals.has("playerNamePrefix")) {
									// Update
									customPrefix = globals.get("playerNamePrefix").getAsString();
									foundTag = true;
									break;
								}
							}
						} catch (Exception e) {
						}
					}

					// Player fallback
					if (!foundTag) {
						if (GameServer.hasPerm(permLevel, "member") && saveSettings.saveNamePrefixes.has("member"))
							customPrefix = saveSettings.saveNamePrefixes.get("member").getAsString();
						else if (GameServer.hasPerm(permLevel, "player") && saveSettings.saveNamePrefixes.has("player"))
							customPrefix = saveSettings.saveNamePrefixes.get("player").getAsString();
					}
				}
			}
		} else {
			// Go through tags
			boolean foundTag = false;
			for (String tag : tags) {
				// Find tag globals
				try {
					File tagGlobals = new File("tags/" + tag + ".json");
					if (tagGlobals.exists()) {
						JsonObject globals = JsonParser.parseString(Files.readString(tagGlobals.toPath()))
								.getAsJsonObject();

						// Verify globals
						if (globals.has("playerNamePrefix")) {
							// Update
							customPrefix = globals.get("playerNamePrefix").getAsString();
							foundTag = true;
							break;
						}
					}
				} catch (Exception e) {
				}
			}

			// Player fallback
			if (!foundTag && saveSettings.saveNamePrefixes != null) {
				if (GameServer.hasPerm(permLevel, "member") && saveSettings.saveNamePrefixes.has("member"))
					customPrefix = saveSettings.saveNamePrefixes.get("member").getAsString();
				else if (GameServer.hasPerm(permLevel, "player") && saveSettings.saveNamePrefixes.has("player"))
					customPrefix = saveSettings.saveNamePrefixes.get("player").getAsString();
			}
		}
		if (!customPrefix.isEmpty())
			prefix += customPrefix + " ";
		else if (GameServer.hasPerm(permLevel, "developer"))
			prefix += "[dev] ";
		else if (GameServer.hasPerm(permLevel, "admin"))
			prefix += "[admin] ";
		else if (GameServer.hasPerm(permLevel, "moderator"))
			prefix += "[mod] ";

		// Build
		String name = prefix + account.getDisplayName() + (color.equals("default") ? "" : "</color>");

		// Cache
		PrefixedNameCacheContainer cache = new PrefixedNameCacheContainer();
		cache.playerNameWithPrefix = name;
		if (plr != null)
			plr.addObject(cache);

		// Return
		return name;
	}

	/**
	 * Retrieves a online player by ID
	 * 
	 * @param accountID Player ID
	 * @return Player instance or null if offline
	 */
	public Player getPlayer(String accountID) {
		synchronized (players) {
			if (players.containsKey(accountID))
				return players.get(accountID);
			return null;
		}
	}

	/**
	 * Retrieves the room manager of this server
	 * 
	 * @return GameRoomManager instance
	 */
	public GameRoomManager getRoomManager() {
		return roomManager;
	}

	@Override
	protected SmartfoxClient createSocketClient(Socket client) {
		return new SocketSmartfoxClient(client, this);
	}
}