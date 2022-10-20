package org.asf.centuria.interactions.groupobjects.spawnbehaviourproviders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.ObjectCollection;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.interactions.groupobjects.GroupObject;
import org.asf.centuria.util.RandomSelectorUtil;

public class RandomizedSpawnBehaviour implements ISpawnBehaviourProvider {

	private boolean loaded = false;
	private Random rnd = new Random();

	private HashMap<String, String> properties = new HashMap<String, String>();
	private HashMap<Integer, GroupObjectRotation> rotations = new HashMap<Integer, GroupObjectRotation>();

	// Tool to check if a object is not present in the rotation and if it doesnt
	// overlap
	private boolean isSafeToSpawn(GroupObject object, GroupObjectRotation rotation) {
		if (rotation.objects.stream().anyMatch(t -> t.id.equals(object.id)))
			return false; // Already present

		// Check overlap
		NetworkedObject cObject = NetworkedObjects.getObject(object.id);
		if (cObject.locationInfo != null && cObject.locationInfo.position != null) {
			for (GroupObject obj : rotation.objects) {
				NetworkedObject oObject = NetworkedObjects.getObject(obj.id);

				// Check coordinates
				if (oObject.locationInfo != null && oObject.locationInfo.position != null) {
					if (cObject.locationInfo.position.x < oObject.locationInfo.position.x + 5
							&& cObject.locationInfo.position.x > oObject.locationInfo.position.x + -5
							&& cObject.locationInfo.position.z < oObject.locationInfo.position.z + 5
							&& cObject.locationInfo.position.z > oObject.locationInfo.position.z + -5) {
						return false; // Overlap
					}
				}
			}
		}

		return true;
	}

	private boolean overlaps(GroupObject object, List<GroupObject> objects, int distance) {
		// Check overlap
		NetworkedObject cObject = NetworkedObjects.getObject(object.id);
		if (cObject.locationInfo != null && cObject.locationInfo.position != null) {
			for (GroupObject obj : objects) {
				NetworkedObject oObject = NetworkedObjects.getObject(obj.id);

				// Check coordinates
				if (oObject.locationInfo != null && oObject.locationInfo.position != null) {
					if (cObject.locationInfo.position.x < oObject.locationInfo.position.x + distance
							&& cObject.locationInfo.position.x > oObject.locationInfo.position.x + -distance
							&& cObject.locationInfo.position.z < oObject.locationInfo.position.z + distance
							&& cObject.locationInfo.position.z > oObject.locationInfo.position.z + -distance) {
						return true; // Overlap
					}
				}
			}
		}

		return false;
	}

	private static class GroupObjectRotation {
		public long time;
		public ArrayList<GroupObject> objects = new ArrayList<GroupObject>();
		public HashMap<String, String> mapping = new HashMap<String, String>();
	}

	@Override
	public String getID() {
		return "random";
	}

