package org.asf.centuria.discord.handlers.game.packets;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.asf.centuria.players.Player;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.ServerConfigUtils;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

import com.google.gson.JsonObject;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.MessageCreateSpec;

public class UserReportHandler implements IXtPacket<UserReportHandler> {

	private String report;
	private String userID;
	private String category;

	@Override
	public String id() {
		return "fr";
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Send response
		client.sendPacket("%xt%fr%-1%true%");

		// Find account
		CenturiaAccount acc = AccountManager.getInstance().getAccount(userID);
		CenturiaAccount reporter = ((Player) client.container).account;
		if (acc != null) {
			// Build message content
			String message = "**Received a Player Report:**\n";
			message += "\n";
			message += "Report category: **" + category + "**\n";
			message += "Report subject: **" + acc.getDisplayName()
					+ (LinkUtils.isPairedWithDiscord(acc) ? " (<@!" + LinkUtils.getDiscordAccountFrom(acc) + ">)" : "")
					+ "**\n";
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
				if (config.has("reportChannel")) {
					// Find channel
					String ch = config.get("reportChannel").getAsString();
					String srvMessage = message;
					if (config.has("moderatorRole")) {
						// Add ping
						srvMessage += "\n\n<@&" + config.get("moderatorRole").getAsString() + ">";
					}

					// Build message
					MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
					msg.content(srvMessage);
					msg.addFile("report.txt", new ByteArrayInputStream(report.getBytes("UTF-8")));

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
		userID = rd.read();
		category = rd.read();
		report = rd.readRemaining();
	}

	@Override
	public UserReportHandler instantiate() {
		return new UserReportHandler();
	}

}
