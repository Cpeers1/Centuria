package org.asf.centuria.modules.events.quests;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.modules.quests.QuestDefinition;
import org.asf.centuria.interactions.modules.quests.QuestObjective;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;

/**
 * 
 * Base type for all quest-objective-related events
 * 
 * @since Beta 1.8
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class AbstractQuestObjectiveEvent extends AbstractQuestEvent {

	private QuestObjective objective;

	public AbstractQuestObjectiveEvent(GameServer server, Player player, CenturiaAccount account, SmartfoxClient client,
			String questID, QuestDefinition quest, QuestObjective objective) {
		super(server, player, account, client, questID, quest);

		this.objective = objective;
	}

	/**
	 * Retrieves the quest objective definition
	 * 
	 * @return QuestObjective instance
	 */
	public QuestObjective getObjective() {
		return objective;
	}

}
