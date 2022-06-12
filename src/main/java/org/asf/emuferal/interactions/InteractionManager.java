package org.asf.emuferal.interactions;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

public class InteractionManager {

	/**
	 * Initializes the interactions for a specific level
	 * 
	 * @param client  Client to send the packets to
	 * @param levelID Level to find interactions for
	 */
	public static void initInteractionsFor(SmartfoxClient client, int levelID) {
		HashMap<String, NetworkedObject> data = new HashMap<String, NetworkedObject>();

		// Load object ids
		NetworkedObjects.init();
		ArrayList<String> ids = new ArrayList<String>();
		// Find level objects
		for (String id : NetworkedObjects.getCollectionIdsForLevel(Integer.toString(levelID))) {
			NetworkedObjects.getObjects(id).objects.keySet().forEach(t -> ids.add(t));
		}

		// TODO: quest objects

		// Add objects
		for (String id : ids) {
			data.put(id, NetworkedObjects.getObject(id));
		}

		// Send init packet
		XtWriter packet = new XtWriter();
		packet.writeString("qs");
		packet.writeString("-1"); // data prefix
		packet.writeString("-1036"); // unknown
		packet.writeString("24"); // unknown
		packet.writeInt(data.size()); // count
		for (String id : data.keySet()) {
			NetworkedObject ent = data.get(id);
			packet.writeString(id);
			packet.writeInt(ent.primaryObjectInfo.type);
			packet.writeInt(ent.primaryObjectInfo.defId);
		}
		packet.writeString(""); // data suffix
		client.sendPacket(packet.encode());

		// Send qcmd packets
		for (String id : data.keySet()) {
			NetworkedObject ent = data.get(id);
			if (ent.stateInfo.size() == 0) {
				// Set states
				packet = new XtWriter();
				packet.writeString("qcmd");
				packet.writeString("-1"); // data prefix
				packet.writeString("1"); // command
				packet.writeString(id); // interaction ID
				packet.writeString("0"); // unknown
				packet.writeString("0"); // unknown
				packet.writeString("1"); // unknown
				packet.writeString(""); // data suffix
				client.sendPacket(packet.encode());
			}
		}

		// Initialize objects
		for (String id : data.keySet()) {
			NetworkedObject ent = data.get(id);

			// Spawn object
			XtWriter wr = new XtWriter();
			wr.writeString("oi");
			wr.writeInt(-1); // data prefix

			// Object creation parameters
			wr.writeString(id); // World object ID
			wr.writeInt(978);
			wr.writeString(""); // Owner ID

			// Object info
			wr.writeInt(0);
			wr.writeLong(System.currentTimeMillis() / 1000);
			wr.writeDouble(ent.locationInfo.position.x);
			wr.writeDouble(ent.locationInfo.position.y);
			wr.writeDouble(ent.locationInfo.position.z);
			wr.writeDouble(ent.locationInfo.rotation.x);
			wr.writeDouble(ent.locationInfo.rotation.y);
			wr.writeDouble(ent.locationInfo.rotation.z);
			wr.writeDouble(ent.locationInfo.rotation.w);
			wr.add("0%0%0%0.0%0%0%0");
			wr.writeString(""); // data suffix
			client.sendPacket(wr.encode());
		}
	}

}
