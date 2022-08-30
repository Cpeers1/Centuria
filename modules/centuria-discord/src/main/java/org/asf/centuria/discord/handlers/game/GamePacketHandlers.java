package org.asf.centuria.discord.handlers.game;

import org.asf.centuria.discord.handlers.game.packets.FeedbackHandler;
import org.asf.centuria.discord.handlers.game.packets.UserReportHandler;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.eventbus.IEventReceiver;
import org.asf.centuria.modules.events.servers.GameServerStartupEvent;

public class GamePacketHandlers implements IEventReceiver {

	@EventListener
	public void gameStartup(GameServerStartupEvent event) {
		// Register packets
		event.registerPacket(new UserReportHandler());
		event.registerPacket(new FeedbackHandler());
	}

}
