package org.asf.emuferal.interactions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.emuferal.interactions.dataobjects.LocationInfo;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.interactions.dataobjects.ObjectInfo;
import org.asf.emuferal.interactions.dataobjects.PositionInfo;
import org.asf.emuferal.interactions.dataobjects.RotationInfo;
import org.asf.emuferal.interactions.dataobjects.StateInfo;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NetworkedObjects {

	private static HashMap<String, HashMap<String, NetworkedObject>> objects = new HashMap<String, HashMap<String, NetworkedObject>>();
	private static boolean isReady = false;

	public static void init() {
		if (isReady)
			return;

		try {
			// Load the helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("networkedobjects.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Objects").getAsJsonObject();
			strm.close();

			// Load all entries
			for (String levelID : helper.keySet()) {
				JsonObject info = helper.get(levelID).getAsJsonObject();
				for (String uuid : info.keySet()) {
					// Deserialize entry
					JsonObject object = info.get(uuid).getAsJsonObject();

					NetworkedObject obj = new NetworkedObject();
					obj.objectName = object.get("objectName").getAsString();
					obj.localType = object.get("localType").getAsInt();

					if (object.has("primaryObjectInfo")) {
						obj.primaryObjectInfo = new ObjectInfo();
						obj.primaryObjectInfo.defId = object.get("primaryObjectInfo").getAsJsonObject().get("defId")
								.getAsInt();
						obj.primaryObjectInfo.type = object.get("primaryObjectInfo").getAsJsonObject().get("type")
								.getAsInt();
					}
					if (object.has("subObjectInfo")) {
						obj.subObjectInfo = new ObjectInfo();
						obj.subObjectInfo.defId = object.get("subObjectInfo").getAsJsonObject().get("defId").getAsInt();
						obj.subObjectInfo.type = object.get("subObjectInfo").getAsJsonObject().get("type").getAsInt();
					}

					LocationInfo locationInfo = new LocationInfo();
					locationInfo.position = new PositionInfo();
					locationInfo.rotation = new RotationInfo();
					locationInfo.position.x = object.get("locationInfo").getAsJsonObject().get("position")
							.getAsJsonObject().get("x").getAsDouble();
					locationInfo.position.y = object.get("locationInfo").getAsJsonObject().get("position")
							.getAsJsonObject().get("y").getAsDouble();
					locationInfo.position.z = object.get("locationInfo").getAsJsonObject().get("position")
							.getAsJsonObject().get("z").getAsDouble();
					locationInfo.rotation.x = object.get("locationInfo").getAsJsonObject().get("rotation")
							.getAsJsonObject().get("x").getAsDouble();
					locationInfo.rotation.y = object.get("locationInfo").getAsJsonObject().get("rotation")
							.getAsJsonObject().get("y").getAsDouble();
					locationInfo.rotation.z = object.get("locationInfo").getAsJsonObject().get("rotation")
							.getAsJsonObject().get("z").getAsDouble();
					locationInfo.rotation.w = object.get("locationInfo").getAsJsonObject().get("rotation")
							.getAsJsonObject().get("w").getAsDouble();
					obj.locationInfo = locationInfo;
					genBranches(object.get("stateInfo").getAsJsonObject(), obj.stateInfo);

					// Add to collection
					if (!NetworkedObjects.objects.containsKey(levelID))
						NetworkedObjects.objects.put(levelID, new HashMap<String, NetworkedObject>());
					HashMap<String, NetworkedObject> objects = NetworkedObjects.objects.get(levelID);
					objects.put(uuid, obj);
				}
			}
		} catch (IOException e) {
		}

		isReady = true;
	}

	private static void genBranches(JsonObject branches, HashMap<String, ArrayList<StateInfo>> output) {
		// Parse branches
		for (String bId : branches.keySet()) {
			JsonArray ent = branches.get(bId).getAsJsonArray();
			ArrayList<StateInfo> states = new ArrayList<StateInfo>();
			output.put(bId, states);

			for (JsonElement ele2 : ent) {
				JsonObject entry = ele2.getAsJsonObject();
				StateInfo info = new StateInfo();
				info.command = entry.get("command").getAsString();
				info.actorId = entry.get("actorId").getAsString();
				ArrayList<String> params = new ArrayList<String>();
				for (JsonElement param : entry.get("params").getAsJsonArray()) {
					params.add(param.getAsString());
				}
				info.params = params.toArray(t -> new String[t]);
				states.add(info);
				if (entry.has("branches")) {
					genBranches(entry.get("branches").getAsJsonObject(), info.branches);
				}
			}
		}
	}

	/**
	 * Retrieves all networked objects for the given levelID
	 * 
	 * @param levelID Level ID to retrieve all objects for
	 * @return Array of networked object ID strings
	 */
	public static String[] getObjectsFor(String levelID) {
		if (!objects.containsKey(levelID))
			return null;

		return objects.get(levelID).keySet().toArray(t -> new String[t]);
	}

	/**
	 * Retrieves networked objects by their ID
	 * 
	 * @param levelID Level ID containing the object
	 * @param id      Object UUID
	 * @return NetworkedObject instance or null
	 */
	public static NetworkedObject getObject(String levelID, String id) {
		if (!objects.containsKey(levelID))
			return null;
		return objects.get(levelID).get(id);
	}

}
