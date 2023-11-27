package org.asf.centuria.interactions.modules;

import java.util.List;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.interactions.InteractionStateChangeEvent;
import org.asf.centuria.modules.events.interactions.InteractionWorldSetupEvent;

public class EventsModule extends InteractionModule {

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
		EventBus.getInstance().dispatchEvent(new InteractionWorldSetupEvent(player, levelID, ids));
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		return false;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
		return false;
	}

	@Override
	public void onStateChange(Player player, String objectID, NetworkedObject object, int oldState, int newState) {
		EventBus.getInstance()
				.dispatchEvent(new InteractionStateChangeEvent(player, objectID, object, oldState, newState));
	}

}
