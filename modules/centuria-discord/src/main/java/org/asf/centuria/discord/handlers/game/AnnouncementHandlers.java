package org.asf.centuria.discord.handlers.game;

import java.io.InputStream;
import java.net.URL;

import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.ServerConfigUtils;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.eventbus.IEventReceiver;
import org.asf.centuria.modules.events.updates.ServerUpdateEvent;

import com.google.gson.JsonObject;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;

public class AnnouncementHandlers implements IEventReceiver {

//	@EventListener
//	public void maintenanceStart(MaintenanceStartEvent event) {
//		announce("**" + DiscordBotModule.getServerName() + " Server Maintenance**\n"
//				+ "The Centuria servers are currently under maintenance, we will be back soon!");
//	}
//
//	@EventListener
//	public void maintenanceEnd(MaintenanceEndEvent event) {
//		announce("**" + DiscordBotModule.getServerName() + " Server Maintenance Ended**\n"
//				+ "Server maintenance has been complete and servers are coming back online!");
//	}

	@EventListener
	public void update(ServerUpdateEvent event) {
		// Build update message
		String messageSimple = "";
		String messageComplete = "";
		if (event.hasVersionInfo())
			messageSimple = "**" + DiscordBotModule.getServerName() + " " + event.getUpdateVersion() + "**!\n";
		else
			messageSimple = "**" + DiscordBotModule.getServerName() + " has been Updated!**\n";
		messageSimple += "\n";
		messageSimple += "Centuria has been updated, the servers "
				+ (event.hasTimer() ? "will be restarted in __" + event.getTimeRemaining() + " minutes__"
						: "are restarting")
				+ "!";

		// Add changelog to complete message if possible
		messageComplete = messageSimple;
		messageComplete += "\n";
		messageComplete += "\n";
		try {
			if (event.hasVersionInfo()) {
				messageComplete += "Changelog:\n";
				messageComplete += "```\n";
				InputStream strm = new URL("https://raw.githubusercontent.com/Cpeers1/Centuria/main/changelogs/"
						+ event.getUpdateVersion()).openStream();
				String log = new String(strm.readAllBytes(), "UTF-8");
				strm.close();
				messageComplete += log + "\n";
				messageComplete += "```";
			} else
				messageComplete = null;
		} catch (Exception e) {
			messageComplete = null;
		}

		// Select message
		if (messageComplete == null || messageComplete.length() > 2000)
			announce(messageSimple);
		else
			announce(messageComplete);
	}

	private void announce(String message) {
		// Send to all guild log channels
		for (Guild g : DiscordBotModule.getClient().getGuilds().toIterable()) {
			String guildID = g.getId().asString();
			JsonObject config = ServerConfigUtils.getServerConfig(guildID);
			if (config.has("announcementChannel")) {
				// Find channel
				String ch = config.get("announcementChannel").getAsString();
				String srvMessage = message;
				if (config.has("announcementPingRole")) {
					// Add ping
					srvMessage += "\n\n<@&" + config.get("announcementPingRole").getAsString() + ">";
				}

				// Attempt to send message
				try {
					g.getChannelById(Snowflake.of(ch)).block().getRestChannel().createMessage(srvMessage).block();
				} catch (Exception e) {
				}
			}
		}
	}

}
