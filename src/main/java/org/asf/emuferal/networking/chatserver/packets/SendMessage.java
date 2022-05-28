package org.asf.emuferal.networking.chatserver.packets;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.packets.xt.gameserver.world.WorldReadyPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;

public class SendMessage extends AbstractChatPacket {

	private String message;
	private String room;

	@Override
	public String id() {
		return "chat.postMessage";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new SendMessage();
	}

	@Override
	public void parse(JsonObject data) {
		message = data.get("message").getAsString();
		room = data.get("conversationId").getAsString();
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {
		// Chat commands
		if (message.startsWith(">")) {
			String cmd = message.substring(1).trim();
			if (handleCommand(cmd, client))
				return true;
		}

		// Check mute
		EmuFeralAccount acc = client.getPlayer();
		if (acc.getPlayerInventory().containsItem("penalty") && acc.getPlayerInventory().getItem("penalty")
				.getAsJsonObject().get("type").getAsString().equals("mute")) {
			JsonObject banInfo = acc.getPlayerInventory().getItem("penalty").getAsJsonObject();
			if (banInfo.get("unmuteTimestamp").getAsLong() == -1
					|| banInfo.get("unmuteTimestamp").getAsLong() > System.currentTimeMillis()) {
				return true; // ignore chat
			}
		}

		if (client.isInRoom(room)) {
			// Send response
			JsonObject res = new JsonObject();
			res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
			res.addProperty("conversationId", room);
			res.addProperty("message", message);
			res.addProperty("source", client.getPlayer().getAccountID());
			SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			res.addProperty("sentAt", fmt.format(new Date()));
			res.addProperty("eventId", "chat.postMessage");
			res.addProperty("success", true);

			// Send to all in room
			for (ChatClient cl : client.getServer().getClients()) {
				if (cl.isInRoom(room))
					cl.sendPacket(res);
			}
		}

		return true;
	}

	// Command parser
	private ArrayList<String> parseCommand(String args) {
		ArrayList<String> args3 = new ArrayList<String>();
		char[] argarray = args.toCharArray();
		boolean ignorespaces = false;
		String last = "";
		int i = 0;
		for (char c : args.toCharArray()) {
			if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
				if (ignorespaces)
					ignorespaces = false;
				else
					ignorespaces = true;
			} else if (c == ' ' && !ignorespaces && (i == 0 || argarray[i - 1] != '\\')) {
				args3.add(last);
				last = "";
			} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
					&& (argarray[i + 1] != ' ' || ignorespaces))) {
				last += c;
			}

