package org.asf.emuferal.packets.xt.gameserver.interactions;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.interactions.InteractionStartEvent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

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
		if (System.getProperty("debugMode") != null) {
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

		if (System.getProperty("debugMode") != null) {
			System.out.println("[INTERACTION] [START]  Server to client: " + build());
		}

		// TODO?
		return true;
	}

}
