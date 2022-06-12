package org.asf.emuferal.packets.xt.gameserver.interactions;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.interactions.InteractionCancelEvent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class InteractionCancel implements IXtPacket<InteractionCancel> {

	private String target;

	@Override
	public InteractionCancel instantiate() {
		return new InteractionCancel();
	}

	@Override
	public String id() {
		return "oac";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		target = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction cancel
		Player plr = (Player) client.container;

		// log interaction details
		if (System.getProperty("debugMode") != null) {
			System.out.println("[INTERACTION] [CANCELED] Client to server (target: " + target + ")");
		}

		// Load object
		NetworkedObject obj = NetworkedObjects.getObject(target);

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new InteractionCancelEvent(plr, target, obj));

		return true;
	}

}
