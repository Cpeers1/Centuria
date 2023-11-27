package org.asf.centuria.modules.events.quests;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.modules.quests.QuestDefinition;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;

/**
 * 
 * Base type for all quest-related events
 * 
 * @since Beta 1.8
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class AbstractQuestEvent extends EventObject {

	private Player player;
	private SmartfoxClient client;
	private CenturiaAccount account;
	private GameServer server;

	private String questID;
	private QuestDefinition quest;

	public AbstractQuestEvent(GameServer server, Player player, CenturiaAccount account, SmartfoxClient client,
			String questID, QuestDefinition quest) {
		this.client = client;
		this.account = account;
		this.player = player;
		this.server = server;

		this.questID = questID;
		this.quest = quest;
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

	/**
	 * Retrieves the quest ID
	 * 
	 * @return Quest ID string
	 */
	public String getQuestID() {
		return questID;
	}

	/**
	 * Retrieves the quest definition
	 * 
	 * @return QuestDefinition instance
	 */
	public QuestDefinition getQuest() {
		return quest;
	}

}
