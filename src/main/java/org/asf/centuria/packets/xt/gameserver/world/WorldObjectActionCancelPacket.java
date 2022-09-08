package org.asf.centuria.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.interactions.InteractionCancelEvent;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectActionCancelPacket implements IXtPacket<WorldObjectActionCancelPacket> {

	private static final String PACKET_ID = "oac";

	private String target;

	@Override
	public WorldObjectActionCancelPacket instantiate() {
		return new WorldObjectActionCancelPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
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
		if (Centuria.debugMode) {
			System.out.println("[INTERACTION] [CANCELED] Client to server (target: " + target + ")");
		}

		// Load object
		NetworkedObject obj = NetworkedObjects.getObject(target);

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new InteractionCancelEvent(plr, target, obj));

		return true;
	}

}
