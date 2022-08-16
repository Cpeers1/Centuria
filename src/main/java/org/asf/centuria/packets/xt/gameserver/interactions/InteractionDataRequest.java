package org.asf.centuria.packets.xt.gameserver.interactions;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.interactions.InteractionDataRequestEvent;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;

public class InteractionDataRequest implements IXtPacket<InteractionDataRequest> {

	private static final String PACKET_ID = "oaskr";

	private String target;
	private int state;

	@Override
	public InteractionDataRequest instantiate() {
		return new InteractionDataRequest();
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

		if (System.getProperty("debugMode") != null) {
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