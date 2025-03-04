package org.asf.centuria.modules.events.objects;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.centuria.interactions.dataobjects.ObjectCollection;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Object Definition Init Event - called when the NetworkedObjects manager is
 * being initialized
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("objects.init")
public class ObjectDefinitionInitEvent extends EventObject {

	private HashMap<String, ObjectCollection> objects;
	private HashMap<String, ArrayList<String>> levelOverrideMap;
	private HashMap<String, ArrayList<String>> overrideMap;
	private HashMap<String, String> objectIdMap;

	public ObjectDefinitionInitEvent(HashMap<String, ObjectCollection> objects,
			HashMap<String, ArrayList<String>> levelOverrideMap, HashMap<String, ArrayList<String>> overrideMap,
			HashMap<String, String> objectIdMap) {
		this.objects = objects;
		this.levelOverrideMap = levelOverrideMap;
		this.overrideMap = overrideMap;
		this.objectIdMap = objectIdMap;
	}

	@Override
	public String eventPath() {
		return "objects.init";
	}

	/**
	 * Adds overrides IDs to specific level IDs
	 * 
	 * @param levelID    Level ID to add the given override to
	 * @param overrideID Override ID
	 */
	public void defineLevelOverride(String levelID, String overrideID) {
		if (!levelOverrideMap.containsKey(levelID))
			levelOverrideMap.put(levelID, new ArrayList<String>());
		if (!levelOverrideMap.get(levelID).contains(overrideID))
			levelOverrideMap.get(levelID).add(overrideID);
	}

	/**
	 * Adds object collection IDs to specific overrides (requires definition of the
	 * given object collection)
	 * 
	 * @param overrideID Override ID
	 * @param objectID   Object collection ID
	 */
	public void defineCollectionPath(String overrideID, String objectID) {
		if (!objects.containsKey(objectID))
			return;
		if (!overrideMap.containsKey(overrideID))
			overrideMap.put(overrideID, new ArrayList<String>());
		if (!levelOverrideMap.get(overrideID).contains(objectID))
			levelOverrideMap.get(overrideID).add(objectID);
	}

	/**
	 * Defines object collections (appends to the existing collection if present)
	 * 
	 * @param objectID Object collection ID
	 * @param col      ObjectCollection instance
	 */
	public void defineObjectCollection(String objectID, ObjectCollection col) {
		if (objects.containsKey(objectID)) {
			ObjectCollection o = objects.get(objectID);
			o.name = col.name;
			o.objects.putAll(col.objects);
			col = o;
		} else {
			objects.put(objectID, col);
		}

		col.objects.forEach((id, obj) -> {
			objectIdMap.put(id, obj.containerId);
		});
	}

}