			i++;
		}

		if (last == "" == false)
			args3.add(last);

		return args3;
	}

	// Command handler
	private boolean handleCommand(String cmd, ChatClient client) {
		// Check permissions
		if (client.getPlayer().getPlayerInventory().containsItem("permissions")) {
			String permLevel = client.getPlayer().getPlayerInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
			if (permLevel.equals("moderator") || permLevel.equals("admin") || permLevel.equals("developer")) {
				// Parse command
				ArrayList<String> args = parseCommand(cmd);
				String cmdId = "";
				if (args.size() > 0) {
					cmdId = args.remove(0);
					cmd = cmdId;

					// Run command
					switch (cmdId) {
					case "mute": {
						// Mute
						if (args.size() < 1) {
							systemMessage("Missing argument: player", cmd, client);
							return true;
						} else if (args.size() < 2) {
							systemMessage("Missing argument: minutes", cmd, client);
							return true;
						}

						int minutes;
						try {
							minutes = Integer.valueOf(args.get(1));
						} catch (Exception e) {
							systemMessage("Invalid value for argument: minutes", cmd, client);
							return true;
						}
						int hours = 0;
						try {
							if (args.size() >= 3)
								hours = Integer.valueOf(args.get(2));
						} catch (Exception e) {
							systemMessage("Invalid value for argument: hours", cmd, client);
							return true;
						}
						int days = 0;
						try {
							if (args.size() >= 4)
								hours = Integer.valueOf(args.get(3));
						} catch (Exception e) {
							systemMessage("Invalid value for argument: days", cmd, client);
							return true;
						}

						// Find player
						String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Mute
						if (acc.getPlayerInventory().containsItem("penalty") && acc.getPlayerInventory()
								.getItem("penalty").getAsJsonObject().get("type").getAsString().equals("ban")) {
							// Check ban
							systemMessage("Specified account is banned.", cmd, client);
							return true;
						}

						JsonObject banInfo = new JsonObject();
						banInfo.addProperty("type", "mute");
						banInfo.addProperty("unmuteTimestamp", System.currentTimeMillis() + (minutes * 60 * 1000)
								+ (hours * 60 * 60 * 1000) + (days * 24 * 60 * 60 * 1000));
						acc.getPlayerInventory().setItem("penalty", banInfo);

						systemMessage("Muted " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "tempban": {
						// Temporary ban
						if (args.size() < 1) {
							systemMessage("Missing argument: player", cmd, client);
							return true;
						} else if (args.size() < 2) {
							systemMessage("Missing argument: days", cmd, client);
							return true;
						}
						int days;
						try {
							days = Integer.valueOf(args.get(1));
						} catch (Exception e) {
							systemMessage("Invalid value for argument: days", cmd, client);
							return true;
						}

						// Find player
						String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Ban temporarily
						JsonObject banInfo = new JsonObject();
						banInfo.addProperty("type", "ban");
						banInfo.addProperty("unbanTimestamp",
								System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000));
						acc.getPlayerInventory().setItem("penalty", banInfo);

						// Find online player
						for (Player plr : EmuFeral.gameServer.getPlayers()) {
							if (plr.account.getDisplayName().equals(args.get(0))) {
								// Kick the player
								plr.client.sendPacket("%xt%ua%-1%3561%"); // TODO: quit instead of ok
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
								}
								plr.client.disconnect();
								break;
							}
						}

						systemMessage("Temporarily banned " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "permban": {
						// Permanent ban
						if (args.size() < 1) {
							systemMessage("Missing argument: player", cmd, client);
							return true;
						}

						// Find player
						String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Ban permanently
						JsonObject banInfo = new JsonObject();
						banInfo.addProperty("type", "ban");
						banInfo.addProperty("unbanTimestamp", -1);
						acc.getPlayerInventory().setItem("penalty", banInfo);

						// Find online player
						for (Player plr : EmuFeral.gameServer.getPlayers()) {
							if (plr.account.getDisplayName().equals(args.get(0))) {
								// Kick the player
								plr.client.sendPacket("%xt%ua%-1%3561%"); // TODO: quit instead of ok
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
								}
								plr.client.disconnect();
								break;
							}
						}

						systemMessage("Banned " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "pardon": {
						// Remove all penalties
						if (args.size() < 1) {
							systemMessage("Missing argument: player", cmd, client);
							return true;
						}

						// Find player
						String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Remove penalties
						if (acc.getPlayerInventory().containsItem("penalty"))
							acc.getPlayerInventory().deleteItem("penalty");

						systemMessage("Penalties removed from " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "kick": {
						// Kick command
						if (args.size() < 1) {
							systemMessage("Missing argument: player", cmd, client);
							return true;
						}

						// Find player
						for (Player plr : EmuFeral.gameServer.getPlayers()) {
							if (plr.account.getDisplayName().equals(args.get(0))) {
								// Kick the player
								systemMessage("Kicked " + plr.account.getDisplayName() + ".", cmd, client);
								plr.client.sendPacket("%xt%ua%-1%4086%"); // TODO: quit instead of ok
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
								}
								plr.client.disconnect();
								return true;
							}
						}

						// Player not found
						systemMessage("Player is not online.", cmd, client);
						return true;
					}
					case "removeperms": {
						// Check perms
						if (permLevel.equals("admin") || permLevel.equals("developer")) {
							// Permanent ban
							if (args.size() < 1) {
								systemMessage("Missing argument: player", cmd, client);
								return true;
							}

							// Find player
							String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
							if (uuid == null) {
								// Player not found
								systemMessage("Specified account could not be located.", cmd, client);
								return true;
							}
							EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);

							// Take permissions away
							if (acc.getPlayerInventory().containsItem("permissions"))
								acc.getPlayerInventory().deleteItem("permissions");

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getPlayerInventory().deleteItem("permissions");
									break;
								}
							}

							// Completed
							systemMessage("Removed all permissions from " + acc.getDisplayName() + ".", cmd, client);
							return true;
						}
					}
					case "makeadmin": {
						// Check perms
						if (permLevel.equals("admin") || permLevel.equals("developer")) {
							// Permanent ban
							if (args.size() < 1) {
								systemMessage("Missing argument: player", cmd, client);
								return true;
							}

							// Find player
							String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
							if (uuid == null) {
								// Player not found
								systemMessage("Specified account could not be located.", cmd, client);
								return true;
							}
							EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);

							// Make admin
							if (!acc.getPlayerInventory().containsItem("permissions"))
								acc.getPlayerInventory().setItem("permissions", new JsonObject());
							if (!acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
									.has("permissionLevel"))
								acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
										.remove("permissionLevel");
							acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
									.addProperty("permissionLevel", "admin");
							acc.getPlayerInventory().setItem("permissions",
									acc.getPlayerInventory().getItem("permissions"));

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getPlayerInventory().setItem("permissions",
											acc.getPlayerInventory().getItem("permissions"));
									break;
								}
							}

							// Completed
							systemMessage("Made " + acc.getDisplayName() + " administrator.", cmd, client);
							return true;
						}
					}
					case "makemoderator": {
						// Check perms
						if (permLevel.equals("admin") || permLevel.equals("developer")) {
							// Permanent ban
							if (args.size() < 1) {
								systemMessage("Missing argument: player", cmd, client);
								return true;
							}

							// Find player
							String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
							if (uuid == null) {
								// Player not found
								systemMessage("Specified account could not be located.", cmd, client);
								return true;
							}
							EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);

							// Make moderator
							if (!acc.getPlayerInventory().containsItem("permissions"))
								acc.getPlayerInventory().setItem("permissions", new JsonObject());
							if (!acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
									.has("permissionLevel"))
								acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
										.remove("permissionLevel");
							acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
									.addProperty("permissionLevel", "moderator");
							acc.getPlayerInventory().setItem("permissions",
									acc.getPlayerInventory().getItem("permissions"));

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getPlayerInventory().setItem("permissions",
											acc.getPlayerInventory().getItem("permissions"));
									break;
								}
							}

							// Completed
							systemMessage("Made " + acc.getDisplayName() + " moderator.", cmd, client);
							return true;
						}
					}
					case "updatewarning": {
						// Check perms
						if (permLevel.equals("admin") || permLevel.equals("developer")) {
							if (args.size() < 1) {
								systemMessage("Missing argument: minutes-remaining", cmd, client);
								return true;
							}

							// Parse arguments
							int mins = 0;
							try {
								mins = Integer.valueOf(args.get(0));
							} catch (Exception e) {
								systemMessage("Invalid value for argument: minutes-remaining", cmd, client);
								return true;
							}

							// Warn everyone
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								if (mins == 1)
									plr.client.sendPacket("%xt%ua%-1%7390|1%");
								else
									plr.client.sendPacket("%xt%ua%-1%7391|" + mins + "%");
							}

							return true;
						}
					}
					case "updateshutdown": {
						// Check perms
						if (permLevel.equals("admin") || permLevel.equals("developer")) {
							// Disconnect everyone
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								plr.client.sendPacket("%xt%ua%-1%__FORCE_RELOGIN__%");
							}

							// Inform the game server to disconnect with maintenance
							EmuFeral.gameServer.maintenance = true;

							// Wait a bit
							int i = 0;
							while (EmuFeral.gameServer.getPlayers().length != 0) {
								i++;
								if (i == 30)
									break;
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
								}
							}
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								plr.client.disconnect();
							}

							// Wait for logoff and exit
							int l = 0;
							while (EmuFeral.gameServer.getPlayers().length != 0) {
								l++;
								if (l == 60) {
									break;
								}
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
								}
							}

							// Exit
							System.exit(0);
							return true;
						}
					}
					//Developer commands below..
					case "tpm": {
						//Teleports a player to a map.
						if (permLevel.equals("developer")) {
							if (args.size() < 1) {
								systemMessage("Missing argument: teleport UUID", cmd, client);
								return true;
							}
							
							WorldReadyPacket wrp = new WorldReadyPacket();
							wrp.teleportUUID = args.get(0);
							
							try {
								wrp.handle(((Player)client.container).client);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								systemMessage("Error: " + e.getMessage(), cmd, client);
							}
						
							return true;
						}
						return true;
					}		
					
					case "help": {
						// Help command
						String message = "List of commands:\n";
						message += " - kick \"<member>\"\n";
						message += " - permbam \"<name>\"\n";
						message += " - tempban \"<name>\" <days>\"\n";
						message += " - mute \"<name>\" <minutes> [hours] [days]\n";
						message += " - pardon \"<name>\"\n";
						if (permLevel.equals("admin") || permLevel.equals("developer")) {
							message += " - makeadmin \"<name>\"\n";
							message += " - makemoderator \"<name>\"\n";
							message += " - removeperms \"<name>\"\n";
							message += " - updatewarning <minutes-remaining>\n";
							message += " - updateshutdown\n";
						}
						message += " - help";
						systemMessage(message, cmdId, client);
						return true;
					}
					}
				}

				// Command not found
				systemMessage("Command not recognized, use help for a list of commands", cmd, client);
				return true;
			}
		}
		return false;
	}

	private void systemMessage(String message, String cmd, ChatClient client) {
		// Send response
		JsonObject res = new JsonObject();
		res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
		res.addProperty("conversationId", room);
		res.addProperty("message", "Issued chat command: " + cmd + ":\n[system] " + message);
		res.addProperty("source", client.getPlayer().getAccountID());
		res.addProperty("sentAt", LocalDateTime.now().toString());
		res.addProperty("eventId", "chat.postMessage");
		res.addProperty("success", true);
		client.sendPacket(res);
	}

}
