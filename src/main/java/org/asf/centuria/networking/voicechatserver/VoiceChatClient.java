package org.asf.centuria.networking.voicechatserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.voicechat.VoiceChatLoginEvent;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.persistentservice.BasePersistentServiceClient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VoiceChatClient extends BasePersistentServiceClient<VoiceChatClient, VoiceChatServer> {

	private CenturiaAccount player;

	// Anti-hack
	public int banCounter = 0;

	// Room lock
	public boolean isReady = false;

	public VoiceChatClient(Socket client, VoiceChatServer server) {
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
	}

	@Override
	protected void runClient() throws IOException {
		// Read initial packet
		JsonObject handshakeStart = readRawPacket();

		// Check validity
		if (!handshakeStart.get("cmd").getAsString().equals("session.start")) {
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
		Thread.currentThread().setName("Voice Chat Client Thread: " + acc.getDisplayName() + " [ID "
				+ acc.getAccountID() + ", Address "
				+ ((InetSocketAddress) getSocket().getRemoteSocketAddress()).getAddress().getHostAddress() + "]");

		// Remove sensitive info and fire event
		handshakeStart.remove("auth_token");
		VoiceChatLoginEvent evt = new VoiceChatLoginEvent(getServer(), acc, this, handshakeStart);
		EventBus.getInstance().dispatchEvent(evt);
		if (evt.isCancelled()) {
			disconnect(); // Cancelled
			return;
		}

		// Check maintenance mode
		if (Centuria.gameServer.maintenance) {
			boolean lockout = true;

			// Check permissions
			if (acc.getSaveSharedInventory().containsItem("permissions")) {
				String permLevel = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
						.get("permissionLevel").getAsString();
				if (GameServer.hasPerm(permLevel, "admin")) {
					lockout = false;
				}
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

		// Disconnect connected instances
		for (VoiceChatClient cl : getServer().getClients())
			if (cl.getPlayer().getAccountID().equals(acc.getAccountID()) && cl != this)
				cl.disconnect();

		// Log the login attempt
		Centuria.logger
				.info("Voice chat login from IP: " + getSocket().getRemoteSocketAddress() + ": " + acc.getLoginName());

		// Save account in memory
		player = acc;
		Centuria.logger.info("Player " + getPlayer().getDisplayName() + " connected to the voice chat server.");

		// Send success
		JsonObject res = new JsonObject();
		res.addProperty("eventId", "session.start");
		res.addProperty("success", true);
		sendPacket(res);
	}

	/**
	 * Retrieves the connected player object
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getPlayer() {
		return player;
	}

}
