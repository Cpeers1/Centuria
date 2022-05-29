package org.asf.emuferal.networking.gameserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.Random;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.networking.smartfox.BaseSmartfoxServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;
import org.asf.emuferal.packets.xml.handshake.auth.ClientToServerAuthPacket;
import org.asf.emuferal.packets.xml.handshake.version.ClientToServerHandshake;
import org.asf.emuferal.packets.xml.handshake.version.ServerToClientOK;
import org.asf.emuferal.packets.xt.KeepAlive;
import org.asf.emuferal.packets.xt.gameserver.PrefixedPacket;
import org.asf.emuferal.packets.xt.gameserver.avatareditor.AvatarEditorSelectLook;
import org.asf.emuferal.packets.xt.gameserver.avatareditor.UserAvatarSave;
import org.asf.emuferal.packets.xt.gameserver.avatareditor.UserTutorialCompleted;
import org.asf.emuferal.packets.xt.gameserver.interactions.InteractionCancel;
import org.asf.emuferal.packets.xt.gameserver.interactions.InteractionFinish;
import org.asf.emuferal.packets.xt.gameserver.interactions.InteractionStart;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemUseDye;
import org.asf.emuferal.packets.xt.gameserver.objects.WorldObjectGlide;
import org.asf.emuferal.packets.xt.gameserver.objects.WorldObjectRespawn;
import org.asf.emuferal.packets.xt.gameserver.objects.WorldObjectSetRespawn;
import org.asf.emuferal.packets.xt.gameserver.objects.WorldObjectUpdate;
import org.asf.emuferal.packets.xt.gameserver.players.AvatarAction;
import org.asf.emuferal.packets.xt.gameserver.players.AvatarLookGet;
import org.asf.emuferal.packets.xt.gameserver.shops.ShopList;
import org.asf.emuferal.packets.xt.gameserver.social.FindPlayer;
import org.asf.emuferal.packets.xt.gameserver.social.FollowOnlineStatusUpdate;
import org.asf.emuferal.packets.xt.gameserver.social.FollowingList;
import org.asf.emuferal.packets.xt.gameserver.social.JumpToPlayer;
import org.asf.emuferal.packets.xt.gameserver.social.PlayerOnlineStatus;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.packets.xt.gameserver.world.RoomJoinTutorial;
import org.asf.emuferal.packets.xt.gameserver.world.WorldReadyPacket;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.security.AddressChecker;
import org.asf.emuferal.security.IpAddressMatcher;

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
	private ArrayList<Player> players = new ArrayList<Player>();

	public ArrayList<String> vpnIpsV4 = new ArrayList<String>();
	public ArrayList<String> vpnIpsV6 = new ArrayList<String>();

	public String whitelistFile = null;

	public Player[] getPlayers() {
		while (true) {
			try {
				return players.toArray(t -> new Player[t]);
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
		registerPacket(new JoinRoom());
		registerPacket(new RoomJoinTutorial());
		registerPacket(new ShopList());
		registerPacket(new WorldReadyPacket());
		registerPacket(new WorldObjectUpdate());
		registerPacket(new WorldObjectRespawn());
		registerPacket(new WorldObjectSetRespawn());
		registerPacket(new WorldObjectGlide());
		registerPacket(new AvatarLookGet());
		registerPacket(new PlayerOnlineStatus());
		registerPacket(new JumpToPlayer());
		registerPacket(new FindPlayer());
		registerPacket(new AvatarAction());
		registerPacket(new InteractionStart());
		registerPacket(new InteractionCancel());
		registerPacket(new InteractionFinish());
		registerPacket(new UserTutorialCompleted());
		registerPacket(new AvatarEditorSelectLook());
		registerPacket(new UserAvatarSave());

		// social packets
		registerPacket(new FollowingList());
		registerPacket(new FollowOnlineStatusUpdate());
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
		if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
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
		EmuFeralAccount acc = AccountManager.getInstance().getAccount(payload.get("uuid").getAsString());
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
				JsonObject response = new JsonObject();
				JsonObject b = new JsonObject();
				b.addProperty("r", auth.rField);
				JsonObject o = new JsonObject();
				o.addProperty("statusId", -16);
				o.addProperty("_cmd", "login");
				JsonObject params = new JsonObject();
				params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
				params.addProperty("pendingFlags", 0);
				params.addProperty("activeLookId", acc.getActiveLook());
				params.addProperty("sanctuaryLookId", acc.getActiveSanctuaryLook());
				params.addProperty("sessionId", acc.getAccountID());
				params.addProperty("userId", acc.getAccountNumericID());
				params.addProperty("avatarInvId", 0);
				o.add("params", params);
				o.addProperty("status", -16);
				b.add("o", o);
				response.add("b", b);
				response.addProperty("t", "xt");
				sendPacket(client, response.toString());
				return;
			}
		}

		// If the client is out of date, send error
		if (badClient) {
			JsonObject response = new JsonObject();
			JsonObject b = new JsonObject();
			b.addProperty("r", auth.rField);
			JsonObject o = new JsonObject();
			o.addProperty("statusId", -24);
			o.addProperty("_cmd", "login");
			JsonObject params = new JsonObject();
			params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
			params.addProperty("pendingFlags", 0);
			params.addProperty("activeLookId", acc.getActiveLook());
			params.addProperty("sanctuaryLookId", acc.getActiveSanctuaryLook());
			params.addProperty("sessionId", acc.getAccountID());
			params.addProperty("userId", acc.getAccountNumericID());
			params.addProperty("avatarInvId", 0);
			o.add("params", params);
			o.addProperty("status", -24);
			b.add("o", o);
			response.add("b", b);
			response.addProperty("t", "xt");
			sendPacket(client, response.toString());
			return;
		}

		// Check ban
		if (isBanned(acc)) {
			// Disconnect with error
			JsonObject response = new JsonObject();
			JsonObject b = new JsonObject();
			b.addProperty("r", auth.rField);
			JsonObject o = new JsonObject();
			o.addProperty("statusId", -15);
			o.addProperty("_cmd", "login");
			JsonObject params = new JsonObject();
			params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
			params.addProperty("pendingFlags", 0);
			params.addProperty("activeLookId", acc.getActiveLook());
			params.addProperty("sanctuaryLookId", acc.getActiveSanctuaryLook());
			params.addProperty("sessionId", acc.getAccountID());
			params.addProperty("userId", acc.getAccountNumericID());
			params.addProperty("avatarInvId", 0);
			o.add("params", params);
			o.addProperty("status", -15);
			b.add("o", o);
			response.add("b", b);
			response.addProperty("t", "xt");
			sendPacket(client, response.toString());
			return;
		}

		// Check ip ban
		if (isIPBanned(client.getSocket(), acc, vpnIpsV4, vpnIpsV6, whitelistFile)) {
			// Disconnect silently
			JsonObject response = new JsonObject();
			JsonObject b = new JsonObject();
			b.addProperty("r", auth.rField);
			JsonObject o = new JsonObject();
			o.addProperty("statusId", -23);
			o.addProperty("_cmd", "login");
			JsonObject params = new JsonObject();
			params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
			params.addProperty("pendingFlags", 0);
			params.addProperty("activeLookId", acc.getActiveLook());
			params.addProperty("sanctuaryLookId", acc.getActiveSanctuaryLook());
			params.addProperty("sessionId", acc.getAccountID());
			params.addProperty("userId", acc.getAccountNumericID());
			params.addProperty("avatarInvId", 0);
			o.add("params", params);
			o.addProperty("status", -23);
			b.add("o", o);
			response.add("b", b);
			response.addProperty("t", "xt");
			sendPacket(client, response.toString());
			return;
		}

		// Disconnect an already connected instance
		for (Player player : getPlayers()) {
			if (player.account.getAccountID().equals(acc.getAccountID())) {
				player.client.disconnect();
			}
		}

		// Build Player object
		Player plr = new Player();
		plr.client = client;
		plr.account = acc;
		plr.activeLook = acc.getActiveLook();
		plr.activeSanctuaryLook = acc.getActiveSanctuaryLook();

		// Save player in the client object
		client.container = plr;

		// Send response
		JsonObject response = new JsonObject();
		JsonObject b = new JsonObject();
		b.addProperty("r", auth.rField);
		JsonObject o = new JsonObject();
		o.addProperty("statusId", 1);
		o.addProperty("_cmd", "login");
		JsonObject params = new JsonObject();
		params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
		params.addProperty("pendingFlags", (plr.account.isPlayerNew() ? 2 : 3));
		params.addProperty("activeLookId", plr.activeLook);
		params.addProperty("sanctuaryLookId", plr.activeSanctuaryLook);
		params.addProperty("sessionId", plr.account.getAccountID());
		params.addProperty("userId", plr.account.getAccountNumericID());
		params.addProperty("avatarInvId", 0);
		o.add("params", params);
		o.addProperty("status", 1);
		b.add("o", o);
		response.add("b", b);
		response.addProperty("t", "xt");
		sendPacket(client, response.toString());

		// Initial login
		System.out.println(
				"Player connected: " + plr.account.getLoginName() + " (as " + plr.account.getDisplayName() + ")");
		sendPacket(client, "%xt%ulc%-1%");
		players.add(plr);
	}

	// IP ban checks (both vpn block and ip banning)
	public static boolean isIPBanned(Socket socket, EmuFeralAccount acc, ArrayList<String> vpnIpsV4,
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
		// TODO Auto-generated method stub
		
		//get client's remote socket address
		//String address = socket.getRemoteSocketAddress().toString().substring(1).split(":")[0];
		String address = "185.62.206.20";
		
		ArrayList<String> vpnIpsToCheck = new ArrayList<String>();
		
		//is the address ipv4, or ipv6? we don't need to loop through both
		if(AddressChecker.isIPv4(address))
		{
			vpnIpsToCheck.addAll(vpnIpsV4);
		}
		else if(AddressChecker.isIPv6(address))
		{
			vpnIpsToCheck.addAll(vpnIpsV6);
		}
		
		for(String ipToCheckAgainst : vpnIpsToCheck)
		{
			IpAddressMatcher matcher = new IpAddressMatcher(ipToCheckAgainst);
			
			if(matcher.matches(address))
			{
				//VPN detected
				return true;
			}		
		}

		//no VPN
		return false;
	}

	// IP ban check
	private static boolean isIPBanned(Socket socket) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isBanned(EmuFeralAccount acc) {
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
			if (players.contains(plr)) {
				players.remove(plr);
				System.out.println("Player disconnected: " + plr.account.getLoginName() + " (was "
						+ plr.account.getDisplayName() + ")");
			}

			// Remove player character from all clients
			for (Player player : getPlayers()) {
				if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
					plr.destroyAt(player);
				}
			}

			// Disconnect the player from the chat server
			for (ChatClient chClient : EmuFeral.chatServer.getClients()) {
				if (chClient.getPlayer().getAccountID().equals(plr.account.getAccountID())) {
					chClient.disconnect();
				}
			}

			// Check maintenance, exit server if noone is online during maintenance
			if (maintenance && players.size() == 0) {
				// Exit
				System.exit(0);
			}
		}
	}

	@Override
	protected void onStart() {
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

}
