package org.asf.centuria.discord.handlers.game;

import java.net.InetSocketAddress;

import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.TimedActions;
import org.asf.centuria.discord.UserAllowedIpUtils;
import org.asf.centuria.discord.UserIpBlockUtils;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.eventbus.IEventReceiver;
import org.asf.centuria.modules.events.accounts.AccountLoginEvent;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;

public class LoginEventHandler implements IEventReceiver {

	// Login info container
	private static class LoginInfoContainer {
		public boolean confirmed = false;
		public boolean rejected = false;
	}

	@EventListener
	public void login(AccountLoginEvent event) {
		// Load IP into a string object
		String ip = ((InetSocketAddress) event.getClient().getSocket().getRemoteSocketAddress()).getAddress()
				.getHostAddress();

		// Check blocked IPs
		if (UserIpBlockUtils.isBlocked(event.getAccount(), ip)) {
			// Mark invalid login
			event.setStatus(-10);
			return;
		}

		// 2fa (if enabled)
		if (LinkUtils.isPairedWithDiscord(event.getAccount())
				&& event.getAccount().getPlayerInventory().containsItem("accountoptions")
				&& event.getAccount().getPlayerInventory().getItem("accountoptions").getAsJsonObject().has("enable2fa")
				&& event.getAccount().getPlayerInventory().getItem("accountoptions").getAsJsonObject().get("enable2fa")
						.getAsBoolean()) {
			// Check whitelist
			if (!UserAllowedIpUtils.isAllowed(event.getAccount(), ip) || ip.equals("127.0.0.1")) {
				try {
					// Find account
					String userId = LinkUtils.getDiscordAccountFrom(event.getAccount());
					User owner = DiscordBotModule.getClient().getUserById(Snowflake.of(userId)).block();

					// Build DM message
					MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

					// Build content
					String content = "**Received a Centuria login request**\n";
					content += "Someone is attempting to log into your Centuria account.\n";
					content += "Since you have enabled 2-factor authentication, Centuria has contacted you to verify the login attempt.\n";
					content += "\n";
					content += "\n";
					content += "Here follow the details of the login attempt:\n";
					content += "**Account login name:** `" + event.getAccount().getLoginName() + "`\n";
					content += "**Ingame player name:** `" + event.getAccount().getDisplayName() + "`\n";
					if (!ip.equals("127.0.0.1"))
						content += "**Requested from IP:** `" + ip + "`\n";
					content += "**Last login time:** " + (event.getAccount().getLastLoginTime() == -1 ? "`Unknown`"
							: "<t:" + event.getAccount().getLastLoginTime() + ">") + "\n";
					content += "**Requested at:** <t:" + System.currentTimeMillis() / 1000 + ">\n";
					content += "\n";
					content += "Login will be withheld for up to 15 minutes, if the attempt is not confirmed before that time, the login attempt will be aborted.\n";
					content += "\n";
					content += "\n";
					content += "If you are not attempting to log into your Centuria account, you can reject the login below by selecting a reject option.";
					msg.content(content);

					// Confirmation container
					LoginInfoContainer cont = new LoginInfoContainer();
					Object oldCont = event.getClient().container;
					event.getClient().container = cont;
					int i = 0;
					int boundary = (15 * 60);

					// Build actions
					String accID = event.getAccount().getAccountID();
					String codeAllow = TimedActions.addAction(accID + "-2fa-allow", () -> {
						// Allow login
						cont.confirmed = true;

						// Expire codes
						TimedActions.expire(accID + "-2fa-allow");
						TimedActions.expire(accID + "-2fa-whitelist");
						TimedActions.expire(accID + "-2fa-reject");
						TimedActions.expire(accID + "-2fa-reject-block");
					}, 15 * 60);
					String codeWhitelist = TimedActions.addAction(accID + "-2fa-whitelist", () -> {
						// Allow login
						cont.confirmed = true;

						// Whitelist IP
						UserAllowedIpUtils.whitelistIp(event.getAccount(), ip);

						// Expire codes
						TimedActions.expire(accID + "-2fa-allow");
						TimedActions.expire(accID + "-2fa-whitelist");
						TimedActions.expire(accID + "-2fa-reject");
						TimedActions.expire(accID + "-2fa-reject-block");
					}, 15 * 60);
					String codeReject = TimedActions.addAction(accID + "-2fa-reject", () -> {
						// Reject
						cont.rejected = true;

						// Expire codes
						TimedActions.expire(accID + "-2fa-allow");
						TimedActions.expire(accID + "-2fa-whitelist");
						TimedActions.expire(accID + "-2fa-reject");
						TimedActions.expire(accID + "-2fa-reject-block");
					}, 15 * 60);
					String codeRejectBlock = TimedActions.addAction(accID + "-2fa-reject-block", () -> {
						// Reject
						cont.rejected = true;

						// Block IP
						UserIpBlockUtils.blockIp(event.getAccount(), ip);

						// Expire codes
						TimedActions.expire(accID + "-2fa-allow");
						TimedActions.expire(accID + "-2fa-whitelist");
						TimedActions.expire(accID + "-2fa-reject");
						TimedActions.expire(accID + "-2fa-reject-block");
					}, 15 * 60);

					// Buttons
					if (!ip.equals("127.0.0.1"))
						msg.addComponent(ActionRow.of(
								Button.success("confirmallow2fa/" + userId + "/" + codeAllow, "Confirm login"),
								Button.success("confirmwhitelistip2fa/" + userId + "/" + codeWhitelist,
										"Always allow from this IP"),
								Button.danger("confirmdeny2fa/" + userId + "/" + codeReject, "Reject login"),
								Button.danger("confirmblock2fa/" + userId + "/" + codeRejectBlock,
										"Block IP & Reject")));
					else
						msg.addComponent(ActionRow.of(
								Button.success("confirmallow2fa/" + userId + "/" + codeAllow, "Confirm login"),
								Button.danger("confirmdeny2fa/" + userId + "/" + codeReject, "Reject login")));

					// Send DM
					owner.getPrivateChannel().block().createMessage(msg.build()).subscribe();

					// Wait for confirmation
					while (!cont.confirmed && event.getClient().getSocket() != null) {
						if (i >= boundary || cont.rejected) {
							// Cancel login
							event.getClient().container = oldCont;
							event.setStatus(-10);
							return;
						}

						if (i % 10 == 0) {
							// Keep-alive
							event.getClient().sendPacket("%xt%ka%-1%");
						}

						Thread.sleep(1000);
						i++;
					}

					// Login was confirmed
					event.getClient().container = oldCont;
				} catch (Exception e) {
					event.setStatus(-10);
					return;
				}
			}
		}
	}

}
