package org.asf.centuria.discord.handlers.game.packets;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.ServerConfigUtils;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.entities.players.Player;

import com.google.gson.JsonObject;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.MessageCreateSpec;

public class FeedbackHandler implements IXtPacket<FeedbackHandler> {

	private String report;
	private String category;

	@Override
	public String id() {
		return "anf";
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find account
		CenturiaAccount reporter = ((Player) client.container).account;
		if (reporter != null) {
			// Build message content
			String message = "**Received a Feedback Report:**\n";
			message += "\n";
			message += "Report category: **" + category + "**\n";
			message += "Reporter: **" + reporter.getDisplayName()
					+ (LinkUtils.isPairedWithDiscord(reporter)
							? " (<@!" + LinkUtils.getDiscordAccountFrom(reporter) + ">)"
							: "")
					+ "**";

			// Send report
			for (Guild g : DiscordBotModule.getClient().getGuilds().toIterable()) {
				// Load guild
				String guildID = g.getId().asString();
				JsonObject config = ServerConfigUtils.getServerConfig(guildID);
				if (config.has("feedbackChannel")) {
					// Find channel
					String ch = config.get("feedbackChannel").getAsString();
					String srvMessage = message;
					if (config.has("developerRole")) {
						// Add ping
						srvMessage += "\n\n<@&" + config.get("developerRole").getAsString() + ">";
					}

					// Build message
					MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
					msg.content(srvMessage);
					msg.addFile("report.txt", new ByteArrayInputStream(report.getBytes("UTF-8")));
					msg.addComponent(ActionRow
							.of(Button.success("feedbackreply/" + reporter.getAccountID(), "Reply to feedback")));

					// Attempt to send message
					try {
						g.getChannelById(Snowflake.of(ch)).block().getRestChannel()
								.createMessage(msg.build().asRequest()).block();
					} catch (Exception e) {
					}
				}
			}
		}

		return true;
	}

	@Override
	public void build(XtWriter wr) throws IOException {
	}

	@Override
	public void parse(XtReader rd) throws IOException {
		category = rd.read();
		report = rd.readRemaining();
	}

	@Override
	public FeedbackHandler instantiate() {
		return new FeedbackHandler();
	}

}
