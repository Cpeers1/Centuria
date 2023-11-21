package org.asf.centuria.modules.events.servers;

import java.util.function.Consumer;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.centuria.networking.voicechatserver.VoiceChatServer;
import org.asf.centuria.networking.voicechatserver.networking.AbstractVoiceChatPacket;

/**
 * 
 * VoiceChatServer Startup Event - used to handle startup of the voice chat
 * server and register packets via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("voicechatserver.startup")
public class VoiceChatServerStartupEvent extends EventObject {

	private VoiceChatServer server;
	private Consumer<AbstractVoiceChatPacket> registrationCommand;

	public VoiceChatServerStartupEvent(VoiceChatServer server, Consumer<AbstractVoiceChatPacket> registrationCommand) {
		this.server = server;
		this.registrationCommand = registrationCommand;
	}

	@Override
	public String eventPath() {
		return "voicechatserver.startup";
	}

	/**
	 * Retrieves the voice chat server
	 * 
	 * @return VoiceChatServer instance
	 */
	public VoiceChatServer getServer() {
		return server;
	}

	/**
	 * Registers packets on the voice chat server
	 * 
	 * @param packet Packet to register
	 */
	public void registerPacket(AbstractVoiceChatPacket packet) {
		registrationCommand.accept(packet);
	}

}
