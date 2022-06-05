package org.asf.emuferal.discord.handlers.game;

import org.asf.emuferal.discord.handlers.game.packets.FeedbackHandler;
import org.asf.emuferal.discord.handlers.game.packets.UserReportHandler;
import org.asf.emuferal.modules.eventbus.EventListener;
import org.asf.emuferal.modules.eventbus.IEventReceiver;
import org.asf.emuferal.modules.events.servers.GameServerStartupEvent;

public class GamePacketHandlers implements IEventReceiver {

	@EventListener
	public void gameStartup(GameServerStartupEvent event) {
		// Register packets
		event.registerPacket(new UserReportHandler());
		event.registerPacket(new FeedbackHandler());
	}

}
