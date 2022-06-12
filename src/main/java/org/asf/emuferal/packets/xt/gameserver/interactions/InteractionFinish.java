package org.asf.emuferal.packets.xt.gameserver.interactions;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.InteractionManager;
import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.interactions.dataobjects.StateInfo;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.interactions.InteractionSuccessEvent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class InteractionFinish implements IXtPacket<InteractionFinish> {

	private String target;
	private int currentState;

	@Override
	public InteractionFinish instantiate() {
		return new InteractionFinish();
	}

	@Override
	public String id() {
		return "oaf";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		target = reader.read();
		currentState = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction finish
		Player plr = (Player) client.container;

		if (System.getProperty("debugMode") != null) {
			System.out.println("[INTERACTION] [FINISH] Client to server (target: " + target + ")");
		}

		// Find object
		NetworkedObject obj = NetworkedObjects.getObject(target);
		if (obj == null)
			return true;

		// Build response
		XtWriter pk = new XtWriter();
		pk.writeString("oaf");
		pk.writeInt(-1); // Data prefix
		pk.writeString(target); // Interactable
		pk.writeInt(obj.primaryObjectInfo.type); // Type
		pk.writeString("2");
		pk.writeString(""); // Data suffix
		client.sendPacket(pk.encode());

		// Send qcmd
		if (obj.stateInfo.containsKey(Integer.toString(currentState))) {
			ArrayList<StateInfo> states = obj.stateInfo.get(Integer.toString(currentState));
			for (StateInfo st : states) {
				// Build quest command
				pk = new XtWriter();
				pk.writeString("qcmd");
				pk.writeInt(-1); // Data prefix
				pk.writeString(st.command); // command
				pk.writeInt(1); // State
				pk.writeString(target); // Interactable
				pk.writeInt(0); // Position
				// Parameters
				for (String param : st.params)
					pk.writeString(param);
				pk.writeString(""); // Data suffix
				client.sendPacket(pk.encode());
			}
		}

		// Dispatch event
		InteractionSuccessEvent ev = new InteractionSuccessEvent(plr, target, obj, currentState);
		EventBus.getInstance().dispatchEvent(ev);
		if (ev.isHandled())
			return true;

		// Handle interaction
		InteractionManager.handleInteraction(plr, target, obj, currentState);

		return true;
	}

}
