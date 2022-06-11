package org.asf.emuferal.packets.xt.gameserver.interactions;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class InteractionDataRequest implements IXtPacket<InteractionDataRequest> {

	private String target;
	private int state;

	@Override
	public InteractionDataRequest instantiate() {
		return new InteractionDataRequest();
	}

	@Override
	public String id() {
		return "oaskr";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		target = reader.read();
		state = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction data request
		Player plr = (Player) client.container;

		if (System.getProperty("debugMode") != null) {
			System.out.println(
					"[INTERACTION] [ASKRESPONSE] Client to server (target: " + target + ", state: " + state + ")");
		}

		// Find object
		int id = plr.roomID;
		if (!plr.roomReady)
			id = plr.pendingRoomID;
		NetworkedObject obj = NetworkedObjects.getObject(Integer.toString(id), target);
		if (obj == null)
			return true;
		
		// TODO
		return true;
	}

}
