package org.asf.centuria.modules.events.quests;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.modules.quests.QuestDefinition;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;

/**
 * 
 * Quest start event, called when quests are started
 * 
 * @since Beta 1.8
 * @author Sky Swimmer
 * 
 */
public class QuestCompleteEvent extends AbstractQuestEvent {
	public QuestCompleteEvent(GameServer server, Player player, CenturiaAccount account, SmartfoxClient client,
			String questID, QuestDefinition quest) {
		super(server, player, account, client, questID, quest);
	}
}
