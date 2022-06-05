package org.asf.emuferal.discord.handlers.api;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.discord.DiscordBotModule;
import org.asf.emuferal.discord.LinkUtils;
import org.asf.emuferal.discord.TimedActions;
import org.asf.emuferal.discord.UserIpBlockUtils;
import org.asf.emuferal.modules.eventbus.EventListener;
import org.asf.emuferal.modules.eventbus.IEventReceiver;
import org.asf.emuferal.modules.events.servers.APIServerStartupEvent;
import org.asf.rats.processors.HttpUploadProcessor;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.MessageCreateSpec;

public class ForgotPasswordHandler extends HttpUploadProcessor implements IEventReceiver {

	@Override
	public void process(String contentType, Socket client, String method) {
		// Parse account name
		String path = this.getRequestPath().substring(path().length());
		if (path.isEmpty()) {
			setResponseMessage("Bad Request");
			setResponseCode(400);
			return;
		}
		path = path.substring(1);
		String loginName;
		try {
			loginName = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			setResponseMessage("Bad Request");
			setResponseCode(400);
			return;
		}

		// Find account
		String uuid = AccountManager.getInstance().getUserByLoginName(loginName);
		if (uuid == null) {
			setResponseMessage("Not Found");
			setResponseCode(404);
			return;
		}

		// Check link
		EmuFeralAccount account = AccountManager.getInstance().getAccount(uuid);
		if (LinkUtils.isPairedWithDiscord(account)) {
			// Check IP block
			if (UserIpBlockUtils.isBlocked(account,
					((InetSocketAddress) client.getRemoteSocketAddress()).getAddress().getHostAddress())) {
				// Deny access
				setResponseMessage("Forbidden");
				setResponseCode(403);
				return;
			}

			// Find discord user ID
			String userID = LinkUtils.getDiscordAccountFrom(account);

			// Send DM
			try {
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

				// Message content
				String message = "**Received account password reset request.**\n";
				message += "\n";
				message += "A account password reset request was just made, here follow the details:\n";
				message += "**Account login name:** `" + account.getLoginName() + "`\n";
				message += "**Ingame player name:** `" + account.getDisplayName() + "`\n";
				message += "**Requested from IP:** `"
						+ ((InetSocketAddress) client.getRemoteSocketAddress()).getAddress().getHostAddress() + "`\n";
				message += "**Last login time:** "
						+ (account.getLastLoginTime() == -1 ? "`Unknown`" : "<t:" + account.getLastLoginTime() + ">")
						+ "\n";
				message += "**Requested at:** <t:" + System.currentTimeMillis() / 1000 + ">\n";
				message += "\n";
				message += "This request is only valid for 5 minutes.\n";
				message += "\n";
				message += "When clicking `Reset password`, the account password will be unlocked and the next login will be saved.\n";
				message += "__Please do not press reset until you started the Fer.al client, otherwise it might get abused.__\n";
				message += "\n";
				message += "If you did not request this change, please block it to prevent further changes from the IP this request was made from.";
				msg.content(message);

				// Schedule reset action
				String code = TimedActions.addAction(uuid + "-forgotpassword", () -> {
					// Release password lock
					AccountManager.getInstance().makePasswordUpdateRequested(uuid);
				}, 5 * 60);

				// Buttons
				msg.addComponent(ActionRow.of(
						Button.danger("confirmresetpassword/" + userID + "/" + code, "Reset password"),
						Button.danger("confirmblockip/" + userID + "/" + account.getAccountID() + "/"
								+ ((InetSocketAddress) client.getRemoteSocketAddress()).getAddress().getHostAddress(),
								"Block IP and Dismiss"),
						Button.primary("dismiss", "Dismiss")));

				// Send response
				DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getPrivateChannel().block()
						.createMessage(msg.build()).subscribe();
			} catch (Exception e) {
			}
		}
	}

	@EventListener
	public void startAPI(APIServerStartupEvent event) {
		event.getServer().registerProcessor(this);
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new ForgotPasswordHandler();
	}

	@Override
	public String path() {
		return "/a/reset_password_request";
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
