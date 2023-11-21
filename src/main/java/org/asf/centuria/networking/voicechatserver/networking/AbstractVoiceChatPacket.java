package org.asf.centuria.networking.voicechatserver.networking;

import org.asf.centuria.networking.voicechatserver.VoiceChatClient;
import org.asf.centuria.networking.voicechatserver.VoiceChatServer;
import org.asf.centuria.networking.persistentservice.networking.AbstractPersistentServicePacket;

public abstract class AbstractVoiceChatPacket extends AbstractPersistentServicePacket<VoiceChatClient, VoiceChatServer> {

	/**
	 * Creates a new packet instance
	 * 
	 * @return AbstractVoiceChatPacket instance
	 */
	public abstract AbstractVoiceChatPacket instantiate();

	/**
	 * Handles the packet
	 */
	public abstract boolean handle(VoiceChatClient client);

}
