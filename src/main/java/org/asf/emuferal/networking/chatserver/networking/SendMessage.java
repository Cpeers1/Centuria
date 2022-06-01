package org.asf.emuferal.networking.chatserver.networking;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.dms.DMManager;
import org.asf.emuferal.dms.PrivateChatMessage;
import org.asf.emuferal.ipbans.IpBanManager;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.chatcommands.ChatCommandEvent;
import org.asf.emuferal.modules.events.chatcommands.ModuleCommandSyntaxListEvent;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.packets.xt.gameserver.world.WorldReadyPacket;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.social.SocialManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SendMessage extends AbstractChatPacket {

	private static ArrayList<String> banWords = new ArrayList<String>();
	private static ArrayList<String> filterWords = new ArrayList<String>();

	static {
		// Load filter
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/filter.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(" "))
					filterWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
		}

		// Load ban words
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/instaban.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(" "))
					banWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
		}
	}

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
		// Ignore 'limbo' players
		Player gameClient = client.getPlayer().getOnlinePlayerInstance();
		if (gameClient == null) {
			// Ok, even worse, hacker, disconnect them after banning
			client.getPlayer().ban();
			client.disconnect();
			return true;
		} else if (!gameClient.roomReady || gameClient.room == null) {
			// Limbo player
			return true;
		}

		// Clean message
		message = message.trim();

		// Chat commands
		if (message.startsWith(">")) {
			String cmd = message.substring(1).trim();
			if (handleCommand(cmd, client))
				return true;
		}

		// Check content
		if (message.isBlank()) {
			return true; // ignore chat
		}

		// Check filter
		String newMessage = "";
		for (String word : message.split(" ")) {
			if (banWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				// Ban
				client.getPlayer().ban();
				return true;
			}

			if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				// TODO
				if (true)
					continue;

				// Filter it
				for (String filter : filterWords) {
					while (word.toLowerCase().contains(filter.toLowerCase())) {
						String start = word.substring(0, word.toLowerCase().indexOf(filter.toLowerCase()));
						String rest = word.substring(word.toLowerCase().indexOf(filter.toLowerCase()) + 1);
						String tag = "";
						for (int i = 0; i < filter.length(); i++) {
							tag += "#";
						}
						word = start + tag + rest;
					}
				}
			}

			if (!newMessage.isEmpty())
				newMessage += " " + word;
			else
				newMessage = word;
		}
		message = newMessage;

		// Increase ban counter
		client.banCounter++;

		// Check it
		if (client.banCounter >= 7) {
			// Ban the hacker
			client.getPlayer().ban();
			return true;
		}

		// Check mute
		EmuFeralAccount acc = client.getPlayer();
		if (!client.isRoomPrivate(room) && acc.getPlayerInventory().containsItem("penalty") && acc.getPlayerInventory()
				.getItem("penalty").getAsJsonObject().get("type").getAsString().equals("mute")) {
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
			SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			res.addProperty("sentAt", fmt.format(new Date()));
			res.addProperty("eventId", "chat.postMessage");
			res.addProperty("success", true);

			// If it is a DM, save message
			DMManager manager = DMManager.getInstance();
			if (client.isRoomPrivate(room) && manager.dmExists(room)) {
				PrivateChatMessage msg = new PrivateChatMessage();
				msg.content = message;
				msg.sentAt = fmt.format(new Date());
				msg.source = client.getPlayer().getAccountID();
				manager.saveDMMessge(room, msg);
			}

			// Send to all in room
			SocialManager socialManager = SocialManager.getInstance();
			for (ChatClient cl : client.getServer().getClients()) {
				if (cl.isInRoom(room)) {
					if (!socialManager.socialListExists(cl.getPlayer().getAccountID()) || !socialManager
							.getPlayerIsBlocked(cl.getPlayer().getAccountID(), client.getPlayer().getAccountID())) {
						// Check limbo player
						gameClient = cl.getPlayer().getOnlinePlayerInstance();
						if (gameClient != null && !gameClient.roomReady || gameClient.room == null)
							continue;

						// Send message
						cl.sendPacket(res);
					}
				}
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
		// Load permission level
		String permLevel = "member";
		if (client.getPlayer().getPlayerInventory().containsItem("permissions")) {
			permLevel = client.getPlayer().getPlayerInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
		}

		// Generate the command list
		ArrayList<String> commandMessages = new ArrayList<String>();
		if (GameServer.hasPerm(permLevel, "moderator")) {
			commandMessages.add("kick \"<player>\"");
			commandMessages.add("ipban \"<player/address>\"");
			commandMessages.add("pardonip \"<ip>\"");
			commandMessages.add("permban \"<player>\"");
			commandMessages.add("tempban \"<player>\" <days>\"");
			commandMessages.add("forcenamechange \"<player>\"");
			commandMessages.add("changeothername \"<player>\" \"<new-name>\"");
			commandMessages.add("mute \"<player>\" <minutes> [hours] [days]");
			commandMessages.add("pardon \"<player>\"");
			if (GameServer.hasPerm(permLevel, "developer")) {
				commandMessages.add("makedeveloper \"<name>\"");
			}
			if (GameServer.hasPerm(permLevel, "admin")) {
				commandMessages.add("makeadmin \"<player>\"");
				commandMessages.add("makemoderator \"<player>\"");
				commandMessages.add("removeperms \"<player>\"");
				commandMessages.add("startmaintenance");
				commandMessages.add("endmaintenance");
				commandMessages.add("updatewarning <minutes-remaining>");
				commandMessages.add("updateshutdown");
				commandMessages.add("update <60|30|15|10|5|3|1>");
				commandMessages.add("cancelupdate");
			}
			commandMessages.add("staffroom");
			commandMessages.add("listplayers");
		}

		// Add module commands
		ModuleCommandSyntaxListEvent evMCSL = new ModuleCommandSyntaxListEvent(commandMessages, client,
				client.getPlayer(), permLevel);
		EventBus.getInstance().dispatchEvent(evMCSL);

		// Add help if not empty
		if (!commandMessages.isEmpty())
			commandMessages.add("help");

		// Run command
		if (!commandMessages.isEmpty()) {
			// Parse command
			ArrayList<String> args = parseCommand(cmd);
			String cmdId = "";
			if (args.size() > 0) {
				cmdId = args.remove(0).toLowerCase();
				cmd = cmdId;

				// Run module command
				final String cmdIdentifier = cmd;
				ChatCommandEvent ev = new ChatCommandEvent(cmdId, args, client, client.getPlayer(), permLevel, t -> {
					systemMessage(t, cmdIdentifier, client);
				});
				EventBus.getInstance().dispatchEvent(ev);
				if (ev.isHandled())
					return true;

				// Run system command
				if (GameServer.hasPerm(permLevel, "moderator")) {
					switch (cmdId) {

					//
					// Moderator commands below
					case "listplayers": {
						// Load spawn helper
						JsonObject helper = null;
						try {
							// Load helper
							InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
									.getResourceAsStream("spawns.json");
							helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
									.get("Maps").getAsJsonObject();
							strm.close();
						} catch (Exception e) {
						}

						// Locate suspicious clients
						HashMap<ChatClient, String> suspiciousClients = new HashMap<ChatClient, String>();
						for (ChatClient cl : client.getServer().getClients()) {
							Player plr = cl.getPlayer().getOnlinePlayerInstance();
							if (plr == null) {
								suspiciousClients.put(client, "no gameserver connection");
							} else if (!plr.roomReady || plr.room == null) {
								suspiciousClients.put(client, "limbo");
							}
						}
						// Build message
						String response = "List of online players:";
						for (ChatClient cl : client.getServer().getClients()) {
							Player plr = cl.getPlayer().getOnlinePlayerInstance();
							if (plr != null) {
								String map = "UNKOWN: " + plr.roomID;
								if (helper.has(Integer.toString(plr.roomID)))
									map = helper.get(Integer.toString(plr.roomID)).getAsString();
								response += "\n" + plr.account.getDisplayName() + " (" + map + ")";
							} else if (!suspiciousClients.containsKey(cl)) {
								suspiciousClients.put(cl, "no gameserver connection");
							}
						}
						// Add suspicious clients
						if (suspiciousClients.size() != 0) {
							response += "\n";
							response += "\nSuspicious clients:";
							for (ChatClient cl : suspiciousClients.keySet())
								response += "\n" + cl.getPlayer().getDisplayName() + " [" + suspiciousClients.get(cl)
										+ "]";
						}
						// Send response
						systemMessage(response, cmd, client);
						break;
					}
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

						// Check rank
						if (acc.getPlayerInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer
											.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
													.get("permissionLevel").getAsString(), "admin")
											&& !GameServer.hasPerm(permLevel, "admin")) {
								systemMessage("Unable to mute higher-ranking users.", cmd, client);
								return true;
							}
						}

						// Check if banned
						if (acc.getPlayerInventory().containsItem("penalty") && acc.getPlayerInventory()
								.getItem("penalty").getAsJsonObject().get("type").getAsString().equals("ban")) {
							// Check ban
							systemMessage("Specified account is banned.", cmd, client);
							return true;
						}

						// Mute
						acc.mute(days, hours, minutes);
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

						// Check rank
						if (acc.getPlayerInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer
											.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
													.get("permissionLevel").getAsString(), "admin")
											&& !GameServer.hasPerm(permLevel, "admin")) {
								systemMessage("Unable to ban higher-ranking users.", cmd, client);
								return true;
							}
						}

						// Ban temporarily
						acc.tempban(days);
						systemMessage("Temporarily banned " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "permban": {
						// Temporary ban
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

						// Check rank
						if (acc.getPlayerInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer
											.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
													.get("permissionLevel").getAsString(), "admin")
											&& !GameServer.hasPerm(permLevel, "admin")) {
								systemMessage("Unable to ban higher-ranking users.", cmd, client);
								return true;
							}
						}

						// Ban permanently
						acc.ban();
						systemMessage("Permanently banned " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "ipban": {
						// IP-ban command
						if (args.size() < 1) {
							systemMessage("Missing argument: player or address", cmd, client);
							return true;
						}

						// Find player
						for (Player plr : EmuFeral.gameServer.getPlayers()) {
							if (plr.account.getDisplayName().equals(args.get(0))) {
								// Check rank
								if (plr.account.getPlayerInventory().containsItem("permissions")) {
									if ((GameServer.hasPerm(plr.account.getPlayerInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "developer")
											&& !GameServer.hasPerm(permLevel, "developer"))
											|| GameServer.hasPerm(
													plr.account.getPlayerInventory().getItem("permissions")
															.getAsJsonObject().get("permissionLevel").getAsString(),
													"admin") && !GameServer.hasPerm(permLevel, "admin")) {
										systemMessage("Unable to ban higher-ranking users.", cmd, client);
										return true;
									}
								}

								// Ban IP
								plr.account.ipban();
								systemMessage("IP-banned " + plr.account.getDisplayName() + ".", cmd, client);
								return true;
							}
						}

						// Check if the inputted address is a IP addres
						try {
							InetAddress.getByName(args.get(0));

							// Ban the IP
							IpBanManager.getInstance().banIP(args.get(0));

							// Disconnect all with the given IP address (or attempt to)
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								// Get IP of player
								try {
									InetSocketAddress ip = (InetSocketAddress) plr.client.getSocket()
											.getRemoteSocketAddress();
									InetAddress addr = ip.getAddress();
									String ipaddr = addr.getHostAddress();
									if (ipaddr.equals(args.get(0))) {
										// Ban player
										plr.account.ban();
									}

									return true;
								} catch (Exception e) {
								}
							}

							// Log completion
							systemMessage("Banned IP: " + args.get(0), cmd, client);
						} catch (Exception e) {
						}

						// Player not found
						systemMessage("Player is not online.", cmd, client);
						return true;
					}
					case "staffroom": {
						// Teleport to staff room

						// Find online player
						for (Player plr : EmuFeral.gameServer.getPlayers()) {
							if (plr.account.getAccountID().equals(client.getPlayer().getAccountID())) {
								// Load the requested room
								JoinRoom join = new JoinRoom();
								join.roomType = 0; // World
								join.roomID = 1718;

								// Sync
								GameServer srv = (GameServer) plr.client.getServer();
								for (Player player : srv.getPlayers()) {
									if (plr.room != null && player.room != null && player.room.equals(plr.room)
											&& player != plr) {
										plr.destroyAt(player);
									}
								}

								// Assign room
								plr.roomReady = false;
								plr.pendingRoomID = 1718;
								plr.pendingRoom = "room_STAFFROOM";
								join.roomIdentifier = "room_STAFFROOM";

								// Send response
								plr.client.sendPacket(join);

								break;
							}
						}

						return true;
					}
					case "pardonip": {
						// Remove IP ban
						if (args.size() < 1) {
							systemMessage("Missing argument: ip", cmd, client);
							return true;
						}

						// Check ip ban
						IpBanManager manager = IpBanManager.getInstance();
						if (manager.isIPBanned(args.get(0)))
							manager.unbanIP(args.get(0));

						systemMessage("Removed IP ban: " + args.get(0) + ".", cmd, client);
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

						// Check rank
						if (acc.getPlayerInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer
											.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
													.get("permissionLevel").getAsString(), "admin")
											&& !GameServer.hasPerm(permLevel, "admin")) {
								systemMessage("Unable to pardon higher-ranking users.", cmd, client);
								return true;
							}
						}

						// Pardon player
						acc.pardon();
						systemMessage("Penalties removed from " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "forcenamechange": {
						// Force name change command
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
						acc.forceNameChange();

						// Player found
						systemMessage(
								"Applied a name change requirement to the next login of " + acc.getDisplayName() + ".",
								cmd, client);
						return true;
					}
					case "changeothername": {
						// Name change command
						if (args.size() < 1) {
							systemMessage("Missing argument: player", cmd, client);
							return true;
						} else if (args.size() < 1) {
							systemMessage("Missing argument: new-name", cmd, client);
							return true;
						}

						// Find player
						String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Load info
						EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);
						String oldName = acc.getDisplayName();

						// Check rank
						if (acc.getPlayerInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer
											.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
													.get("permissionLevel").getAsString(), "admin")
											&& !GameServer.hasPerm(permLevel, "admin")) {
								systemMessage("Unable to rename higher-ranking users.", cmd, client);
								return true;
							}
						}

						// Check name lock
						if (AccountManager.getInstance().isDisplayNameInUse(args.get(1))) {
							// Failure
							systemMessage("Invalid value for argument: new-name: display name is in use", cmd, client);
							return true;
						}

						// Change name
						if (!acc.updateDisplayName(args.get(1))) {
							// Failure
							systemMessage("Invalid value for argument: new-name: invalid characters", cmd, client);
							return true;
						}

						// Lock new name
						AccountManager.getInstance().lockDisplayName(args.get(1), acc.getAccountID());

						// Kick online player
						acc.kick();
						systemMessage("Renamed " + oldName + " " + args.get(1) + ".", cmd, client);
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
								// Check rank
								if (plr.account.getPlayerInventory().containsItem("permissions")) {
									if ((GameServer.hasPerm(plr.account.getPlayerInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "developer")
											&& !GameServer.hasPerm(permLevel, "developer"))
											|| GameServer.hasPerm(
													plr.account.getPlayerInventory().getItem("permissions")
															.getAsJsonObject().get("permissionLevel").getAsString(),
													"admin") && !GameServer.hasPerm(permLevel, "admin")) {
										systemMessage("Unable to kick higher-ranking users.", cmd, client);
										return true;
									}
								}

								// Kick the player
								systemMessage("Kicked " + plr.account.getDisplayName() + ".", cmd, client);
								plr.account.kick();
								return true;
							}
						}

						// Player not found
						systemMessage("Player is not online.", cmd, client);
						return true;
					}

					//
					// Admin commands below
					case "makeadmin": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
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
						} else {
							break;
						}
					}
					case "makemoderator": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
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
						} else {
							break;
						}
					}
					case "removeperms": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
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
							if (acc.getPlayerInventory().containsItem("permissions")) {
								if (GameServer
										.hasPerm(acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
												.get("permissionLevel").getAsString(), "developer")
										&& !GameServer.hasPerm(permLevel, "developer")) {
									systemMessage("Unable to remove permissions from higher-ranking users.", cmd,
											client);
									return true;
								}
								acc.getPlayerInventory().deleteItem("permissions");
							}

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getPlayerInventory().deleteItem("permissions");
									break;
								}
							}

							// Find online player
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								if (plr.account.getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.account.getPlayerInventory().deleteItem("permissions");
									break;
								}
							}

							// Completed
							systemMessage("Removed all permissions from " + acc.getDisplayName() + ".", cmd, client);
							return true;
						} else {
							break;
						}
					}
					case "updatewarning": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
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
						} else {
							break;
						}
					}
					case "update": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							if (args.size() < 1) {
								systemMessage("Missing argument: minutes", cmd, client);
								return true;
							}

							// Parse arguments
							int mins = 0;
							switch (args.get(0)) {
							case "60":
								mins = 60;
								break;
							case "30":
								mins = 30;
								break;
							case "15":
								mins = 15;
								break;
							case "10":
								mins = 10;
								break;
							case "5":
								mins = 5;
								break;
							case "3":
								mins = 3;
								break;
							case "1":
								mins = 1;
								break;
							default:
								systemMessage("Invalid value for argument: minutes-remaining", cmd, client);
								return true;
							}

							// Run timer
							if (EmuFeral.runUpdater(mins)) {
								systemMessage("Update timer has been started.", cmd, client);
							} else {
								systemMessage("Update timer is already running.", cmd, client);
							}

							return true;
						} else {
							break;
						}
					}
					case "cancelupdate": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							// Cancel update
							if (EmuFeral.cancelUpdate())
								systemMessage("Update restart cancelled.", cmd, client);
							else
								systemMessage("Update timer is not running.", cmd, client);
							return true;
						} else {
							break;
						}
					}
					case "updateshutdown": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							// Shut down the server
							EmuFeral.updateShutdown();
							return true;
						} else {
							break;
						}
					}
					case "startmaintenance": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							// Enable maintenance mode
							EmuFeral.gameServer.maintenance = true;

							// Disconnect everyone but the staff
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								if (!plr.account.getPlayerInventory().containsItem("permissions")
										|| !GameServer.hasPerm(
												plr.account.getPlayerInventory().getItem("permissions")
														.getAsJsonObject().get("permissionLevel").getAsString(),
												"moderator"))
									plr.client.sendPacket("%xt%ua%-1%__FORCE_RELOGIN__%");
							}

							// Wait a bit
							int i = 0;
							while (Stream.of(EmuFeral.gameServer.getPlayers())
									.filter(plr -> !plr.account.getPlayerInventory().containsItem("permissions")
											|| !GameServer.hasPerm(
													plr.account.getPlayerInventory().getItem("permissions")
															.getAsJsonObject().get("permissionLevel").getAsString(),
													"moderator"))
									.findFirst().isPresent()) {
								i++;
								if (i == 30)
									break;

								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
								}
							}
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								if (!plr.account.getPlayerInventory().containsItem("permissions")
										|| !GameServer.hasPerm(
												plr.account.getPlayerInventory().getItem("permissions")
														.getAsJsonObject().get("permissionLevel").getAsString(),
												"moderator")) {
									// Disconnect from the game server
									plr.client.disconnect();

									// Disconnect it from the chat server
									for (ChatClient cl : client.getServer().getClients()) {
										if (cl.getPlayer().getAccountID().equals(plr.account.getAccountID())) {
											cl.disconnect();
										}
									}
								}
							}

							// Send message
							systemMessage("Maintenance mode enabled.", cmd, client);
							return true;
						} else {
							break;
						}
					}
					case "endmaintenance": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							// Disable maintenance mode
							EmuFeral.gameServer.maintenance = false;
							systemMessage("Maintenance mode disabled.", cmd, client);
							return true;
						} else {
							break;
						}
					}

					//
					// Developer commands below..
					case "makedeveloper": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "developer")) {
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

							// Make developer
							if (!acc.getPlayerInventory().containsItem("permissions"))
								acc.getPlayerInventory().setItem("permissions", new JsonObject());
							if (!acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
									.has("permissionLevel"))
								acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
										.remove("permissionLevel");
							acc.getPlayerInventory().getItem("permissions").getAsJsonObject()
									.addProperty("permissionLevel", "developer");
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
							systemMessage("Made " + acc.getDisplayName() + " developer.", cmd, client);
							return true;
						} else {
							break;
						}
					}
					case "tpm": {
						// Teleports a player to a map.
						if (GameServer.hasPerm(permLevel, "developer")) {
							if (args.size() < 1) {
								systemMessage("Missing argument: teleport UUID", cmd, client);
								return true;
							}

							WorldReadyPacket wrp = new WorldReadyPacket();
							wrp.teleportUUID = args.get(0);

							// Find online player
							for (Player plr : EmuFeral.gameServer.getPlayers()) {
								if (plr.account.getAccountID().equals(client.getPlayer().getAccountID())) {
									try {
										wrp.handle(plr.client);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										systemMessage("Error: " + e.getMessage(), cmd, client);
									}
									break;
								}
							}

							return true;
						} else {
							break;
						}
					}
					}
				}

				//
				// Help command
				if (cmd.equals("help")) {
					String message = "List of commands:";
					for (String commandMessage : commandMessages) {
						message += "\n - " + commandMessage;
					}
					systemMessage(message, cmdId, client);
					return true;
				}
			}

			// Command not found
			systemMessage("Command not recognized, use help for a list of commands", cmd, client);
			return true;
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

		// Log
		System.out.println(client.getPlayer().getDisplayName() + " executed chat command: " + cmd + ": " + message);
	}

}
