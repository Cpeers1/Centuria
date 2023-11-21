package org.asf.centuria.networking.voicechatserver;

import java.net.ServerSocket;
import java.net.Socket;

import org.asf.centuria.Centuria;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.servers.VoiceChatServerStartupEvent;
import org.asf.centuria.networking.persistentservice.BasePersistentServiceServer;
import org.asf.centuria.networking.voicechatserver.networking.*;

public class VoiceChatServer extends BasePersistentServiceServer<VoiceChatClient, VoiceChatServer> {

	public VoiceChatServer(ServerSocket socket) {
		super(socket, VoiceChatClient.class);
	}

	protected void registerPackets() {
		// Allow modules to register packets and to override existing packets
		VoiceChatServerStartupEvent ev = new VoiceChatServerStartupEvent(this, t -> registerPacket(t));
		EventBus.getInstance().dispatchEvent(ev);

		// Packet registry
		registerPacket(new PingPacket());
		registerPacket(new AddParticipantToCallPacket());
	}

	/**
	 * Retrieves chat clients by ID
	 * 
	 * @param accountID Account ID
	 * @return ChatClient instance or null
	 */
	public VoiceChatClient getClient(String accountID) {
		for (VoiceChatClient cl : getClients())
			if (cl.getPlayer().getAccountID().equals(accountID))
				return cl;
		return null;
	}

	@Override
	protected VoiceChatClient createClient(Socket clientSocket) {
		return new VoiceChatClient(clientSocket, this);
	}

	@Override
	protected void logDisconnect(VoiceChatClient client) {
		Centuria.logger
				.info("Player " + client.getPlayer().getDisplayName() + " disconnected from the voice chat server.");
	}

}
