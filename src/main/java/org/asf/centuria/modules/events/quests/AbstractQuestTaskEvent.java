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
 * Base type for all quest-task-related events
 * 
 * @since Beta 1.8
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class AbstractQuestTaskEvent extends AbstractQuestObjectiveEvent {

	private QuestTask task;

	public AbstractQuestTaskEvent(GameServer server, Player player, CenturiaAccount account, SmartfoxClient client,
			String questID, QuestDefinition quest, QuestObjective objective, QuestTask task) {
		super(server, player, account, client, questID, quest, objective);

		this.task = task;
	}

	/**
	 * Retrieves the quest task definition
	 * 
	 * @return QuestTask instance
	 */
	public QuestTask getTask() {
		return task;
	}

}
