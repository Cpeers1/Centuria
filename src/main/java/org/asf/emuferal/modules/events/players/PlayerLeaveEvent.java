package org.asf.emuferal.modules.events.players;

import java.net.InetSocketAddress;
import java.util.Map;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.players.Player;

/**
 * 
 * Player Leave Event - called when a player leaves the game
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("players.leave")
public class PlayerLeaveEvent extends EventObject {

	private Player player;
	private SmartfoxClient client;
	private EmuFeralAccount account;
	private GameServer server;

	public PlayerLeaveEvent(GameServer server, Player player, EmuFeralAccount account, SmartfoxClient client) {
		this.client = client;
		this.account = account;
		this.player = player;
		this.server = server;
	}

	@Override
	public String eventPath() {
		return "players.leave";
	}

	/**
	 * Retrieves the player client
	 * 
	 * @return SmartfoxCient instance
	 */
	public SmartfoxClient getClient() {
		return client;
	}

	/**
	 * Retrieves the player account
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

	/**
	 * Retrieves the player instance
	 * 
	 * @return Player instance
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Retrieves the game server
	 * 
	 * @return GameServer instance
	 */
	public GameServer getServer() {
		return server;
	}

	@Override
	public Map<String, String> eventProperties() {
		return Map.of("accountId", account.getAccountID(), "playerName", account.getDisplayName(), "accountName",
				account.getLoginName(), "address",
				((InetSocketAddress) client.getSocket().getRemoteSocketAddress()).getAddress().getHostAddress());
	}

}
