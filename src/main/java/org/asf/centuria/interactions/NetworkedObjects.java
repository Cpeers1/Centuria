package org.asf.centuria.interactions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.interactions.dataobjects.LocationInfo;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.ObjectCollection;
import org.asf.centuria.interactions.dataobjects.ObjectInfo;
import org.asf.centuria.interactions.dataobjects.PositionInfo;
import org.asf.centuria.interactions.dataobjects.RotationInfo;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.modules.ICenturiaModule;
import org.asf.centuria.modules.ModuleManager;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.objects.ObjectDefinitionInitEvent;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NetworkedObjects {

	private static boolean isReady = false;

	private static HashMap<String, ObjectCollection> objects = new HashMap<String, ObjectCollection>();
	private static HashMap<String, ArrayList<String>> levelOverrideMap = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, ArrayList<String>> overrideMap = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, String> objectIdMap = new HashMap<String, String>();

	public static void init() {
		if (isReady)
			return;

		try {
			// Load the helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("networkedobjects.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
			strm.close();
			loadObjects(helper);

			// Load transformers
			strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("objecttransformers/index.json");
			JsonArray transformers = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();
			for (JsonElement el : transformers) {
				strm = InventoryItemDownloadPacket.class.getClassLoader()
						.getResourceAsStream("objecttransformers/" + el.getAsString());
				helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
				strm.close();
				loadObjects(helper);
			}

			// Load module transformers
			for (ICenturiaModule module : ModuleManager.getInstance().getAllModules()) {
				loadTransformers(module.getClass());
			}
		} catch (IOException e) {
			// This is very bad, should not start allow the server to continue otherwise
			// things will break HARD
			throw new RuntimeException(e);
		}

		// Dispatch event
		EventBus.getInstance()
				.dispatchEvent(new ObjectDefinitionInitEvent(objects, levelOverrideMap, overrideMap, objectIdMap));

		isReady = true;
	}

	private static void loadTransformers(Class<?> cls) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			InputStream strm = new URL(baseURL + "objecttransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				try {
					// Find the transformer document
					strm = new URL(baseURL + "objecttransformers/" + ele.getAsString()).openStream();
					JsonObject transformer = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
							.getAsJsonObject();
					strm.close();

					// Load transformer
					loadObjects(transformer);
				} catch (Exception e) {
					Centuria.logger.error(MarkerManager.getMarker("SHOPS"),
							"Transformer failed to load: " + ele.getAsString() + " (" + fileName + ")", e);
				}
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException)
				return;
			throw new RuntimeException(e);
		}
	}

	private static void loadObjects(JsonObject helper) {
		// Load all entries
		if (helper.has("Objects")) {
			JsonObject objectData = helper.get("Objects").getAsJsonObject();
			for (String objectId : objectData.keySet()) {
				JsonObject info = objectData.get(objectId).getAsJsonObject();
				ObjectCollection objectCol = new ObjectCollection();
				if (objects.containsKey(objectId))
					objectCol = objects.get(objectId);
				if (info.has("name"))
					objectCol.name = info.get("name").getAsString();
				if (info.has("objects")) {
					info = info.get("objects").getAsJsonObject();

					for (String uuid : info.keySet()) {
						// Deserialize entry
						JsonObject object = info.get(uuid).getAsJsonObject();

						NetworkedObject obj = new NetworkedObject();
						if (objectCol.objects.containsKey(uuid))
							obj = objectCol.objects.get(uuid);
						obj.containerId = objectId;
						if (object.has("objectName"))
							obj.objectName = object.get("objectName").getAsString();
						if (object.has("localType"))
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
							obj.subObjectInfo.defId = object.get("subObjectInfo").getAsJsonObject().get("defId")
									.getAsInt();
							obj.subObjectInfo.type = object.get("subObjectInfo").getAsJsonObject().get("type")
									.getAsInt();
						}

						if (object.has("locationInfo")) {
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
						}
						if (object.has("stateInfo")) {
							genBranches(object.get("stateInfo").getAsJsonObject(), obj.stateInfo);
						}

						// Add to collection
						objectCol.objects.put(uuid, obj);
						objectIdMap.put(uuid, objectId);
					}
				}

				// Add object to memory
				objects.put(objectId, objectCol);
			}
		}

		// Load level overrides
		if (helper.has("LevelOverrides")) {
			JsonObject overrides = helper.get("LevelOverrides").getAsJsonObject();
			for (String id : overrides.keySet()) {
				ArrayList<String> ids = new ArrayList<String>();
				for (JsonElement ele : overrides.get(id).getAsJsonArray()) {
					ids.add(ele.getAsString());
				}
				levelOverrideMap.put(id, ids);
			}
		}

		// Override map
		if (helper.has("Overrides")) {
			JsonObject overrides = helper.get("Overrides").getAsJsonObject();
			for (String id : overrides.keySet()) {
				ArrayList<String> ids = new ArrayList<String>();
				for (JsonElement ele : overrides.get(id).getAsJsonArray()) {
					ids.add(ele.getAsString());
				}
				overrideMap.put(id, ids);
			}
		}
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
	 * Retrieves level overrides for a specific level ID
	 * 
	 * @param levelId Level ID
	 * @return Array of override collection IDs
	 */
	public static String[] getOverridesFor(String levelId) {
		if (!levelOverrideMap.containsKey(levelId))
			return new String[0];
		return levelOverrideMap.get(levelId).toArray(t -> new String[t]);
	}

	/**
	 * Retrieves object collection IDs by LevelId
	 * 
	 * @param levelId Level ID to retrieve collections for
	 * @return Array of collection ID strings
	 */
	public static String[] getCollectionIdsForLevel(String levelId) {
		if (!levelOverrideMap.containsKey(levelId))
			return new String[0];

		ArrayList<String> ids = new ArrayList<String>();
		for (String override : levelOverrideMap.get(levelId)) {
			for (String id : getCollectionIdsForOverride(override))
				ids.add(id);
		}
		return ids.toArray(t -> new String[t]);
	}

	/**
	 * Retrieves object collection IDs by level override
	 * 
	 * @param id Override ID
	 * @return Array of collection ID strings
	 */
	public static String[] getCollectionIdsForOverride(String id) {
		if (!overrideMap.containsKey(id))
			return new String[0];

		return overrideMap.get(id).toArray(t -> new String[t]);
	}

	/**
	 * Retrieves object collections by ID
	 * 
	 * @param id Object collection ID
	 * @return ObjectCollection instance or null
	 */
	public static ObjectCollection getObjects(String id) {
		return objects.get(id);
	}

	/**
	 * Retrieves networked objects by their ID
	 * 
	 * @param id Object UUID
	 * @return NetworkedObject instance or null
	 */
	public static NetworkedObject getObject(String id) {
		if (!objectIdMap.containsKey(id) || !objects.containsKey(objectIdMap.get(id)))
			return null;
		return objects.get(objectIdMap.get(id)).objects.get(id);
	}

}
