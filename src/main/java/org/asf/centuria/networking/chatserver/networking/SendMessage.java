package org.asf.centuria.networking.chatserver.networking;

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
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.dms.PrivateChatMessage;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.interactions.modules.QuestManager;
import org.asf.centuria.ipbans.IpBanManager;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.chatcommands.ChatCommandEvent;
import org.asf.centuria.modules.events.chatcommands.ModuleCommandSyntaxListEvent;
import org.asf.centuria.modules.events.maintenance.MaintenanceEndEvent;
import org.asf.centuria.modules.events.maintenance.MaintenanceStartEvent;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.packets.xt.gameserver.room.RoomJoinPacket;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SendMessage extends AbstractChatPacket {

	private static String NIL_UUID = new UUID(0, 0).toString();
	private static ArrayList<String> muteWords = new ArrayList<String>();
	private static ArrayList<String> filterWords = new ArrayList<String>();
	private static ArrayList<String> alwaysfilterWords = new ArrayList<String>();

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

				for (String word : data.split(";"))
					filterWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Load ban words
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/instamute.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(";"))
					muteWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Load always filtered words
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/alwaysfilter.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(";"))
					alwaysfilterWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		DMManager manager = DMManager.getInstance();

		// Ignore 'limbo' players
		Player gameClient = client.getPlayer().getOnlinePlayerInstance();
		if (gameClient == null) {
			// Ok, even worse, hacker, disconnect them after banning
			client.getPlayer().ban("No gameserver connection while chatting");
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

		// Log
		Centuria.logger.info("Chat: " + client.getPlayer().getDisplayName() + ": " + message);

		// Increase ban counter
		client.banCounter++;

		// Check it
		if (client.banCounter >= 7) {
			// Ban the hacker
			client.getPlayer().ban("Spam hack");
			return true;
		}

		// Check mute
		CenturiaAccount acc = client.getPlayer();
		if (!client.isRoomPrivate(room) && acc.getPlayerInventory().containsItem("penalty") && acc.getPlayerInventory()
				.getItem("penalty").getAsJsonObject().get("type").getAsString().equals("mute")) {
			JsonObject banInfo = acc.getPlayerInventory().getItem("penalty").getAsJsonObject();
			if (banInfo.get("unmuteTimestamp").getAsLong() == -1
					|| banInfo.get("unmuteTimestamp").getAsLong() > System.currentTimeMillis()) {
				// Time format
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

				// System message
				JsonObject res = new JsonObject();
				res.addProperty("conversationType", "room");
				res.addProperty("conversationId", room);
				res.addProperty("message", "You are muted and cannot speak in public chats.");
				res.addProperty("source", NIL_UUID);
				res.addProperty("sentAt", fmt.format(new Date()));
				res.addProperty("eventId", "chat.postMessage");
				res.addProperty("success", true);

				// Send message
				client.sendPacket(res);

				return true; // ignore chat
			}
		}

		// Check filter
		String newMessage = "";
		for (String word : message.split(" ")) {
			if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				// Mute
				client.getPlayer().mute(0, 0, 30, "SYSTEM", "Illegal word in chat");

				// Send system message
				if (client.isRoomPrivate(room)) {
					// DM message
					Centuria.systemMessage(gameClient,
							"You have been automatically muted for violating the emulator rules, mute will last 30 minutes.\nReason: illegal word in chat.",
							true);
				} else {
					// Public chat
					Centuria.systemMessage(gameClient,
							"You have been automatically muted for violating the emulator rules, mute will last 30 minutes.\\nReason: illegal word in chat.");
				}

				return true;
			}

			if (!newMessage.isEmpty())
				newMessage += " " + word;
			else
				newMessage = word;
		}
		message = newMessage;

		if (client.isInRoom(room)) {
			// Time format
			SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

			// If it is a DM, save message
			if (client.isRoomPrivate(room) && manager.dmExists(room)) {
				PrivateChatMessage msg = new PrivateChatMessage();
				msg.content = message;
				msg.sentAt = fmt.format(new Date());
				msg.source = client.getPlayer().getAccountID();
				manager.saveDMMessge(room, msg);
			}

			// Send to all in room
			Player cPlayer = gameClient;
			SocialManager socialManager = SocialManager.getInstance();
			for (ChatClient cl : client.getServer().getClients()) {
				if (cl.isInRoom(room)) {
					if (!socialManager.socialListExists(cl.getPlayer().getAccountID()) || !socialManager
							.getPlayerIsBlocked(cl.getPlayer().getAccountID(), client.getPlayer().getAccountID())) {
						// Check limbo player
						gameClient = cl.getPlayer().getOnlinePlayerInstance();
						if (gameClient == null || !gameClient.roomReady || gameClient.room == null)
							continue;

						// Check ghost mode
						if (cPlayer.ghostMode && !gameClient.hasModPerms)
							continue;

						// Filter
						String filteredMessage = "";

						// Load filter settings
						int filterSetting = 0;
						UserVarValue val = cl.getPlayer().getPlayerInventory().getUserVarAccesor()
								.getPlayerVarValue(9362, 0);
						if (val != null)
							filterSetting = val.value;

						// Check filter
						for (String word : message.split(" ")) {
							if (filterSetting != 0) {
								if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
									// Filter it
									for (String filter : filterWords) {
										while (word.toLowerCase().contains(filter.toLowerCase())) {
											String start = word.substring(0,
													word.toLowerCase().indexOf(filter.toLowerCase()));
											String rest = word.substring(
													word.toLowerCase().indexOf(filter.toLowerCase()) + filter.length());
											String tag = "";
											for (int i = 0; i < filter.length(); i++) {
												tag += "#";
											}
											word = start + tag + rest;
										}
									}
								}
							}

							// check always filtered
							if (alwaysfilterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Filter it
								for (String filter : alwaysfilterWords) {
									while (word.toLowerCase().contains(filter.toLowerCase())) {
										String start = word.substring(0,
												word.toLowerCase().indexOf(filter.toLowerCase()));
										String rest = word.substring(
												word.toLowerCase().indexOf(filter.toLowerCase()) + filter.length());
										String tag = "";
										for (int i = 0; i < filter.length(); i++) {
											tag += "#";
										}
										word = start + tag + rest;
									}
								}
							}

							if (!filteredMessage.isEmpty())
								filteredMessage += " " + word;
							else
								filteredMessage = word;
						}

						// Check if the source blocked this player, if so, prevent them form receiving
						if (socialManager.getPlayerIsBlocked(client.getPlayer().getAccountID(),
								cl.getPlayer().getAccountID()))
							continue; // Blocked

						// Send response
						JsonObject res = new JsonObject();
						res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
						res.addProperty("conversationId", room);
						res.addProperty("message", filteredMessage);
						res.addProperty("source", client.getPlayer().getAccountID());
						res.addProperty("sentAt", fmt.format(new Date()));
						res.addProperty("eventId", "chat.postMessage");
						res.addProperty("success", true);

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

		if (Centuria.giveAllResources)
			commandMessages.add("giveBasicMaterials");
		if (Centuria.giveAllCurrency)
			commandMessages.add("giveBasicCurrency");

		if (GameServer.hasPerm(permLevel, "moderator")) {
			commandMessages.add("toggleghostmode");
			commandMessages.add("toggletpoverride");
			commandMessages.add("kick \"<player>\" [\"<reason>\"]");
			commandMessages.add("ipban \"<player/address>\" [\"<reason>\"]");
			commandMessages.add("pardonip \"<ip>\" [\"<reason>\"]");
			commandMessages.add("permban \"<player>\" [\"<reason>\"]");
			commandMessages.add("tempban \"<player>\" <days>\" [\"<reason>\"]");
			commandMessages.add("forcenamechange \"<player>\"");
			commandMessages.add("changeothername \"<player>\" \"<new-name>\"");
			commandMessages.add("mute \"<player>\" <minutes> [hours] [days] [\"<reason>\"]");
			commandMessages.add("pardon \"<player>\" [\"<reason>\"]");
			if (GameServer.hasPerm(permLevel, "developer")) {
				commandMessages.add("makedeveloper \"<name>\"");
				commandMessages.add("srp \"<raw-packet>\" [<player>]");
			}
			if (GameServer.hasPerm(permLevel, "admin")) {
				commandMessages.add("tpm <levelDefID> [<levelType>]");
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
			commandMessages.add("giveitem <itemDefId> [<quantity>] [<player>]");
		}
		commandMessages.add("questrewind <amount-of-quests-to-rewind>");

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

				if (cmdId.equals("givebasicmaterials")) {
					if (Centuria.giveAllResources) {
						var onlinePlayer = client.getPlayer().getOnlinePlayerInstance();

						if (onlinePlayer != null) {
							var accessor = client.getPlayer().getPlayerInventory().getItemAccessor(onlinePlayer);

							accessor.add(6691, 1000);
							accessor.add(6692, 1000);
							accessor.add(6693, 1000);
							accessor.add(6694, 1000);
							accessor.add(6695, 1000);
							accessor.add(6696, 1000);
							accessor.add(6697, 1000);
							accessor.add(6698, 1000);
							accessor.add(6699, 1000);
							accessor.add(6700, 1000);
							accessor.add(6701, 1000);
							accessor.add(6702, 1000);
							accessor.add(6703, 1000);
							accessor.add(6704, 1000);
							accessor.add(6705, 1000);

							// TODO: Check result
							systemMessage("You have been given 1000 of every basic material. Have fun!", cmd, client);
						}
						return true;
					}
				} else if (cmdId.equals("givebasiccurrency")) {
					if (Centuria.giveAllCurrency) {
						var onlinePlayer = client.getPlayer().getOnlinePlayerInstance();

						if (onlinePlayer != null) {
							var accessor = client.getPlayer().getPlayerInventory().getCurrencyAccessor();

							accessor.addLikes(onlinePlayer.client, 1000);
							accessor.addStarFragments(onlinePlayer.client, 1000);

							// TODO: Check result
							systemMessage("You have been given 1000 star fragments and likes. Have fun!", cmd, client);
						}
						return true;
					}
				} else if (cmdId.equals("questrewind")) {
					if (args.size() < 1) {
						// Missing argument
						systemMessage("Missing argument: amount of quests to rewind", cmd, client);
						return true;
					}
					int questsToRewind = 0;
					try {
						questsToRewind = Integer.parseInt(args.get(0));
					} catch (NumberFormatException e) {
						// Missing argument
						systemMessage("Invalid argument: amount of quests to rewind: expected number", cmd, client);
						return true;
					}
					if (questsToRewind < 1) {
						// Missing argument
						systemMessage(
								"Invalid argument: amount of quests to rewind: expected a number greater or equal to one.\n\nTo restart the quest specify 1, 2 restarts the quest before the current one.",
								cmd, client);
						return true;
					}

					// Get current quest position
					String quest = QuestManager.getActiveQuest(client.getPlayer());
					int pos = QuestManager.getQuestPosition(quest);
					if (pos < questsToRewind) {
						// Missing argument
						systemMessage(
								"Invalid argument: amount of quests to rewind: number exceeds the amount of your completed quests.",
								cmd, client);
						return true;
					}

					// Rewind quests
					JsonObject obj = client.getPlayer().getPlayerInventory().getAccessor().findInventoryObject("311",
							22781);
					JsonObject progressionMap = obj.get("components").getAsJsonObject()
							.get("SocialExpanseLinearGenericQuestsCompletion").getAsJsonObject();
					JsonArray arr = progressionMap.get("completedQuests").getAsJsonArray();
					for (int i = 0; i < questsToRewind; i++) {
						arr.remove(arr.get(arr.size() - 1));
					}

					// Save
					client.getPlayer().getPlayerInventory().setItem("311",
							client.getPlayer().getPlayerInventory().getItem("311"));
					systemMessage("Success! Rewinded your quest log, '"
							+ QuestManager.getQuest(QuestManager.getActiveQuest(client.getPlayer())).name
							+ "' is now your active quest! Please log out and log back in to complete the process.",
							cmd, client);

					return true;
				}

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
								suspiciousClients.put(cl, "no gameserver connection");
							} else if ((!plr.roomReady || plr.room == null) && plr.levelID != 25280) {
								suspiciousClients.put(cl, "limbo");
							}
						}
						// Build message
						String response = Centuria.gameServer.getPlayers().length + " player(s) online:";
						for (ChatClient cl : client.getServer().getClients()) {
							Player plr = cl.getPlayer().getOnlinePlayerInstance();
							if (plr != null && !suspiciousClients.containsKey(cl)) {
								String map = "UNKOWN: " + plr.levelID;
								if (plr.levelID == 25280)
									map = "Tutorial";
								else if (helper.has(Integer.toString(plr.levelID)))
									map = helper.get(Integer.toString(plr.levelID)).getAsString();
								response += "\n - " + plr.account.getDisplayName() + " (" + map + ")"
										+ (plr.ghostMode ? " [GHOSTING]" : "");
							} else if (!suspiciousClients.containsKey(cl)) {
								suspiciousClients.put(cl, "no gameserver connection");
							}
						}
						// Add suspicious clients
						if (suspiciousClients.size() != 0) {
							response += "\n";
							response += "\nSuspicious clients:";
							for (ChatClient cl : suspiciousClients.keySet())
								response += "\n - " + cl.getPlayer().getDisplayName() + " [" + suspiciousClients.get(cl)
										+ "]";
						}
						// Send response
						systemMessage(response, cmd, client);
						return true;
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

						String reason = null;
						if (args.size() >= 5)
							reason = args.get(4);

						// Find player
						String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

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
						acc.mute(days, hours, minutes, client.getPlayer().getAccountID(), reason);
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

						String reason = null;
						if (args.size() >= 3)
							reason = args.get(2);

						// Find player
						String uuid = AccountManager.getInstance().getUserByDisplayName(args.get(0));
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

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
						acc.tempban(days, client.getPlayer().getAccountID(), reason);
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
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						String reason = null;
						if (args.size() >= 2)
							reason = args.get(1);

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
						acc.ban(client.getPlayer().getAccountID(), reason);
						systemMessage("Permanently banned " + acc.getDisplayName() + ".", cmd, client);
						return true;
					}
					case "ipban": {
						// IP-ban command
						if (args.size() < 1) {
							systemMessage("Missing argument: player or address", cmd, client);
							return true;
						}

						String reason = null;
						if (args.size() >= 2)
							reason = args.get(1);

						// Find player
						for (Player plr : Centuria.gameServer.getPlayers()) {
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
								plr.account.ipban(client.getPlayer().getAccountID(), reason);
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
							for (Player plr : Centuria.gameServer.getPlayers()) {
								// Get IP of player
								try {
									InetSocketAddress ip = (InetSocketAddress) plr.client.getSocket()
											.getRemoteSocketAddress();
									InetAddress addr = ip.getAddress();
									String ipaddr = addr.getHostAddress();
									if (ipaddr.equals(args.get(0))) {
										// Ban player
										plr.account.ban(client.getPlayer().getAccountID(), reason);
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
						for (Player plr : Centuria.gameServer.getPlayers()) {
							if (plr.account.getAccountID().equals(client.getPlayer().getAccountID())) {
								// Load the requested room
								RoomJoinPacket join = new RoomJoinPacket();
								join.levelType = 0; // World
								join.levelID = 1718;

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
								plr.pendingLevelID = 1718;
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
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Reason
						String reason = null;
						if (args.size() >= 2) {
							reason = args.get(1);
						}

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
						acc.pardon(client.getPlayer().getAccountID(), reason);
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
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
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
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
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
						acc.kickDirect("SYSTEM", "Display name changed");
						systemMessage("Renamed " + oldName + " " + args.get(1) + ".", cmd, client);
						return true;
					}
					case "kick": {
						// Kick command
						if (args.size() < 1) {
							systemMessage("Missing argument: player", cmd, client);
							return true;
						}

						String reason = null;
						if (args.size() >= 2)
							reason = args.get(1);

						// Find player
						for (Player plr : Centuria.gameServer.getPlayers()) {
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
								plr.account.kick(client.getPlayer().getAccountID(), reason);
								return true;
							}
						}

						// Find chat-only connection
						for (ChatClient cl : client.getServer().getClients())
							if (cl.getPlayer().getDisplayName().equals(args.get(0))) {
								// Check rank
								if (cl.getPlayer().getPlayerInventory().containsItem("permissions")) {
									if ((GameServer.hasPerm(cl.getPlayer().getPlayerInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "developer")
											&& !GameServer.hasPerm(permLevel, "developer"))
											|| GameServer.hasPerm(
													cl.getPlayer().getPlayerInventory().getItem("permissions")
															.getAsJsonObject().get("permissionLevel").getAsString(),
													"admin") && !GameServer.hasPerm(permLevel, "admin")) {
										systemMessage("Unable to kick higher-ranking users.", cmd, client);
										return true;
									}
								}

								// Disconnect
								cl.disconnect();
								systemMessage("Kicked " + cl.getPlayer().getDisplayName() + " from the chat server.",
										cmd, client);
								return true;
							}

						// Player not found
						systemMessage("Player is not online.", cmd, client);
						return true;
					}
					case "toggletpoverride": {
						// Override tp locks
						Player plr = client.getPlayer().getOnlinePlayerInstance();
						if (plr.overrideTpLocks) {
							plr.overrideTpLocks = false;
							systemMessage(
									"Teleport override disabled. The system will no longer ignore follower settings.",
									cmd, client);
						} else {
							plr.overrideTpLocks = true;
							systemMessage("Teleport override enabled. The system will ignore follower settings.", cmd,
									client);
						}
						return true;
					}
					case "toggleghostmode": {
						// Ghost mode
						Player plr = client.getPlayer().getOnlinePlayerInstance();
						if (plr.ghostMode) {
							plr.ghostMode = false;

							// Spawn for everyone in room
							GameServer server = (GameServer) plr.client.getServer();
							for (Player player : server.getPlayers()) {
								if (plr.room != null && player.room != null && player.room.equals(plr.room)
										&& player != plr) {
									player.syncTo(plr);
									Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"), "Syncing player "
											+ player.account.getDisplayName() + " to " + plr.account.getDisplayName());
								}
							}

							systemMessage("Ghost mode disabled. You are visible to everyone.", cmd, client);
						} else {
							plr.ghostMode = true;

							// Spawn for everyone in room
							GameServer server = (GameServer) plr.client.getServer();
							for (Player player : server.getPlayers()) {
								if (plr.room != null && player.room != null && player.room.equals(plr.room)
										&& player != plr && !player.hasModPerms) {
									player.destroyAt(player);
									Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"),
											"Removing player " + player.account.getDisplayName() + " from "
													+ plr.account.getDisplayName());
								}
							}

							systemMessage("Ghost mode enabled. You are now invisible to non-moderators.", cmd, client);
						}

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
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

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
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

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
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

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
							for (Player plr : Centuria.gameServer.getPlayers()) {
								if (plr.account.getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.account.getPlayerInventory().deleteItem("permissions");
									plr.hasModPerms = false;
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
							for (Player plr : Centuria.gameServer.getPlayers()) {
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
							if (Centuria.runUpdater(mins)) {
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
							if (Centuria.cancelUpdate())
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
							Centuria.updateShutdown();
							return true;
						} else {
							break;
						}
					}
					case "startmaintenance": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							// Enable maintenance mode
							Centuria.gameServer.maintenance = true;

							// Dispatch maintenance event
							EventBus.getInstance().dispatchEvent(new MaintenanceStartEvent());
							// Cancel if maintenance is disabled
							if (!Centuria.gameServer.maintenance)
								return true;

							// Disconnect everyone but the staff
							for (Player plr : Centuria.gameServer.getPlayers()) {
								if (!plr.account.getPlayerInventory().containsItem("permissions")
										|| !GameServer.hasPerm(
												plr.account.getPlayerInventory().getItem("permissions")
														.getAsJsonObject().get("permissionLevel").getAsString(),
												"moderator"))
									plr.client.sendPacket("%xt%ua%-1%__FORCE_RELOGIN__%");
							}

							// Wait a bit
							int i = 0;
							while (Stream.of(Centuria.gameServer.getPlayers())
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
							for (Player plr : Centuria.gameServer.getPlayers()) {
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
							Centuria.gameServer.maintenance = false;

							// Dispatch maintenance end event
							EventBus.getInstance().dispatchEvent(new MaintenanceEndEvent());

							// Cancel if maintenance is enabled
							if (Centuria.gameServer.maintenance)
								return true;

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
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

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
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							try {
								// Teleports a player to a map.
								String defID = "";
								if (args.size() < 1) {
									systemMessage("Missing argument: teleport defID", cmd, client);
									return true;
								}

								// Parse arguments
								defID = args.get(0);
								String type = "0";
								if (args.size() > 1) {
									type = args.get(1);
								}

								// Teleport
								client.getPlayer().getOnlinePlayerInstance().teleportToRoom(Integer.valueOf(defID),
										Integer.valueOf(type), -1, "room_" + defID, "");
							} catch (Exception e) {
								e.printStackTrace();
								systemMessage("Error: " + e, cmd, client);
							}
						}

						return true;
					}
					case "srp": {
						// Sends a raw packet
						if (GameServer.hasPerm(permLevel, "developer")) {
							String packet = "";
							if (args.size() < 1) {
								systemMessage("Missing argument: raw-packet", cmd, client);
								return true;
							}

							// Parse arguments
							packet = args.get(0);
							String player = client.getPlayer().getDisplayName();
							if (args.size() > 1) {
								player = args.get(1);
							}
							String uuid = AccountManager.getInstance().getUserByDisplayName(player);
							if (uuid == null) {
								// Player not found
								systemMessage("Specified account could not be located.", cmd, client);
								return true;
							}
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

							// Send packet
							try {
								if (acc.getOnlinePlayerInstance() == null)
									systemMessage("Error: player not online", cmd, client);
								acc.getOnlinePlayerInstance().client.sendPacket(packet);
								systemMessage("Packet has been sent.", cmd, client);
							} catch (Exception e) {
								systemMessage("Error: " + e, cmd, client);
							}

							return true;
						}
					}
					case "giveitem":
						if (GameServer.hasPerm(permLevel, "admin")) {
							try {
								int defID = 0;
								int quantity = 1;
								String player = "";
								String uuid = "";

								if (args.size() < 1) {
									systemMessage("Missing argument: itemDefId", cmd, client);
									return true;
								}

								defID = Integer.valueOf(args.get(0));

								if (args.size() == 2) {
									quantity = Integer.valueOf(args.get(1));
								}

								if (args.size() >= 3) {
									player = args.get(2);

									// check existence of player

									uuid = AccountManager.getInstance().getUserByDisplayName(player);
									if (uuid == null) {
										// Player not found
										systemMessage("Specified account could not be located.", cmd, client);
										return true;
									}
								}

								// funny stuff check
								if (quantity <= 0 || defID <= 0) {
									systemMessage("You cannot give 0 or less quantity of/or an item ID of 0 or below.",
											cmd, client);
									return true;
								}

								// player case..

								if (uuid.equals("")) {
									// give item to the command sender..

									var onlinePlayer = client.getPlayer().getOnlinePlayerInstance();

									if (onlinePlayer != null) {
										var result = client.getPlayer().getPlayerInventory()
												.getItemAccessor(client.getPlayer().getOnlinePlayerInstance())
												.add(defID, quantity);

										// TODO: Check result
										systemMessage("Gave " + client.getPlayer().getDisplayName() + " " + quantity
												+ " of item " + defID + ".", cmd, client);
										return true;
									} else {
										// TODO: support for giving offline players items.. somehow
										systemMessage("Specified account does not appear to be online.", cmd, client);
									}

								} else {
									var onlinePlayer = AccountManager.getInstance().getAccount(uuid)
											.getOnlinePlayerInstance();

									if (onlinePlayer != null) {
										var result = onlinePlayer.account.getPlayerInventory()
												.getItemAccessor(onlinePlayer).add(defID, quantity);

										// TODO: Check result
										systemMessage("Gave " + onlinePlayer.account.getDisplayName() + " " + quantity
												+ " of item " + uuid + ".", cmd, client);
										return true;
									} else {
										// TODO: support for giving offline players items.. somehow
										systemMessage("Specified account does not appear to be online.", cmd, client);
									}

								}

								return true;
							} catch (Exception e) {
								systemMessage("Error: " + e, cmd, client);
								return true;
							}
						} else {
							break;
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
					message += "\n\nSymbol guide:";
					message += "\n[] = optional arguement";
					message += "\n<> = replace with arguement";
					message += "\nYou do not need the symbols on the command itself, its only for informational purposes.";
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
		Centuria.logger.info(client.getPlayer().getDisplayName() + " executed chat command: " + cmd + ": " + message);
	}

}
