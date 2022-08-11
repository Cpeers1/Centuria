package org.asf.centuria.modules.events.players;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.players.Player;

/**
 * 
 * Player Join Event - called when a client successfully logs into the server
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("players.join")
public class PlayerJoinEvent extends EventObject {

	private Player player;
	private SmartfoxClient client;
	private CenturiaAccount account;
	private GameServer server;

	public PlayerJoinEvent(GameServer server, Player player, CenturiaAccount account, SmartfoxClient client) {
		this.client = client;
		this.account = account;
		this.player = player;
		this.server = server;
	}

	@Override
	public String eventPath() {
		return "players.join";
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
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
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

}
