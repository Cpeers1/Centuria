package org.asf.centuria.interactions.groupobjects.spawnbehaviourproviders;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.ObjectCollection;
import org.asf.centuria.interactions.groupobjects.GroupObject;

public class FallbackSpawnBehaviour implements ISpawnBehaviourProvider {

	@Override
	public String getID() {
		return "fallback";
	}

	@Override
	public GroupObject[] provideCurrent(int levelID, Player plr) {
		ObjectCollection linearObjects = new ObjectCollection();
		for (String override : NetworkedObjects.getOverridesFor(Integer.toString(levelID))) {
			for (String col : NetworkedObjects.getCollectionIdsForOverride(override)) {
				ObjectCollection objects = NetworkedObjects.getObjects(col);
				if (objects.name.endsWith("_GroupLinearObjects")) {
					linearObjects = objects;
				}
			}
		}

		final ObjectCollection oF = linearObjects;
		return linearObjects.objects.keySet().stream().map(t -> {
			GroupObject obj = new GroupObject();
			obj.id = t;
			obj.type = oF.objects.get(t).subObjectInfo.type;
			return obj;
		}).toArray(t -> new GroupObject[t]);
	}

	@Override
	public void onCollect(Player player, String id) {
		// Not implemented
	}

}
