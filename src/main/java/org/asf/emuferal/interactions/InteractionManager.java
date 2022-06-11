package org.asf.emuferal.interactions;

import java.util.ArrayList;

import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

public class InteractionManager {

	/**
	 * Initializes the interactions for a specific level
	 * 
	 * @param client  Client to send the packets to
	 * @param levelID Level to find interactions for
	 */
	public static void initInteractionsFor(SmartfoxClient client, int levelID) {
		ArrayList<InteractionInitData> data = new ArrayList<InteractionInitData>();

		// TODO: proper implementation

		// Send init packet
		XtWriter packet = new XtWriter();
		packet.writeString("qs");
		packet.writeString("-1"); // data prefix
		packet.writeString("-1036"); // unknown
		packet.writeString("24"); // unknown
		packet.writeInt(data.size()); // count
		for (InteractionInitData ent : data) {
			packet.writeString(ent.interactableID);
			packet.writeInt(ent.interactionType);
			packet.writeInt(ent.interactionDefID);
		}
		packet.writeString(""); // data suffix
		client.sendPacket(packet.encode());

		// Send qcmd packets
		for (InteractionInitData ent : data) {
			if (ent.interactionType == 34) {
				// These need qcmd packets packet = new XtWriter();
				packet = new XtWriter();
				packet.writeString("qs");
				packet.writeString("-1"); // data prefix
				packet.writeString("1"); // unknown
				packet.writeString(ent.interactableID); // interaction ID
				packet.writeString("0"); // unknown
				packet.writeString("0"); // unknown
				packet.writeString("1"); // unknown
				packet.writeString(""); // data suffix
				client.sendPacket(packet.encode());
			}
		}

		// TODO: objectinfo for each entry, use defID 978 for interactable components

		levelID = levelID;
	}

}
