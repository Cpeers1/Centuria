package org.asf.centuria.interactions.modules.linearobjects;

import java.util.List;
import java.util.stream.Stream;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.modules.InteractionModule;

public class LockpickItemModule extends InteractionModule {

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		if (object.primaryObjectInfo != null && object.primaryObjectInfo.type == 1
				&& object.primaryObjectInfo.defId == 6965) {
			// This is a lockpick, check if its valid at this time
			return Stream.of(InteractionManager.getActiveSpawnBehaviour().provideCurrent(player.levelID))
					.anyMatch(t -> t.id.equals(id));
		}
		return false;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
		// Add lockpick
		player.account.getPlayerInventory().getCurrencyAccessor().addLockpicks(player.client, 1);
		
		// TODO: respawning?
		return false;
	}

}
