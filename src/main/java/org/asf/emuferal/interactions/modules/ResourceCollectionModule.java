package org.asf.emuferal.interactions.modules;

import java.util.List;

import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.players.Player;

public class ResourceCollectionModule extends InteractionModule {

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
		// TODO Auto-generated method stub
		ids = ids;
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean shouldDestroyResource(Player player, String id, NetworkedObject object, int state,
			boolean destroyOnCompletion) {
		// TODO Auto-generated method stub
		return destroyOnCompletion;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handleInteractionDataRequest(Player player, String id, NetworkedObject object, int state) {
		// TODO Auto-generated method stub
		return false;
	}

}
