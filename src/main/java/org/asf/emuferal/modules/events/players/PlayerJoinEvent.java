package org.asf.emuferal.modules.events.players;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.players.Player;

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
	private EmuFeralAccount account;
	private GameServer server;

	public PlayerJoinEvent(GameServer server, Player player, EmuFeralAccount account, SmartfoxClient client) {
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

}
