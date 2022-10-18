package org.asf.centuria.interactions.groupobjects.spawnbehaviourproviders;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.groupobjects.GroupObject;

public class RandomizedSpawnBehaviour implements ISpawnBehaviourProvider {

	public static HashMap<Integer, GroupObjectRotation> rotations = new HashMap<Integer, GroupObjectRotation>(); 

	private static class GroupObjectRotation {
		public long time;
		public ArrayList<GroupObject> objects = new ArrayList<GroupObject>();
	}

	@Override
	public String getID() {
		return "random";
	}

	@Override
	public GroupObject[] provideCurrent(int levelID, Player plr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCollect(Player player, String id) {
		// TODO Auto-generated method stub
	}

}
