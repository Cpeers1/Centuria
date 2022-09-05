package org.asf.centuria.discord.handlers.game;

import java.net.InetSocketAddress;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.UserAllowedIpUtils;
import org.asf.centuria.discord.UserIpBlockUtils;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.eventbus.IEventReceiver;
import org.asf.centuria.modules.events.chatcommands.ChatCommandEvent;
import org.asf.centuria.modules.events.chatcommands.ModuleCommandSyntaxListEvent;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

public class ModuleCommands implements IEventReceiver {

	@EventListener
	public void initCommands(ModuleCommandSyntaxListEvent ev) {
		// Add pair and unpair
		ev.addCommandSyntaxMessage("pairdiscord <pair-code>");
		ev.addCommandSyntaxMessage("transferdiscord <pair-code> [confirm]");
		ev.addCommandSyntaxMessage("2faipblock [list/unblock] [<ip-address>]");
		ev.addCommandSyntaxMessage("2faipwhitelist [list/revoke] [<ip-address>]");

		// Add moderator command for pulling user info
		if (ev.hasPermission("moderator")) {
			ev.addCommandSyntaxMessage("finddiscord \"<player>\"");
		}
	}

	@EventListener
	public void runCommand(ChatCommandEvent ev) {
		// Handle command
		switch (ev.getCommandID()) {
		case "pair": {
			ev.respond(
					"The generic pair command has been deprecated, use pairdiscord instead (same syntax as the legacy command)");
			break;
		}
		case "transferownership": {
			ev.respond(
					"The generic transferownership command has been deprecated, use v instead (same syntax as the legacy command)");
			break;
		}
		case "ipblock": {
			ev.respond(
					"The generic ipblock command has been deprecated, use 2faipblock instead (same syntax as the legacy command)");
			break;
		}
		case "ipwhitelist": {
			ev.respond(
					"The generic ipwhitelist command has been deprecated, use 2faipwhitelist instead (same syntax as the legacy command)");
			break;
		}
		case "pairdiscord": {
			if (ev.getCommandArguments().length < 1) {
				// Respond with error message
				ev.respond("Missing argument: pair-code");
				return;
			}

			// Check for a existing account connection
			if (LinkUtils.isPairedWithDiscord(ev.getAccount())) {
				// Respond with error message
				ev.respond("Error: this account has already been paired with a Discord account.");
				return;
			}

			// Load arguments
			String code = ev.getCommandArguments()[0];

			// Find user
			String userId = LinkUtils.useCode(code);
			if (userId == null) {
				// Respond with error message
				ev.respond("Error: invalid pairing code.");
				return;
			}

			// Check if the Discord user has already been paired
			if (LinkUtils.isPairedWithCenturia(userId)) {
				// Respond with error message
				ev.respond("Error: invalid pairing code.");
				return;
			}

			// Pair the account
			ev.respond("Attempting to pair accounts... Please wait...");
			LinkUtils.pairAccount(ev.getAccount(), userId,
					((InetSocketAddress) ev.getClient().getSocket().getRemoteSocketAddress()).getAddress()
							.getHostAddress(),
					true, false);
			User discordUser = DiscordBotModule.getClient().getUserById(Snowflake.of(userId)).block();
			ev.respond("Success! Current Centuria account has been paired with the Discord account "
					+ discordUser.getTag() + "!");

			break;
		}
		case "transferdiscord": {
			if (ev.getCommandArguments().length < 1) {
				// Respond with error message
				ev.respond("Missing argument: pair-code");
				return;
			}
			if (ev.getCommandArguments().length < 2 || !ev.getCommandArguments()[1].equals("confirm")) {
				// Respond with error message
				ev.respond(
						"Are you sure you wish to transfer ownership of this account to another Discord account?If you wish to proceed, run '>transferownership <code> confirm'");
				return;
			}

			// Check for existing account connection
			if (!ev.getAccount().getPlayerInventory().containsItem("pairedaccount")) {
				// Respond with error message
				ev.respond("Error: unpaired account.");
				return;
			}

			// Load arguments
			String code = ev.getCommandArguments()[0];

			// Find user
			String userId = LinkUtils.useCode(code);
			if (userId == null) {
				// Respond with error message
				ev.respond("Error: invalid pairing code.");
				return;
			}

			// Check if the Discord user has already been paired
			if (LinkUtils.isPairedWithCenturia(userId)) {
				// Respond with error message
				ev.respond("Error: invalid pairing code.");
				return;
			}

			// Remove connection
			ev.respond("Attempting to transfer ownership of the account... Please wait...");
			LinkUtils.unpairAccount(ev.getAccount(),
					((InetSocketAddress) ev.getClient().getSocket().getRemoteSocketAddress()).getAddress()
							.getHostAddress(),
					true);
			// Link with new owner
			LinkUtils.pairAccount(ev.getAccount(), userId,
					((InetSocketAddress) ev.getClient().getSocket().getRemoteSocketAddress()).getAddress()
							.getHostAddress(),
					true, true);
			User discordUser = DiscordBotModule.getClient().getUserById(Snowflake.of(userId)).block();
			ev.respond("Success! Current Centuria account has been paired with the Discord account "
					+ discordUser.getTag() + "!");
			break;
		}
		case "2faipblock": {
			// IP block utility

			// Verify arguments
			if (ev.getCommandArguments().length < 1) {
				// Respond with error message
				ev.respond("Missing argument: task");
				return;
			}

			// Parse arguments
			String task = ev.getCommandArguments()[0];
			if (!task.equalsIgnoreCase("list") && !task.equalsIgnoreCase("unblock")) {
				// Respond with error message
				ev.respond("Invalid argument: task: expected list or unblock");
				return;
			}

			// Handle task
			if (task.equalsIgnoreCase("list")) {
				// List blocked IP
				String[] ips = UserIpBlockUtils.getBlockedIps(ev.getAccount());
				if (ips.length == 0) {
					// Error
					ev.respond("You haven't blocked any IPs yet");
				} else {
					// Build list
					String message = "List of blocked IPs:";
					for (String ip : ips) {
						message += "\n - " + ip;
					}
					ev.respond(message);
				}
			} else {
				// Parse arguments
				if (ev.getCommandArguments().length < 2) {
					// Respond with error message
					ev.respond("Missing argument: ip-address");
					return;
				}

				String ip = ev.getCommandArguments()[1];

				// Verify IP
				if (!UserIpBlockUtils.isBlocked(ev.getAccount(), ip)) {
					// Respond with error message
					ev.respond("Invalid argument: ip-address: specified address has not been blocked");
					return;
				}

				UserIpBlockUtils.unblockIp(ev.getAccount(), ip);
				ev.respond("Success! IP " + ip + " has been unblocked!");
			}

			break;
		}
		case "2faipwhitelist": {
			// IP whitelist utility

			// Verify arguments
			if (ev.getCommandArguments().length < 1) {
				// Respond with error message
				ev.respond("Missing argument: task");
				return;
			}

			// Parse arguments
			String task = ev.getCommandArguments()[0];
			if (!task.equalsIgnoreCase("list") && !task.equalsIgnoreCase("revoke")) {
				// Respond with error message
				ev.respond("Invalid argument: task: expected list or revoke");
				return;
			}

			// Handle task
			if (task.equalsIgnoreCase("list")) {
				// List whitelisted IP
				String[] ips = UserAllowedIpUtils.getAllowedIps(ev.getAccount());
				if (ips.length == 0) {
					// Error
					ev.respond("You haven't whitelisted any IPs yet");
				} else {
					// Build list
					String message = "List of whitelisted IPs:";
					for (String ip : ips) {
						message += "\n - " + ip;
					}
					ev.respond(message);
				}
			} else {
				// Parse arguments
				if (ev.getCommandArguments().length < 2) {
					// Respond with error message
					ev.respond("Missing argument: ip-address");
					return;
				}

				String ip = ev.getCommandArguments()[1];

				// Verify IP
				if (!UserAllowedIpUtils.isAllowed(ev.getAccount(), ip)) {
					// Respond with error message
					ev.respond("Invalid argument: ip-address: specified address has not been whitelisted");
					return;
				}

				UserAllowedIpUtils.removeFromWhitelist(ev.getAccount(), ip);
				ev.respond("Success! IP " + ip + " has been removed from the whitelist!");
			}

			break;
		}

		case "finddiscord": {
			if (ev.hasPermission("moderator")) {
				// Check arguments
				if (ev.getCommandArguments().length < 1) {
					// Respond with error message
					ev.respond("Missing argument: player");
					return;
				}

				// Find player UUID
				String uuid = AccountManager.getInstance().getUserByDisplayName(ev.getCommandArguments()[0]);
				if (uuid == null) {
					// Respond with error message
					ev.respond("Invalid argument: player: player not recognized");
					return;
				}
				CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc == null) {
					// Respond with error message
					ev.respond("Invalid argument: player: player not recognized");
					return;
				}

				// Check account link
				String userID = LinkUtils.getDiscordAccountFrom(acc);
				if (userID == null) {
					// Respond with error message
					ev.respond("The specified account has not been paired with any Discord account.");
					return;
				}

				// Show account info
				String res = "Discord user ID: " + userID;
				try {
					res = "Discord user: "
							+ DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getTag();
				} catch (Exception e) {
				}
				ev.respond(res);
			}
			break;
		}
		}
	}

}
