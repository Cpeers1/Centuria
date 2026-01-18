package org.asf.centuria.modules.events.quests;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.modules.quests.QuestDefinition;
import org.asf.centuria.interactions.modules.quests.QuestObjective;
import org.asf.centuria.interactions.modules.quests.QuestTask;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;

/**
 * 
 * Quest objective completed event - dispatched when tasks are completed
 * 
 * @author Sky Swimmer
 * 
 */
public class QuestTaskCompletedEvent extends AbstractQuestTaskEvent {

	public QuestTaskCompletedEvent(GameServer server, Player player, CenturiaAccount account, SmartfoxClient client,
			String questID, QuestDefinition quest, QuestObjective objective, QuestTask task) {
		super(server, player, account, client, questID, quest, objective, task);
	}

}