	@Override
	public GroupObject[] provideCurrent(int levelID, Player plr) {
		boolean update = true;

		// Load config
		if (!loaded) {
			try {
				// Create config if needed
				if (!new File("spawning.conf").exists()) {
					try {
						// Create config
						Files.writeString(Path.of("spawning.conf"),
								// Rotation interval
								"rotation-hours=3\n"

										// Minimal chest count
										+ "lockedchests-min=12\n"

										// Maximal chest count
										+ "lockedchests-max=15\n"

										// Digspot minimal count
										+ "digspots-min=6\n"

										// Digspot maximal count
										+ "digspots-max=8\n"

										// Minimal lockpick count
										+ "lockpicks-min=16\n"

										// Maximal lockpick count
										+ "lockpicks-max=19\n"

										// Minimal waystone path count
										+ "waystone-paths-min=3\n"

										// lockpicks waystone path count
										+ "waystone-paths-max=4\n");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				// Load properties
				for (String line : Files.readAllLines(Path.of("spawning.conf"))) {
					if (line.isEmpty() || line.startsWith("#"))
						continue;
					String key = line;
					String value = "";
					if (key.contains("=")) {
						value = key.substring(key.indexOf("=") + 1);
						key = key.substring(0, key.indexOf("="));
					}
					properties.put(key, value);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			loaded = true;
		}

		// Check rotation
		int rotationHours = Integer.parseInt(properties.getOrDefault("rotation-hours", "3"));
		if (rotations.containsKey(levelID)
				&& rotations.get(levelID).time + (rotationHours * 60 * 60 * 1000) > System.currentTimeMillis()) {
			update = false;
		}

		// Rotate if needed
		if (update) {
			// Retrieve all objects
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
			GroupObject[] objects = linearObjects.objects.keySet().stream().map(t -> {
				GroupObject obj = new GroupObject();
				obj.id = t;
				obj.type = oF.objects.get(t).subObjectInfo.type;
				return obj;
			}).toArray(t -> new GroupObject[t]);

			// Create rotation object
			GroupObjectRotation newRot = new GroupObjectRotation();
			newRot.time = System.currentTimeMillis();

			//
			// Spawn lockpicks
			//

			int minLockpicks = Integer.parseInt(properties.getOrDefault("lockpicks-min", "16"));
			int maxLockpicks = Integer.parseInt(properties.getOrDefault("lockpicks-max", "19"));
			int lockpicksToSpawn = rnd.nextInt(maxLockpicks + 1);
			while (lockpicksToSpawn < minLockpicks)
				lockpicksToSpawn = rnd.nextInt(maxLockpicks + 1);

			// Find lockpicks
			GroupObject[] lockpicks = Stream.of(objects).filter(t -> {
				NetworkedObject nObj = NetworkedObjects.getObject(t.id);
				if (nObj.primaryObjectInfo != null && nObj.primaryObjectInfo.type == 1
						&& nObj.primaryObjectInfo.defId == 6965)
					return true;
				return false;
			}).toArray(t -> new GroupObject[t]);

			// Select lockpicks
			int remainingLockpickObjects = lockpicks.length;
			int distanceBetweenLockpicks = (remainingLockpickObjects - lockpicksToSpawn);
			distanceBetweenLockpicks = (int) ((100d / (double) remainingLockpickObjects)
					* (double) distanceBetweenLockpicks) * 2;
			for (int i = 0; i < lockpicksToSpawn && remainingLockpickObjects > 0; i++) {
				while (true) {
					GroupObject lockpick = RandomSelectorUtil.selectRandom(Stream.of(lockpicks).toList());
					if (!overlaps(lockpick, newRot.objects, distanceBetweenLockpicks)
							&& isSafeToSpawn(lockpick, newRot)) {
						newRot.objects.add(lockpick);
						remainingLockpickObjects--;
						break;
					}
				}
			}

			//
			// Spawn waystones
			//

			int minWaystonePaths = Integer.parseInt(properties.getOrDefault("waystone-paths-min", "3"));
			int maxWaystonePaths = Integer.parseInt(properties.getOrDefault("waystone-paths-max", "4"));
			int waystonePathsToSpawn = rnd.nextInt(maxWaystonePaths + 1);
			while (waystonePathsToSpawn < minWaystonePaths)
				waystonePathsToSpawn = rnd.nextInt(maxWaystonePaths + 1);

			// Find waystone paths
			GroupObject[] waystonePaths = Stream.of(objects).filter(t -> {
				NetworkedObject nObj = NetworkedObjects.getObject(t.id);
				if (nObj.primaryObjectInfo != null && nObj.primaryObjectInfo.type == 1
						&& nObj.primaryObjectInfo.defId == 3579 && nObj.subObjectInfo != null
						&& nObj.subObjectInfo.type == 5 && nObj.subObjectInfo.defId == 0)
					return true;
				return false;
			}).toArray(t -> new GroupObject[t]);

			// Select waystone paths
			int remainingWaystonePaths = waystonePaths.length;
			int distanceWaystonePaths = (remainingWaystonePaths - waystonePathsToSpawn);
			distanceWaystonePaths = (int) ((100d / (double) remainingWaystonePaths) * (double) distanceWaystonePaths)
					/ 2;
			ArrayList<GroupObject> waystones = new ArrayList<GroupObject>();
			for (int i = 0; i < waystonePathsToSpawn && remainingWaystonePaths > 0; i++) {
				while (true) {
					GroupObject waystonePath = RandomSelectorUtil.selectRandom(Stream.of(waystonePaths).toList());
					if (!overlaps(waystonePath, waystones, distanceWaystonePaths)
							&& isSafeToSpawn(waystonePath, newRot)) {
						newRot.objects.add(waystonePath);
						remainingWaystonePaths--;
						waystones.add(waystonePath);
						break;
					}
				}
			}

			//
			// Spawn dig spots
			//

			int minDigspots = Integer.parseInt(properties.getOrDefault("digspot-min", "6"));
			int maxDigspots = Integer.parseInt(properties.getOrDefault("digspot-max", "8"));
			int digspotsToSpawn = rnd.nextInt(maxDigspots + 1);
			while (digspotsToSpawn < minDigspots)
				digspotsToSpawn = rnd.nextInt(maxDigspots + 1);

			// Find dig spots
			GroupObject[] digspots = Stream.of(objects).filter(t -> {
				NetworkedObject nObj = NetworkedObjects.getObject(t.id);
				if (nObj.primaryObjectInfo != null && nObj.primaryObjectInfo.type == 7
						&& nObj.primaryObjectInfo.defId == 0 && nObj.subObjectInfo != null
						&& nObj.subObjectInfo.type == 6 && nObj.subObjectInfo.defId == 0)
					return true;
				return false;
			}).toArray(t -> new GroupObject[t]);

			// Select dig spots
			int remainingDigspots = digspots.length;
			int distanceDigspots = (remainingDigspots - digspotsToSpawn);
			distanceDigspots = (int) ((100d / (double) remainingDigspots) * (double) distanceDigspots) / 2;
			ArrayList<GroupObject> spawnedDigspots = new ArrayList<GroupObject>();
			for (int i = 0; i < digspotsToSpawn && remainingDigspots > 0; i++) {
				while (true) {
					GroupObject digspot = RandomSelectorUtil.selectRandom(Stream.of(digspots).toList());
					if (!overlaps(digspot, spawnedDigspots, distanceDigspots) && isSafeToSpawn(digspot, newRot)) {
						newRot.objects.add(digspot);
						remainingDigspots--;
						spawnedDigspots.add(digspot);

						// Find related interactables
						for (String objId : linearObjects.objects.keySet()) {
							NetworkedObject nObj = linearObjects.objects.get(objId);

							// Check states
							for (ArrayList<StateInfo> states : nObj.stateInfo.values()) {
								boolean found = false;
								for (StateInfo state : states) {
									if (state.command.equals("35") && state.params.length == 2
											&& state.params[1].equals(digspot.id)) {
										found = true;
										break;
									}
								}
								if (found) {
									GroupObject obj = new GroupObject();
									obj.id = objId;
									obj.type = nObj.subObjectInfo.type;
									newRot.objects.add(obj);
								}
							}
						}

						break;
					}
				}
			}

			//
			// Spawn chests
			//

			int minChests = Integer.parseInt(properties.getOrDefault("lockedchests-min", "12"));
			int maxChests = Integer.parseInt(properties.getOrDefault("lockedchests-max", "15"));
			int chestsToSpawn = rnd.nextInt(maxChests + 1);
			while (chestsToSpawn < minChests)
				chestsToSpawn = rnd.nextInt(maxChests + 1);

			// Find chests
			GroupObject[] lockedChests = Stream.of(objects).filter(t -> {
				NetworkedObject nObj = NetworkedObjects.getObject(t.id);
				if (nObj.primaryObjectInfo != null && nObj.primaryObjectInfo.type == 1
						&& nObj.primaryObjectInfo.defId == 4984 && nObj.subObjectInfo != null
						&& nObj.subObjectInfo.type == 0 && nObj.subObjectInfo.defId == 0)
					return true;
				return false;
			}).toArray(t -> new GroupObject[t]);

			// Select chests
			int remainingChests = lockedChests.length;
			int distanceChests = (remainingChests - chestsToSpawn);
			distanceChests = (int) ((100d / (double) remainingChests) * (double) distanceChests) / 2;
			ArrayList<GroupObject> chests = new ArrayList<GroupObject>();
			for (int i = 0; i < chestsToSpawn && remainingChests > 0; i++) {
				while (true) {
					GroupObject chest = RandomSelectorUtil.selectRandom(Stream.of(lockedChests).toList());
					if (!overlaps(chest, chests, distanceChests) && isSafeToSpawn(chest, newRot)) {
						newRot.objects.add(chest);
						remainingChests--;
						chests.add(chest);

						// Okay tricky mess time
						// There is no way to know, other than via deduction, what interactions are
						// related
						// Usually its: interaction (main), use lockpick, gamesuccess
						// We are following that chain

						// Find index
						NetworkedObject obj = linearObjects.objects.get(chest.id);
						NetworkedObject[] objs = linearObjects.objects.values().toArray(t -> new NetworkedObject[t]);
						String[] keys = linearObjects.objects.keySet().toArray(t -> new String[t]);
						int i2 = 0;
						for (NetworkedObject o : objs) {
							if (o == obj)
								break;
							i2++;
						}

						// Find objects
						NetworkedObject useLockpick = objs[i2 + 1];
						if (!useLockpick.objectName.equals("Use Lockpick"))
							throw new RuntimeException(
									"Chest failed to load: " + chest.id + ", chart deduction was incorrect!");
						NetworkedObject gameSuccess = objs[i2 + 2];
						if (!gameSuccess.objectName.equals("GameSuccess"))
							throw new RuntimeException(
									"Chest failed to load: " + chest.id + ", chart deduction was incorrect!");

						// Add objects
						GroupObject gobj = new GroupObject();
						gobj.id = keys[i2 + 1];
						gobj.type = useLockpick.subObjectInfo.type;
						newRot.objects.add(gobj);
						gobj = new GroupObject();
						gobj.id = keys[i2 + 2];
						gobj.type = gameSuccess.subObjectInfo.type;
						newRot.objects.add(gobj);

						// Mapping
						newRot.mapping.put(chest.id, keys[i2 + 2]);

						break;
					}
				}
			}

			// Apply rotation
			if (!rotations.containsKey(levelID) || rotations.get(levelID).time < newRot.time)
				rotations.put(levelID, newRot);
		}

		// Return objects
		GroupObjectRotation rot = rotations.get(levelID);
		return rot.objects.stream().filter(t -> {
			// Check if its still valid in this rotation
			if (plr.account.getPlayerInventory().getInteractionMemory().hasTreasureBeenUnlocked(levelID,
					rot.mapping.getOrDefault(t.id, t.id))) {
				long lastUnlock = plr.account.getPlayerInventory().getInteractionMemory()
						.getLastTreasureUnlockTime(levelID, rot.mapping.getOrDefault(t.id, t.id));
				if (lastUnlock < System.currentTimeMillis() + (rotationHours * 60 * 60 * 1000) || lastUnlock < rot.time)
					return false;
			}
			return true;
		}).toArray(t -> new GroupObject[t]);
	}

	@Override
	public void onCollect(Player player, String id) {
		// Mark as collected
		player.account.getPlayerInventory().getInteractionMemory().unlocked(player.levelID, id);
	}

}
