package org.asf.centuria.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.interactions.InteractionDataRequestEvent;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectAskResponsePacket implements IXtPacket<WorldObjectAskResponsePacket> {

	private static final String PACKET_ID = "oaskr";

	private String target;
	private int state;

	@Override
	public WorldObjectAskResponsePacket instantiate() {
		return new WorldObjectAskResponsePacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
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

		if (Centuria.debugMode) {
			System.out.println(
					"[INTERACTION] [ASKRESPONSE] Client to server (target: " + target + ", state: " + state + ")");
		}

		// Find object
		NetworkedObject obj = NetworkedObjects.getObject(target);
		if (obj == null)
			return true;

		// Dispatch event
		InteractionDataRequestEvent req = new InteractionDataRequestEvent(plr, target, obj, state);
		EventBus.getInstance().dispatchEvent(req);
		if (req.isHandled())
			return true;

		// Handle request
		InteractionManager.handleInteractionDataRequest(plr, target, obj, state);
		return true;
	}

}
