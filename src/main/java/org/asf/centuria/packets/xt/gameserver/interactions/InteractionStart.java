package org.asf.centuria.packets.xt.gameserver.interactions;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.interactions.InteractionStartEvent;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class InteractionStart implements IXtPacket<InteractionStart> {

	private static final String PACKET_ID = "oas";

	private String source;
	private String target;

	@Override
	public InteractionStart instantiate() {
		return new InteractionStart();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		target = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// TODO: verify this
		writer.writeInt(-1); // Data prefix

		writer.writeString(target);
		writer.writeString(source);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction start
		Player plr = (Player) client.container;
		source = plr.account.getAccountID();

		// log interaction details
		if (Centuria.debugMode) {
			System.out.println("[INTERACTION] [START]  Client to server (target: " + target + ")");
		}

		// Load object
		NetworkedObject obj = NetworkedObjects.getObject(target);
		if (obj == null)
			System.err.println("[INTERACTION] [START]  WARNING: Unrecognized object: " + target);

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new InteractionStartEvent(plr, target, obj));

		// Send response
		client.sendPacket(this);

		if (Centuria.debugMode) {
			System.out.println("[INTERACTION] [START]  Server to client: " + build());
		}

		// TODO?
		return true;
	}

}
