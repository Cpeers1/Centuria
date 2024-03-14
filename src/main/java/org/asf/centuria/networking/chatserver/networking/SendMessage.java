package org.asf.centuria.networking.chatserver.networking;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.highlevel.ItemAccessor;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.dms.PrivateChatMessage;
import org.asf.centuria.entities.generic.Quaternion;
import org.asf.centuria.entities.generic.Vector3;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.enums.objects.WorldObjectMoverNodeType;
import org.asf.centuria.interactions.modules.QuestManager;
import org.asf.centuria.ipbans.IpBanManager;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.accounts.AccountDisconnectEvent;
import org.asf.centuria.modules.events.accounts.MiscModerationEvent;
import org.asf.centuria.modules.events.accounts.AccountDisconnectEvent.DisconnectType;
import org.asf.centuria.modules.events.chat.ChatMessageBroadcastEvent;
import org.asf.centuria.modules.events.chat.ChatMessageReceivedEvent;
import org.asf.centuria.modules.events.chatcommands.ChatCommandEvent;
import org.asf.centuria.modules.events.chatcommands.ModuleCommandSyntaxListEvent;
import org.asf.centuria.modules.events.maintenance.MaintenanceEndEvent;
import org.asf.centuria.modules.events.maintenance.MaintenanceStartEvent;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.ChatClient.OcProxyMetadata;
import org.asf.centuria.networking.chatserver.networking.moderator.ModeratorClient;
import org.asf.centuria.networking.chatserver.proxies.OcProxyInfo;
import org.asf.centuria.networking.chatserver.proxies.ProxySession;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.voicechatserver.VoiceChatClient;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.rooms.GameRoom;
import org.asf.centuria.rooms.impl.GatheringRoomProvider;
import org.asf.centuria.rooms.privateinstances.PrivateInstance;
import org.asf.centuria.rooms.privateinstances.containervars.PrivateInstanceContainer;
import org.asf.centuria.social.SocialManager;
import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.TextFilterService;
import org.asf.centuria.textfilter.result.FilterResult;
import org.asf.centuria.textfilter.result.WordMatch;
import org.asf.centuria.util.io.DataWriter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SendMessage extends AbstractChatPacket {

	private static String NIL_UUID = new UUID(0, 0).toString();

	public static ArrayList<String> clearanceCodes = new ArrayList<String>();
	private static Random rnd = new Random();

	private String message;
	private String room;

	private static OutputStream chatLogBinary;

	static {
		try {
			// Open chat log binary file
			File chatLogFile = new File("logs/chatlog.bin");
			chatLogFile.getParentFile().mkdirs();
			chatLogBinary = new FileOutputStream(chatLogFile);
		} catch (IOException e) {
			// Log
			Centuria.logger.warn(
					"Could not open the chat log binary! Chat logging will not be available for this session!", e);
		}
	}

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

		// Check moderator perms
		String permLevel = "member";
		if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
			permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
		}

		// Security checks
		// Check moderator perms
		if (!GameServer.hasPerm(permLevel, "moderator")) {
			// Ignore 'limbo' players
			Player gameClient = client.getPlayer().getOnlinePlayerInstance();
			if (gameClient == null) {
				// Ok-
				// Bye bye, you're not ingame
				client.disconnect();
				return true;
			} else if (!gameClient.roomReady || gameClient.room == null) {
				// Limbo player
				return true;
			}

			// Check room type
			//
			// If its not a mod and its a room the player isnt in, they shouldnt receive the
			// messages
			if ((!client.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.PRIVATE_CHAT)
					|| !manager.dmExists(room))
					&& !client.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.TRANSIENT_CHAT)) {
				// Check if sanctuary
				if (room.startsWith("sanctuary_")) {
					if (!gameClient.room.equals(room)) {
						// Invalid
						return true;
					}
				} else {
					// Check game room
					GameRoom gameRoom = Centuria.gameServer.getRoomManager().getRoom(room);
					if (gameRoom != null && gameRoom.getLevelID() != gameClient.levelID) {
						// Invalid
						return true;
					}
				}
			}
		}

		// Clean message
		message = replaceCaseInsensitive(message, "<noparse>", "");
		message = replaceCaseInsensitive(message, "</noparse>", "");
		message = replaceCaseInsensitive(message, "\\<noparse\\>", "");
		message = replaceCaseInsensitive(message, "\\</noparse\\>", "");
		message = replaceCaseInsensitive(message, "\\<noparse>", "");
		message = replaceCaseInsensitive(message, "\\</noparse>", "");
		message = replaceCaseInsensitive(message, "<noparse\\>", "");
		message = replaceCaseInsensitive(message, "</noparse\\>", "");
		message = message.trim();

		// Check content
		if (message.isBlank()) {
			return true; // ignore chat
		}

		// Fire event
		ChatMessageReceivedEvent evt = new ChatMessageReceivedEvent(client.getServer(), client.getPlayer(), client,
				message, room);
		EventBus.getInstance().dispatchEvent(evt);
		if (evt.isCancelled())
			return true; // Cancelled

		// Chat commands
		if (message.startsWith(">") || message.startsWith("/")) {
			String cmd = message.substring(1).trim();
			if (handleCommand(cmd, client))
				return true;
		}

		// Find type
		String type = "";

		// Find other player in this room first
		boolean found = false;
		for (ChatClient cl : client.getServer().getClients()) {
			if (cl.isInRoom(room)) {
				found = true;
				type = cl.getRoom(room).getType();
				break;
			}
		}

		// Find by room
		if (!found) {
			// Check sanctuary
			if (room.startsWith("sanctuary_")) {
				// Sanctuary
				type = ChatRoomTypes.ROOM_CHAT;
				found = true;
			} else {
				// Find room in room manager
				Player plr = client.getPlayer().getOnlinePlayerInstance();
				if (plr != null) {
					GameServer server = (GameServer) plr.client.getServer();
					if (server.getRoomManager().getRoom(room) != null) {
						// Found room chat
						room = ChatRoomTypes.ROOM_CHAT;
						found = true;
					}
				}

				// Check
				if (!found) {
					// DMs
					if (DMManager.getInstance().dmExists(room)) {
						// Found DM chat
						room = ChatRoomTypes.PRIVATE_CHAT;
						found = true;
					} else {
						// Transient
						room = ChatRoomTypes.TRANSIENT_CHAT;
						found = true;
					}
				}
			}
		}

		// Log
		if (!client.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.PRIVATE_CHAT)) {
			// Log to server log
			Centuria.logger.info("Chat: " + client.getPlayer().getDisplayName() + ": " + message);

			// Log to chat log
			if (chatLogBinary != null) {
				try {
					// Create entry
					// Room: string
					// Type: string
					// User ID: string
					// Message: string
					// Timestamp: long
					ByteArrayOutputStream bO = new ByteArrayOutputStream();
					DataWriter writer = new DataWriter(bO);
					writer.writeString(room);
					writer.writeString(client.getPlayer().getAccountID());
					writer.writeString(message);
					writer.writeLong(System.currentTimeMillis());
					synchronized (chatLogBinary) {
						writer = new DataWriter(chatLogBinary);
						writer.writeBytes(bO.toByteArray());
						chatLogBinary.flush();
					}
				} catch (IOException e) {
				}
			}
		}

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
		if (client.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.ROOM_CHAT)
				&& acc.getSaveSharedInventory().containsItem("penalty") && acc.getSaveSharedInventory()
						.getItem("penalty").getAsJsonObject().get("type").getAsString().equals("mute")) {
			JsonObject muteInfo = acc.getSaveSharedInventory().getItem("penalty").getAsJsonObject();
			if (muteInfo.get("unmuteTimestamp").getAsLong() == -1
					|| muteInfo.get("unmuteTimestamp").getAsLong() > System.currentTimeMillis()) {
				// Time format
				SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
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
		if (TextFilterService.getInstance().shouldFilterMute(message)) {
			// Mod log
			FilterResult fres = TextFilterService.getInstance().filter(message, false);
			String matchedWords = "";
			for (WordMatch match : fres.getMatches()) {
				if (match.getSeverity().ordinal() >= FilterSeverity.INSTAMUTE.ordinal()) {
					if (matchedWords.isEmpty())
						matchedWords = match.getMatchedPhrase();
					else
						matchedWords += ", " + match.getMatchedPhrase();
				}
			}
			EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.mute",
					"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
					Map.of("Chat message", message, "Matched word(s)", matchedWords, "Resulting action", "muted"),
					"SYSTEM", client.getPlayer()));

			// Mute
			client.getPlayer().mute(0, 0, 30, "SYSTEM",
					"Muted due to an illegal word said in the chat, we request you to keep your chat respectful, safe and clean!");

			// Send system message
			JsonObject res = new JsonObject();
			res.addProperty("conversationType", client.getRoom(room).getType());
			res.addProperty("conversationId", room);
			res.addProperty("message",
					"You have been automatically muted in public chat for violating the server rules, mute will last 30 minutes.\nReason: Muted due to an illegal word said in the chat, we request you to keep your chat respectful, safe and clean!");
			res.addProperty("source", NIL_UUID);// Time format
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			res.addProperty("sentAt", fmt.format(new Date()));
			res.addProperty("eventId", "chat.postMessage");
			res.addProperty("success", true);
			client.sendPacket(res);
			return true;
		}

		// Fire event
		ChatMessageBroadcastEvent evt2 = new ChatMessageBroadcastEvent(client.getServer(), client.getPlayer(), client,
				message, room, type);
		EventBus.getInstance().dispatchEvent(evt2);
		if (evt2.isCancelled())
			return true; // Cancelled

		// OC proxying
		String ocProxyName = null;

		// Get proxy session
		ProxySession session = client.getObject(ProxySession.class);
		if (session == null) {
			// Create if missing
			session = new ProxySession();
			client.addObject(session);
		}

		// Check sticky status
		ProxySession.RoomProxySession roomSes = null;
		if (session.roomSessions.containsKey(room)) {
			// Check sticky
			roomSes = session.roomSessions.get(room);
			if (roomSes.sticky) {
				// Update oc proxy thats being used
				ocProxyName = roomSes.lastUsedOcName;
			}
		}

		// Find proxy
		for (OcProxyMetadata md : client.getOcProxyMetadata()) {
			// Check message
			if (message.startsWith(md.prefix) && message.endsWith(md.suffix)) {
				// Found OC
				ocProxyName = md.name;

				// Update message
				message = message.substring(md.prefix.length());
				if (!md.suffix.isEmpty()) {
					// Remove suffix
					message = message.substring(0, message.lastIndexOf(md.suffix));
				}
				message = message.trim();

				// Check content
				if (message.isBlank()) {
					return true; // ignore chat
				}
				break;
			}
		}

		// Check result
		if (ocProxyName != null) {
			// Get proxy
			OcProxyInfo proxy = OcProxyInfo.ofUser(client.getPlayer(), ocProxyName);
			if (proxy != null) {
				// Update name string for it to be used in the chat itself
				ocProxyName = "<color=#00f7ff><noparse>" + proxy.displayName + "</noparse>"
						+ (proxy.characterPronouns.toLowerCase().equals("n/a")
								|| proxy.characterPronouns.toLowerCase().isEmpty() ? ""
										: " [<noparse>" + proxy.characterPronouns + "</noparse>]")
						+ "</color> <color=#daa520>[" + client.getPlayer().getDisplayName() + "]</color>";

				// Update sticky proxying
				if (roomSes != null)
					roomSes.lastUsedOcName = proxy.displayName;
			} else {
				// Refresh
				ocProxyName = null;
				client.reloadProxies();
			}
		}

		// Check room
		SocialManager socialManager = SocialManager.getInstance();
		if (client.isInRoom(room) || GameServer.hasPerm(permLevel, "moderator")) {
			// Time format
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

			// If it is a DM, save message
			if (client.isInRoom(room) && client.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.PRIVATE_CHAT)
					&& manager.dmExists(room)) {
				// Save message
				PrivateChatMessage msg = new PrivateChatMessage();
				msg.content = message;
				msg.sentAt = System.currentTimeMillis();
				msg.source = client.getPlayer().getAccountID();
				if (ocProxyName != null)
					msg.source = "plaintext:" + ocProxyName;
				manager.saveDMMessge(room, msg);
			}

			// Send to all in room
			Player cPlayer = client.getPlayer().getOnlinePlayerInstance();
			for (ChatClient receiver : client.getServer().getClients()) {
				// Fetch receiver moderator perms
				String permLevel2 = "member";
				if (receiver.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
					permLevel2 = receiver.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}

				// Check if in room
				if (receiver.isInRoom(room)) {
					// Check if the receiver has blocked the sender and that neither is a moderator
					if (!socialManager.socialListExists(receiver.getPlayer().getAccountID())
							|| !socialManager.getPlayerIsBlocked(receiver.getPlayer().getAccountID(),
									client.getPlayer().getAccountID())
							|| GameServer.hasPerm(permLevel2, "moderator")
							|| GameServer.hasPerm(permLevel, "moderator")) {
						// Check limbo player
						Player gameClient = receiver.getPlayer().getOnlinePlayerInstance();
						if ((gameClient != null && (!gameClient.roomReady || gameClient.room == null))
								|| (gameClient == null && !GameServer.hasPerm(permLevel2, "moderator")))
							continue;

						// Check ghost mode
						if (cPlayer != null && cPlayer.ghostMode && !GameServer.hasPerm(permLevel2, "moderator")
								&& client.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.ROOM_CHAT))
							continue;

						// Check if the sender has blocked this receiver, if so, prevent the receiver
						// from receiving the message that was sent, also applies to moderator-sent
						// messages unless its a dm and the player is a moderator
						if (socialManager.getPlayerIsBlocked(client.getPlayer().getAccountID(),
								receiver.getPlayer().getAccountID())) {
							// Check mod perms and room type
							if (GameServer.hasPerm(permLevel, "moderator")) {
								if (client.isInRoom(room)
										&& client.getRoom(room).getType().equals(ChatRoomTypes.ROOM_CHAT)) {
									continue; // Blocked
								}
							} else
								continue; // Blocked
						}

						// Load filter settings
						int filterSetting = 0;
						UserVarValue val = receiver.getPlayer().getSaveSpecificInventory().getUserVarAccesor()
								.getPlayerVarValue(9362, 0);
						if (val != null)
							filterSetting = val.value;
						boolean isStrict = filterSetting != 0;

						// Filter
						String filteredMessage = TextFilterService.getInstance().filterString(message, isStrict);

						// Send response
						JsonObject res = new JsonObject();
						res.addProperty("conversationType", type);
						res.addProperty("conversationId", room);
						res.addProperty("message", filteredMessage);
						if (GameServer.hasPerm(permLevel2, "moderator")
								&& receiver.getObject(ModeratorClient.class) != null)
							res.addProperty("unfilteredMessage", message);
						res.addProperty("source", client.getPlayer().getAccountID());
						res.addProperty("sentAt", fmt.format(new Date()));
						res.addProperty("eventId", "chat.postMessage");
						res.addProperty("success", true);
						if (ocProxyName != null) {
							res.addProperty("source", "plaintext:" + ocProxyName);
							res.addProperty("author", client.getPlayer().getAccountID());
						}

						// Send message
						receiver.sendPacket(res);
					}
				} else {
					// Not in room

					// Check moderator client
					if (receiver.getObject(ModeratorClient.class) != null) {
						if (GameServer.hasPerm(permLevel2, "moderator")) {
							// Send through centuria moderator protocol if needed
							if (!type.equals(ChatRoomTypes.PRIVATE_CHAT)) {
								// Load filter settings
								int filterSetting = 0;
								UserVarValue val = receiver.getPlayer().getSaveSpecificInventory().getUserVarAccesor()
										.getPlayerVarValue(9362, 0);
								if (val != null)
									filterSetting = val.value;
								boolean isStrict = filterSetting != 0;

								// Filter
								String filteredMessage = TextFilterService.getInstance().filterString(message,
										isStrict);

								// Send
								JsonObject res = new JsonObject();
								res.addProperty("eventId", "centuria.moderatorclient.postedMessageInOtherRoom");
								res.addProperty("conversationType", type);
								res.addProperty("conversationId", room);
								res.addProperty("message", filteredMessage);
								res.addProperty("unfilteredMessage", message);
								res.addProperty("source", client.getPlayer().getAccountID());
								res.addProperty("sentAt", fmt.format(new Date()));
								res.addProperty("success", true);
								if (ocProxyName != null) {
									res.addProperty("source", "plaintext:" + ocProxyName);
									res.addProperty("author", client.getPlayer().getAccountID());
								}

								// Send message
								receiver.sendPacket(res);
							}
						}
					}
				}
			}
		}

		return true;
	}

	private String replaceCaseInsensitive(String msg, String target, String replacement) {
		while (msg.toLowerCase().contains(target.toLowerCase())) {
			int i = msg.toLowerCase().indexOf(target.toLowerCase());
			msg = msg.substring(0, i) + replacement + msg.substring(i + target.length());
		}
		return msg;
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
		if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
			permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
		}

		// Generate the command list
		ArrayList<String> commandMessages = new ArrayList<String>();

		if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
				|| GameServer.hasPerm(permLevel, "admin"))
			commandMessages.add("giveBasicMaterials");
		if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
				|| GameServer.hasPerm(permLevel, "admin"))
			commandMessages.add("giveBasicCurrency");

		commandMessages.add("togglenameprefix");
		if (GameServer.hasPerm(permLevel, "moderator")) {
			commandMessages.add("toggleghostmode");
			commandMessages.add("toggletpoverride");
			commandMessages.add("kick \"<player>\" [\"<reason>\"]");
			commandMessages.add("ipban \"<player/address>\" [\"<reason>\"]");
			commandMessages.add("pardonip \"<ip>\"");
			commandMessages.add("permban \"<player>\" [\"<reason>\"]");
			commandMessages.add("tempban \"<player>\" <days>\" [\"<reason>\"]");
			commandMessages.add("forcenamechange \"<player>\"");
			commandMessages.add("changeothername \"<player>\" \"<new-name>\"");
			commandMessages.add("mute \"<player>\" <minutes> [hours] [days] [\"<reason>\"]");
			commandMessages.add("pardon \"<player>\" [\"<reason>\"]");
			commandMessages.add("xpinfo [\"<player>\"]");
			commandMessages.add("takexp <amount> [\"<player>\"]");
			commandMessages.add("resetxp [\"<player>\"]");
			commandMessages.add("takelevels <amount> [\"<player>\"]");
			commandMessages.add("takeitem <itemDefId> [<quantity>] [<player>]");
			commandMessages.add("questskip [<amount>] [<player>]");
			commandMessages.add("tpm <levelDefID> [<room id>] [<level type>] [\\\"<player>\\\"]");
			commandMessages.add("setplayertag \"<tag id>\" [\"<player>\"] [\"<escaped  tag json data>\"]");
			commandMessages.add("removeplayertag \"<tag id>\" [\"<player>\"]");
			if (GameServer.hasPerm(permLevel, "admin")) {
				commandMessages.add("generateclearancecode");
				commandMessages.add("addxp <amount> [\"<player>\"]");
				commandMessages.add("addlevels <amount> [\"<player>\"]");
				commandMessages.add("resetalllevels [confirm]");
				commandMessages.add("makeadmin \"<player>\"");
				commandMessages.add("makemoderator \"<player>\"");
				commandMessages.add("removeperms \"<player>\"");
				commandMessages.add("startmaintenance [\"<reason>\"]");
				commandMessages.add("endmaintenance");
				commandMessages.add("stopserver");
				commandMessages.add("updatewarning <minutes-remaining>");
				commandMessages.add("updateshutdown [\"<reason>\"]");
				commandMessages.add("shutdownserver [\"<reason>\"]");
				commandMessages.add("update <60|30|15|10|5|3|1>");
				commandMessages.add("cancelupdate");
			}
			if (GameServer.hasPerm(permLevel, "developer")) {
				commandMessages.add("makedeveloper \"<name>\"");
				commandMessages.add("srp \"<raw-packet>\" [<player>]");
			}
			commandMessages.add("staffroom");
			commandMessages.add("listplayers");
			commandMessages.add("listplayerpps");
			commandMessages.add("coordsof/coords [\"<player>\"]");
			commandMessages.add("tp [\"<player to teleport>\"] \"<target player>\"");
			commandMessages.add("tp [\"<player to teleport>\"] <x> <y> <z>");
			commandMessages.add("tpall \"<target player>\"");
			commandMessages.add("tpall <x> <y> <z>");
			commandMessages.add("tpserverto \"<target player>\"");
			commandMessages.add("gatherallplayers");
			commandMessages.add("endgathering");
		}
		if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemFurnitureItems
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemMods
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemSanctuaryTypes
				|| (GameServer.hasPerm(permLevel, "admin") || ((client.getPlayer().getSaveSpecificInventory()
						.getSaveSettings().allowGiveItemAvatars
						|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes
						|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
						|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemFurnitureItems
						|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemMods
						|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
						|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemSanctuaryTypes)
						&& GameServer.hasPerm(permLevel, "moderator"))))
			if (GameServer.hasPerm(permLevel, "moderator"))
				commandMessages.add("giveitem <itemDefId> [<quantity>] [<player>]");
			else
				commandMessages.add("giveitem <itemDefId> [<quantity>]");
		if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemFurnitureItems
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemMods
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemSanctuaryTypes
				|| GameServer.hasPerm(permLevel, "moderator"))
			if (GameServer.hasPerm(permLevel, "moderator"))
				commandMessages.add("removeallfiltereditems [<player>]");
			else
				commandMessages.add("removeallfiltereditems");
		commandMessages.add("questrewind <amount-of-quests-to-rewind>");

		// OC proxying
		commandMessages.add("");
		commandMessages.add("Character proxies:");
		commandMessages.add("oc show \"<name>\" [\"<player>\"]");
		commandMessages.add("oc list [\"<player>\"]");
		commandMessages.add("");
		commandMessages.add("Proxy management:");
		commandMessages.add(
				"oc register \"<name>\" \"[<trigger prefix>]message[<trigger suffix>]\" (eg. oc register \"Alice\" \"alice: message\")");
		commandMessages.add(
				"oc settrigger \"<name>\" \"[<trigger prefix>]message[<trigger suffix>]\" (eg. oc settrigger \"Alice\" \"alice: message\")");
		if (!GameServer.hasPerm(permLevel, "moderator"))
			commandMessages.add("oc rename \"<name>\" \"<new name>\"");
		else
			commandMessages.add("oc rename \"<name>\" \"<new name>\" [\"<player>\"]");
		if (!GameServer.hasPerm(permLevel, "moderator"))
			commandMessages.add("oc delete \"<name>\"");
		else
			commandMessages.add("oc delete \"<name>\" [\"<player>\"]");
		if (!GameServer.hasPerm(permLevel, "moderator"))
			commandMessages.add("oc bio \"<name>\" \"<new bio>\"");
		else
			commandMessages.add("oc bio \"<name>\" \"<new bio>\" [\"<player>\"]");
		if (!GameServer.hasPerm(permLevel, "moderator"))
			commandMessages.add("oc pronouns \"<name>\" \"<new pronouns>\"");
		else
			commandMessages.add("oc pronouns \"<name>\" \"<new pronouns>\" [\"<player>\"]");
		commandMessages.add("oc toggleprivate \"<name>\"");
		commandMessages.add("");
		commandMessages.add("Sticky proxy:");
		commandMessages.add("oc stickyproxy \"<name>\"");
		commandMessages.add("oc stickyoff");

		// Private instances
		commandMessages.add("");
		commandMessages.add("Private instances:");
		commandMessages.add("privinst list");
		commandMessages.add("privinst show #<number> (eg. privinst show #1)"); // TODO
		commandMessages.add("privinst leave #<number> (eg. privinst leave #1)"); // TODO
		commandMessages.add("privinst connect #<number> (eg. privinst connect #1)"); // TODO
		commandMessages.add("privinst disconnect"); // TODO
		commandMessages.add("");
		commandMessages.add("Private instance management:");
		commandMessages.add("privinst create \"<name>\" \"<description>\""); // TODO
		commandMessages.add("privinst rename #<number> \"<new name>\""); // TODO
		commandMessages.add("privinst setdescription #<number> \"<new description>\""); // TODO
		commandMessages.add("privinst allowinvites #<number> true/false"); // TODO
		commandMessages.add("privinst listmembers #<number>"); // TODO
		commandMessages.add("privinst kick #<number> \"<username to kick>\""); // TODO
		commandMessages.add("privinst makeowner #<number> \"<username of new owner>\""); // TODO
		commandMessages.add("privinst delete #<number>"); // TODO
		commandMessages.add("");
		commandMessages.add("Private instance invites:");
		commandMessages.add("privinst invite #<number> \"<username to invite>\""); // TODO
		commandMessages.add("privinst accept <invite-id>"); // TODO
		commandMessages.add("privinst decline <invite-id>"); // TODO
		commandMessages.add("privinst show <invite-id>"); // TODO
		commandMessages.add("privinst invites"); // TODO

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
					if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
							|| GameServer.hasPerm(permLevel, "admin")) {
						var onlinePlayer = client.getPlayer().getOnlinePlayerInstance();
						var accessor = client.getPlayer().getSaveSpecificInventory().getItemAccessor(onlinePlayer);

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
						return true;
					}
				} else if (cmdId.equals("togglenameprefix")) {
					// Name prefix
					if (client.getPlayer().getSaveSharedInventory().containsItem("prefixdisabled")) {
						// Enable prefix
						client.getPlayer().getSaveSharedInventory().deleteItem("prefixdisabled");
						systemMessage(
								"Name prefix and color re-enabled, note this does not fully apply until you and other players relog.",
								cmd, client);
					} else {
						// Disable prefix
						client.getPlayer().getSaveSharedInventory().setItem("prefixdisabled", new JsonObject());
						systemMessage(
								"Name prefix and color disabled, please note this does not fully apply until you and other players relog.",
								cmd, client);
					}
					return true;
				} else if (cmdId.equals("givebasiccurrency")) {
					if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
							|| GameServer.hasPerm(permLevel, "admin")) {
						var onlinePlayer = client.getPlayer().getOnlinePlayerInstance();
						var accessor = client.getPlayer().getSaveSpecificInventory().getCurrencyAccessor();

						accessor.addLikes(onlinePlayer == null ? null : onlinePlayer.client, 1000);
						accessor.addStarFragments(onlinePlayer == null ? null : onlinePlayer.client, 1000);

						// TODO: Check result
						systemMessage("You have been given 1000 star fragments and likes. Have fun!", cmd, client);
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
					JsonObject obj = client.getPlayer().getSaveSpecificInventory().getAccessor()
							.findInventoryObject("311", 22781);
					JsonObject progressionMap = obj.get("components").getAsJsonObject()
							.get("SocialExpanseLinearGenericQuestsCompletion").getAsJsonObject();
					JsonArray arr = progressionMap.get("completedQuests").getAsJsonArray();
					for (int i = 0; i < questsToRewind; i++) {
						arr.remove(arr.get(arr.size() - 1));
					}

					// Save
					client.getPlayer().getSaveSpecificInventory().setItem("311",
							client.getPlayer().getSaveSpecificInventory().getItem("311"));
					systemMessage("Success! Rewinded your quest log, '"
							+ QuestManager.getQuest(QuestManager.getActiveQuest(client.getPlayer())).name
							+ "' is now your active quest! Please log out and log back in to complete the process.",
							cmd, client);

					return true;
				} else if (cmdId.equals("oc") && args.size() >= 1) {
					String task = args.get(0).toLowerCase();
					switch (task) {

					// Register
					case "register": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to register a OC",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to register a OC",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Verify name with filters
						if (TextFilterService.getInstance().isFiltered(name, true, "USERNAMEFILTER")) {
							// Reply with error
							systemMessage("Invalid argument: name: this name was blocked as it may be inappropriate",
									cmd + " " + task, client);
							return true;
						}

						// Check arguments
						if (args.size() < 2) {
							// Missing argument
							systemMessage("Missing argument: trigger: the game needs to know when to use this OC.\n"
									+ "\n"
									+ "For a trigger, you need to create a template message, with the word 'message' to describe what the game must use as message content.\n"
									+ "\n"
									+ "Example: \"Alice: message\", usage example: \"alice: hi\", the chat would say hi as alice\n"
									+ "Another example: \"[[message]]\", usage example: \"[[some message]]\", the chat say \"some message\" as the OC tied to the trigger",
									cmd + " " + task, client);
							return true;
						}

						// Get trigger string
						String trigger = args.get(1);

						// Verify trigger
						if (!trigger.contains("message")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: trigger: missing the word 'message', the game needs to know when to use this OC.\n"
											+ "\n"
											+ "For a trigger, you need to create a template message, with the word 'message' to describe what the game must use as message content.\n"
											+ "\n"
											+ "Example: \"Alice: message\", usage example: \"alice: hi\", the chat would say hi as alice\n"
											+ "Another example: \"[[message]]\", usage example: \"[[some message]]\", the chat say \"some message\" as the OC tied to the trigger",
									cmd + " " + task, client);
							return true;
						}

						// Create the trigger
						String prefix = trigger.substring(0, trigger.indexOf("message"));
						String suffix = trigger.substring(trigger.indexOf("message") + "message".length());
						if (suffix.contains("message")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: trigger: unable to determine what instance of 'message' to use as trigger delimiter! Please use the word only once in a trigger text!",
									cmd + " " + task, client);
							return true;
						}
						if (prefix.isEmpty() && suffix.isEmpty()) {
							// Invalid argument
							systemMessage(
									"Invalid argument: trigger: please make sure to not only have 'message' in your trigger text",
									cmd + " " + task, client);
							return true;
						}

						// Verify OC existence
						if (OcProxyInfo.ocExists(client.getPlayer(), name)) {
							// Already exists
							systemMessage("Invalid argument: name: you already have a OC named " + name
									+ ", use `oc show` to look it up", cmd + " " + task, client);
							return true;
						}

						// Create OC
						OcProxyInfo.saveOc(client.getPlayer(), name, prefix, suffix);

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully created the OC " + name + "!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Trigger update
					case "settrigger": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Check arguments
						if (args.size() < 2) {
							// Missing argument
							systemMessage("Missing argument: trigger: the game needs to know when to use this OC.\n"
									+ "\n"
									+ "For a trigger, you need to create a template message, with the word 'message' to describe what the game must use as message content.\n"
									+ "\n"
									+ "Example: \"Alice: message\", usage example: \"alice: hi\", the chat would say hi as alice\n"
									+ "Another example: \"[[message]]\", usage example: \"[[some message]]\", the chat say \"some message\" as the OC tied to the trigger",
									cmd + " " + task, client);
							return true;
						}

						// Get trigger string
						String trigger = args.get(1);

						// Verify trigger
						if (!trigger.contains("message")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: trigger: missing the word 'message', the game needs to know when to use this OC.\n"
											+ "\n"
											+ "For a trigger, you need to create a template message, with the word 'message' to describe what the game must use as message content.\n"
											+ "\n"
											+ "Example: \"Alice: message\", usage example: \"alice: hi\", the chat would say hi as alice\n"
											+ "Another example: \"[[message]]\", usage example: \"[[some message]]\", the chat say \"some message\" as the OC tied to the trigger",
									cmd + " " + task, client);
							return true;
						}

						// Create the trigger
						String prefix = trigger.substring(0, trigger.indexOf("message"));
						String suffix = trigger.substring(trigger.indexOf("message") + "message".length());
						if (suffix.contains("message")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: trigger: unable to determine what instance of 'message' to use as trigger delimiter! Please use the word only once in a trigger text!",
									cmd + " " + task, client);
							return true;
						}
						if (prefix.isEmpty() && suffix.isEmpty()) {
							// Invalid argument
							systemMessage(
									"Invalid argument: trigger: please make sure to not only have 'message' in your trigger text",
									cmd + " " + task, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(client.getPlayer(), name)) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}

						// Update OC
						OcProxyInfo oc = OcProxyInfo.ofUser(client.getPlayer(), name);
						oc.triggerPrefix = prefix;
						oc.triggerSuffix = suffix;
						OcProxyInfo.saveOc(client.getPlayer(), oc);

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully updated trigger of OC " + name + "!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Rename
					case "rename": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Check arguments
						if (args.size() < 2) {
							// Missing argument
							systemMessage("Missing argument: new name", cmd + " " + task, client);
							return true;
						}

						// Get new name
						String newName = args.get(1);
						newName = newName.trim();
						if (newName.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: new name", cmd + " " + task, client);
							return true;
						}
						if (!newName.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: new name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Verify name with filters
						if (TextFilterService.getInstance().isFiltered(newName, true, "USERNAMEFILTER")) {
							// Reply with error
							systemMessage(
									"Invalid argument: new name: this name was blocked as it may be inappropriate",
									cmd + " " + task, client);
							return true;
						}

						// Find ID
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 3 && GameServer.hasPerm(permLevel, "moderator"))
							player = args.get(2);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Find account
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						if (acc == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(acc, name)) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}
						if (OcProxyInfo.ocExists(acc, newName)) {
							// Already exists
							systemMessage("Invalid argument: new name: name already in use by another OC",
									cmd + " " + task, client);
							return true;
						}

						// Update OC
						OcProxyInfo oc = OcProxyInfo.ofUser(acc, name);
						OcProxyInfo.deleteOc(acc, name);
						oc.displayName = newName;
						OcProxyInfo.saveOc(acc, oc);

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully updated the name of OC " + name + "!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Delete
					case "delete": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to delete a OC",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to delete a OC",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Find ID
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 2 && GameServer.hasPerm(permLevel, "moderator"))
							player = args.get(1);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Find account
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						if (acc == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(acc, name)) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}

						// Confirm
						if (!GameServer.hasPerm(permLevel, "moderator")
								&& (args.size() < 2 || !args.get(1).equals("confirm"))) {
							systemMessage("This command will delete the character " + name
									+ "!\nAre you sure you want to continue?\nAdd 'confirm' to the command to confirm your action.",
									cmd, client);
							return true;
						}

						// Delete OC
						OcProxyInfo.deleteOc(acc, name);

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully deleted the OC " + name + "!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Bio
					case "bio": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Bio argument
						// Check arguments
						if (args.size() < 2) {
							// Missing argument
							systemMessage("Missing argument: bio", cmd + " " + task, client);
							return true;
						}

						// Get bio
						String bio = args.get(1);
						bio = bio.trim();

						// Verify bio with filters
						if (TextFilterService.getInstance().isFiltered(bio, false)) {
							// Reply with error
							systemMessage("Invalid argument: bio: this bio was blocked as it may be inappropriate",
									cmd + " " + task, client);
							return true;
						}

						// Find ID
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 3 && GameServer.hasPerm(permLevel, "moderator"))
							player = args.get(2);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Find account
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						if (acc == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(acc, name)) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}

						// Update OC
						OcProxyInfo oc = OcProxyInfo.ofUser(acc, name);
						oc.characterBio = bio;
						OcProxyInfo.saveOc(acc, oc);

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully updated the bio of " + name + "!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Pronouns
					case "pronouns": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Pronouns argument
						// Check arguments
						if (args.size() < 2) {
							// Missing argument
							systemMessage("Missing argument: pronouns", cmd + " " + task, client);
							return true;
						}

						// Get pronouns
						String pronouns = args.get(1);
						pronouns = pronouns.trim();

						// Verify pronouns with filters
						if (TextFilterService.getInstance().isFiltered(pronouns, true)) {
							// Reply with error
							systemMessage(
									"Invalid argument: pronouns: these pronouns were blocked as they may be inappropriate",
									cmd + " " + task, client);
							return true;
						}

						// Find ID
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 3 && GameServer.hasPerm(permLevel, "moderator"))
							player = args.get(2);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Find account
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						if (acc == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(acc, name)) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}

						// Update OC
						OcProxyInfo oc = OcProxyInfo.ofUser(acc, name);
						oc.characterPronouns = pronouns;
						OcProxyInfo.saveOc(acc, oc);

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully updated the pronouns of " + name + "!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Sticky proxy
					case "stickyproxy": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to enable sticky proxy mode",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name toenable sticky proxy mode",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(client.getPlayer(), name)) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}

						// Enable
						ProxySession session = client.getObject(ProxySession.class);
						if (session == null) {
							// Create if missing
							session = new ProxySession();
							client.addObject(session);
						}
						ProxySession.RoomProxySession roomSes = session.roomSessions.get(room);
						if (roomSes == null) {
							// Create session
							roomSes = new ProxySession.RoomProxySession();
							session.roomSessions.put(room, roomSes);
						}
						roomSes.sticky = true;
						roomSes.lastUsedOcName = name;

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully enabled sticky proxying mode!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Sticky proxy
					case "stickyoff": {
						// Remove task argument
						args.remove(0);

						// Disable
						ProxySession session = client.getObject(ProxySession.class);
						if (session == null) {
							// Create if missing
							session = new ProxySession();
							client.addObject(session);
						}
						ProxySession.RoomProxySession roomSes = session.roomSessions.get(room);
						if (roomSes == null) {
							// Create session
							roomSes = new ProxySession.RoomProxySession();
							session.roomSessions.put(room, roomSes);
						}
						roomSes.sticky = false;

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage("Successfully disabled sticky proxying mode!", cmd + " " + task, client);

						// Return
						return true;
					}

					// Privacy
					case "toggleprivate": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to update a OC",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(client.getPlayer(), name)) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}

						// Toggle
						OcProxyInfo oc = OcProxyInfo.ofUser(client.getPlayer(), name);
						oc.publiclyVisible = !oc.publiclyVisible;
						OcProxyInfo.saveOc(client.getPlayer(), oc);

						// Reload
						client.reloadProxies();

						// Success!
						systemMessage(
								"Privacy status of OC " + oc.displayName + ": "
										+ (oc.publiclyVisible ? "publicly visible" : "private"),
								cmd + " " + task, client);

						// Return
						return true;
					}

					// Show
					case "show": {
						// Remove task argument
						args.remove(0);

						// Check arguments
						if (args.size() < 1) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to display OCs",
									cmd + " " + task, client);
							return true;
						}

						// Get name
						String name = args.get(0);
						name = name.trim();
						if (name.isEmpty()) {
							// Missing argument
							systemMessage("Missing argument: name: requiring a OC name to display OCs",
									cmd + " " + task, client);
							return true;
						}
						if (!name.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid argument
							systemMessage(
									"Invalid argument: name: contains invalid characters, can only be alphanumeric, contain spaces, dots, dashes and underscores",
									cmd + " " + task, client);
							return true;
						}

						// Find ID
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 2)
							player = args.get(1);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Find account
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						if (acc == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Verify OC existence
						if (!OcProxyInfo.ocExists(acc, name) || (!OcProxyInfo.ofUser(acc, name).publiclyVisible
								&& !GameServer.hasPerm(permLevel, "moderator"))) {
							// Already exists
							systemMessage("Invalid argument: name: could not find the OC", cmd + " " + task, client);
							return true;
						}

						// Get OC
						OcProxyInfo oc = OcProxyInfo.ofUser(acc, name);

						// Get filter settings
						int filterSetting = 0;
						UserVarValue val = client.getPlayer().getSaveSpecificInventory().getUserVarAccesor()
								.getPlayerVarValue(9362, 0);
						if (val != null)
							filterSetting = val.value;
						boolean isStrict = filterSetting != 0;

						// Filter
						String filteredBio = TextFilterService.getInstance().filterString(oc.characterBio, isStrict);

						// Display
						systemMessage("Overview of " + name + ":\n" + "\nName: " + oc.displayName + "\nPronouns: "
								+ oc.characterPronouns
								+ (uuid.equals(client.getPlayer().getAccountID())
										? "\nTrigger: </noparse><mark><noparse>" + oc.triggerPrefix + "message"
												+ oc.triggerSuffix + "</noparse></mark><noparse>"
										: "")
								+ "\n" + "\nBio:" + "\n" + filteredBio, cmd + " " + task, client);

						// Return
						return true;
					}

					// List
					case "list": {
						// Remove task argument
						args.remove(0);

						// Find ID
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 1)
							player = args.get(0);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// Find account
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						if (acc == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}

						// List ocs
						String msg = "List of OCs:";
						for (OcProxyInfo oc : OcProxyInfo.allOfUser(acc)) {
							// Check privacy
							if (oc.publiclyVisible || GameServer.hasPerm(permLevel, "moderator"))
								msg += "\n - " + oc.displayName;
						}
						systemMessage(msg, cmd, client);

						// Return
						return true;
					}

					default: {
						cmd = cmd + " " + task;
						break;
					}

					}
				} else if (cmdId.equals("privinst") && args.size() >= 1) {
					String task = args.get(0).toLowerCase();
					switch (task) {

					// List
					case "list": {
						// Remove task argument
						args.remove(0);

						// List private instances
						int i = 1;
						String msg = "List of private instances:";
						for (PrivateInstance inst : Centuria.gameServer.getPrivateInstanceManager()
								.getJoinedInstancesOf(client.getPlayer().getAccountID())) {
							// Find owner
							String owner = inst.getOwnerID();
							CenturiaAccount ownerAcc = AccountManager.getInstance().getAccount(owner);
							if (ownerAcc != null)
								owner = ownerAcc.getDisplayName();
							msg += " - #" + i++ + " - " + inst.getName() + " (owned by " + owner + ")";
						}
						systemMessage(msg, cmd, client);

						// Return
						return true;
					}

					default: {
						cmd = cmd + " " + task;
						break;
					}

					}
				}

				// Run system command
				if (GameServer.hasPerm(permLevel, "moderator")) {
					switch (cmdId) {

					//
					// Moderator commands below
					case "listplayerpps": {
						// Player packet-per-second rates of game
						String response = "Player packet-per-second rates for game clients:";
						for (Player plr : Centuria.gameServer.getPlayers())
							response += "\n - " + plr.account.getDisplayName() + " - current: "
									+ plr.client.getPacketsPerSecondRate() + " - peak: "
									+ plr.client.getHighestPacketsPerSecondRate();

						// Player packet-per-second rates of chat
						response += "\n\nChat packet-per-second rates:";
						for (ChatClient plr : Centuria.chatServer.getClients())
							response += "\n - " + plr.getPlayer().getDisplayName() + " - current: "
									+ plr.getPacketsPerSecondRate() + " - peak: "
									+ plr.getHighestPacketsPerSecondRate();

						// Player packet-per-second rates of voice chat
						response += "\n\nVoice chat packet-per-second rates:";
						for (VoiceChatClient plr : Centuria.voiceChatServer.getClients())
							response += "\n - " + plr.getPlayer().getDisplayName() + " - current: "
									+ plr.getPacketsPerSecondRate() + " - peak: "
									+ plr.getHighestPacketsPerSecondRate();

						// Send response
						systemMessage(response, cmd, client);
						return true;
					}
					case "listplayers": {
						// Create list
						// Load spawn helper
						JsonObject helper = null;
						try {
							// Load helper
							InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
									.getResourceAsStream("content/world/spawns.json");
							helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
									.get("Maps").getAsJsonObject();
							strm.close();
						} catch (Exception e) {
						}

						// Locate suspicious clients from chat server
						ArrayList<String> mapLessClients = new ArrayList<String>();
						HashMap<CenturiaAccount, String> suspiciousClients = new HashMap<CenturiaAccount, String>();
						for (ChatClient cl : client.getServer().getClients()) {
							if (!mapLessClients.contains(cl.getPlayer().getAccountID())) {
								Player plr = cl.getPlayer().getOnlinePlayerInstance();
								if (plr == null) {
									// Check perms
									String permLevel2 = "member";
									if (cl.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
										permLevel2 = cl.getPlayer().getSaveSharedInventory().getItem("permissions")
												.getAsJsonObject().get("permissionLevel").getAsString();
									}
									if (GameServer.hasPerm(permLevel2, "moderator"))
										continue;

									// No game server
									mapLessClients.add(cl.getPlayer().getAccountID());
									suspiciousClients.put(cl.getPlayer(), "no gameserver connection");
								} else if ((!plr.roomReady || plr.room == null) && plr.levelID != 25280) {
									// In limbo
									mapLessClients.add(cl.getPlayer().getAccountID());
									suspiciousClients.put(cl.getPlayer(), "limbo");
								}
							}
						}

						// Limbo clients from game server
						for (Player plr : Centuria.gameServer.getPlayers()) {
							if (!mapLessClients.contains(plr.account.getAccountID())) {
								if ((!plr.roomReady || plr.room == null) && plr.levelID != 25280) {
									// In limbo
									mapLessClients.add(plr.account.getAccountID());
									suspiciousClients.put(plr.account, "limbo");
								}
							}
						}

						// Find level IDs
						int ingame = 0;
						ArrayList<String> playerIDs = new ArrayList<String>();
						ArrayList<Integer> levelIDs = new ArrayList<Integer>();
						HashMap<Integer, ArrayList<String>> rooms = new HashMap<Integer, ArrayList<String>>();
						HashMap<Player, String> playersInRooms = new HashMap<Player, String>();
						for (Player plr : Centuria.gameServer.getPlayers()) {
							if (!playerIDs.contains(plr.account.getAccountID())
									&& !mapLessClients.contains(plr.account.getAccountID())
									&& (plr.roomReady || plr.levelID == 25280)) {
								// Increase count
								playerIDs.add(plr.account.getAccountID());
								ingame++;

								// Add level if missing
								if (!levelIDs.contains(plr.levelID)) {
									levelIDs.add(plr.levelID);
									rooms.put(plr.levelID, new ArrayList<String>());
								}

								// Add to room map
								if (plr.room != null)
									playersInRooms.put(plr, plr.room);

								// Get room list
								ArrayList<String> rLst = rooms.get(plr.levelID);

								// Find room instances
								GameRoom room = plr.getRoom();
								if (room != null && room.getLevelID() == plr.levelID
										&& !rLst.contains(room.getInstanceID())) {
									rLst.add(room.getInstanceID());
								}
							}
						}

						// Build message
						String response = Centuria.gameServer.getPlayers().length + " player(s) connected, " + ingame
								+ " player(s) in world:";

						// Add each level
						playerIDs = new ArrayList<String>();
						for (int levelID : levelIDs) {
							// Determine map name
							String map = "UNKOWN: " + levelID;
							if (levelID == 25280)
								map = "Tutorial [" + levelID + "]";
							else if (helper.has(Integer.toString(levelID)))
								map = helper.get(Integer.toString(levelID)).getAsString() + " [" + levelID + "]";

							// Find rooms
							for (String roomID : rooms.get(levelID)) {
								// Find players in rooms
								for (Player plr : playersInRooms.keySet()) {
									if (!playerIDs.contains(plr.account.getAccountID())) {
										// Make sure it doesnt get added more than once
										playerIDs.add(plr.account.getAccountID());

										// Check
										GameRoom room = plr.getRoom();
										if (room != null && room.getLevelID() == levelID
												&& room.getInstanceID().equals(roomID)) {
											// Add to response
											response += "\n- " + plr.account.getDisplayName() + " - " + map + " - room "
													+ room.getInstanceID() + (plr.ghostMode ? " [GHOSTING]" : "");

											// Check suspicious
											Optional<CenturiaAccount> susAcc = suspiciousClients.keySet().stream()
													.filter(t -> t.getAccountID().equals(plr.account.getAccountID()))
													.findFirst();
											if (susAcc.isPresent()) {
												// Note it
												response += " [ WARNING: " + suspiciousClients.get(susAcc.get()) + " ]";
											}
										}
									}
								}
							}

							// Players in other rooms
							for (Player plr : playersInRooms.keySet()) {
								String plrRoom = playersInRooms.get(plr);
								if (!mapLessClients.contains(plr.account.getAccountID())
										&& !playerIDs.contains(plr.account.getAccountID())) {
									// Check
									GameRoom room = ((GameServer) plr.client.getServer()).getRoomManager()
											.getRoom(plrRoom);
									if (room == null && plr.levelID == levelID) {
										// Add to response
										response += "\n - " + plr.account.getDisplayName() + " - " + map
												+ (plr.ghostMode ? " [GHOSTING]" : "");

										// Check suspicious
										Optional<CenturiaAccount> susAcc = suspiciousClients.keySet().stream()
												.filter(t -> t.getAccountID().equals(plr.account.getAccountID()))
												.findFirst();
										if (susAcc.isPresent()) {
											// Note it
											response += " [ WARNING: " + suspiciousClients.get(susAcc.get()) + " ]";
										}
									}
								}
							}
						}

						// Add suspicious clients
						if (suspiciousClients.size() != 0) {
							String susClientsStr = "";
							susClientsStr += "\n";
							susClientsStr += "\nSuspicious clients:";
							for (CenturiaAccount acc : suspiciousClients.keySet()) {
								// Add
								susClientsStr += "\n - " + acc.getDisplayName() + " [" + suspiciousClients.get(acc)
										+ "]";
							}
							response += susClientsStr;
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
								days = Integer.valueOf(args.get(3));
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
						if (acc.getSaveSharedInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer.hasPerm(acc.getSaveSharedInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "admin")
											&& !GameServer.hasPerm(permLevel, "admin")) {
								systemMessage("Unable to mute higher-ranking users.", cmd, client);
								return true;
							}
						}

						// Check if banned
						if (acc.getSaveSharedInventory().containsItem("penalty") && acc.getSaveSharedInventory()
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
						if (acc.getSaveSharedInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer.hasPerm(acc.getSaveSharedInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "admin")
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
						if (acc.getSaveSharedInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer.hasPerm(acc.getSaveSharedInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "admin")
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

						// Check clearance
						if (!GameServer.hasPerm(permLevel, "admin")) {
							// Check arguments
							if (args.size() < 3) {
								systemMessage(
										"Error: clearance code required, please add a admin-issued clearance code to the command AFTER the reason for the IP ban.",
										cmd, client);
								return true;
							}

							// Check code
							synchronized (clearanceCodes) {
								if (clearanceCodes.contains(args.get(2))) {
									clearanceCodes.remove(args.get(2));
								} else {
									systemMessage("Error: invalid clearance code.", cmd, client);
									return true;
								}
							}
						}

						// Find player
						for (Player plr : Centuria.gameServer.getPlayers()) {
							if (plr.account.getDisplayName().equals(args.get(0))) {
								// Check rank
								if (plr.account.getSaveSharedInventory().containsItem("permissions")) {
									if ((GameServer.hasPerm(
											plr.account.getSaveSharedInventory().getItem("permissions")
													.getAsJsonObject().get("permissionLevel").getAsString(),
											"developer") && !GameServer.hasPerm(permLevel, "developer"))
											|| GameServer.hasPerm(
													plr.account.getSaveSharedInventory().getItem("permissions")
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
								if (plr.client.getAddress().equals(args.get(0))) {
									// Ban player
									plr.account.ban(client.getPlayer().getAccountID(), reason);
								}
							}

							// Log completion
							systemMessage("Banned IP: " + args.get(0), cmd, client);

							return true;
						} catch (Exception e) {
						}

						// Player not found
						systemMessage("Player is not online.", cmd, client);
						return true;
					}
					case "staffroom": {
						// Teleport to staff room

						// Find online player
						Player plr = client.getPlayer().getOnlinePlayerInstance();
						if (plr != null) {
							// Teleport
							GameRoom room = ((GameServer) plr.client.getServer()).getRoomManager()
									.getOrCreateRoom(plr.pendingLevelID, "STAFFROOM");
							plr.teleportToRoom(1718, 0, 0, room.getID(), "");
							return true;
						}

						// Player not found
						systemMessage("Player is not online.", cmd, client);
						return true;
					}
					case "pardonip": {
						// Remove IP ban
						if (args.size() < 1) {
							systemMessage("Missing argument: ip", cmd, client);
							return true;
						}

						// Check clearance
						if (!GameServer.hasPerm(permLevel, "admin")) {
							// Check arguments
							if (args.size() < 2) {
								systemMessage(
										"Error: clearance code required, please add a admin-issued clearance code to the command.",
										cmd, client);
								return true;
							}

							// Check code
							synchronized (clearanceCodes) {
								if (clearanceCodes.contains(args.get(1))) {
									clearanceCodes.remove(args.get(1));
								} else {
									systemMessage("Error: invalid clearance code.", cmd, client);
									return true;
								}
							}
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
						if (acc.getSaveSharedInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer.hasPerm(acc.getSaveSharedInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "admin")
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
						if (acc.getSaveSharedInventory().containsItem("permissions")) {
							if ((GameServer
									.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
											.get("permissionLevel").getAsString(), "developer")
									&& !GameServer.hasPerm(permLevel, "developer"))
									|| GameServer.hasPerm(acc.getSaveSharedInventory().getItem("permissions")
											.getAsJsonObject().get("permissionLevel").getAsString(), "admin")
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

						// Prevent old name from being used
						AccountManager.getInstance().releaseDisplayName(oldName);
						AccountManager.getInstance().lockDisplayName(oldName, "-1");

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
								if (plr.account.getSaveSharedInventory().containsItem("permissions")) {
									if ((GameServer.hasPerm(
											plr.account.getSaveSharedInventory().getItem("permissions")
													.getAsJsonObject().get("permissionLevel").getAsString(),
											"developer") && !GameServer.hasPerm(permLevel, "developer"))
											|| GameServer.hasPerm(
													plr.account.getSaveSharedInventory().getItem("permissions")
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
								if (cl.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
									if ((GameServer.hasPerm(
											cl.getPlayer().getSaveSharedInventory().getItem("permissions")
													.getAsJsonObject().get("permissionLevel").getAsString(),
											"developer") && !GameServer.hasPerm(permLevel, "developer"))
											|| GameServer.hasPerm(
													cl.getPlayer().getSaveSharedInventory().getItem("permissions")
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
						if (plr == null) {
							systemMessage("Teleport overrides cannot be toggled unless you are ingame.", cmd, client);
							return true;
						}
						if (plr.overrideTpLocks) {
							plr.overrideTpLocks = false;
							systemMessage(
									"Teleport override disabled. The system will no longer ignore follower settings.",
									cmd, client);
							EventBus.getInstance().dispatchEvent(new MiscModerationEvent("tpoverride.disabled",
									"Teleport Override Disabled", Map.of("Teleport override status", "Disabled"),
									plr.account.getAccountID(), null));
						} else {
							// Check clearance
							if (!GameServer.hasPerm(permLevel, "admin")) {
								// Check arguments
								if (args.size() < 1) {
									systemMessage(
											"Error: clearance code required, please add a admin-issued clearance code to the command.",
											cmd, client);
									return true;
								}

								// Check code
								synchronized (clearanceCodes) {
									if (clearanceCodes.contains(args.get(0))) {
										clearanceCodes.remove(args.get(0));
									} else {
										systemMessage("Error: invalid clearance code.", cmd, client);
										return true;
									}
								}
							}

							plr.overrideTpLocks = true;
							systemMessage("Teleport override enabled. The system will ignore follower settings.", cmd,
									client);
							EventBus.getInstance()
									.dispatchEvent(new MiscModerationEvent("tpoverride.enabled",
											"Teleport Override Enabled", Map.of("Teleport override status", "Enabled"),
											plr.account.getAccountID(), null));
						}
						return true;
					}
					case "toggleghostmode": {
						// Ghost mode
						Player plr = client.getPlayer().getOnlinePlayerInstance();
						if (plr == null) {
							systemMessage("Ghost mode cannot be toggled unless you are ingame.", cmd, client);
							return true;
						}
						if (plr.ghostMode) {
							plr.ghostMode = false;

							// Spawn for everyone in room
							GameServer server = (GameServer) plr.client.getServer();
							for (Player player : server.getPlayers()) {
								if (plr.room != null && player.room != null && player.room.equals(plr.room)
										&& player != plr) {
									plr.syncTo(player, WorldObjectMoverNodeType.InitPosition);
									Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"), "Syncing player "
											+ player.account.getDisplayName() + " to " + plr.account.getDisplayName());
								}
							}

							systemMessage("Ghost mode disabled. You are visible to everyone.", cmd, client);
							EventBus.getInstance()
									.dispatchEvent(new MiscModerationEvent("ghostmode.disabled", "Ghost Mode Disabled",
											Map.of("Ghost mode status", "Disabled"), plr.account.getAccountID(), null));

							// Delete ghost mode file
							plr.account.getSaveSharedInventory().deleteItem("ghostmode");
						} else {
							// Enable ghost mode
							plr.ghostMode = true;

							// Despawn for everyone in room
							GameServer server = (GameServer) plr.client.getServer();
							for (Player player : server.getPlayers()) {
								if (plr.room != null && player.room != null && player.room.equals(plr.room)
										&& player != plr && !player.hasModPerms) {
									plr.destroyAt(player);
									Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"),
											"Removing player " + player.account.getDisplayName() + " from "
													+ plr.account.getDisplayName());
								}
							}

							systemMessage("Ghost mode enabled. You are now invisible to non-moderators.", cmd, client);
							EventBus.getInstance()
									.dispatchEvent(new MiscModerationEvent("ghostmode.enabled", "Ghost Mode Enabled",
											Map.of("Ghost mode status", "Enabled"), plr.account.getAccountID(), null));

							// Keep enabled even after logout
							plr.account.getSaveSharedInventory().setItem("ghostmode", new JsonObject());
						}

						return true;
					}

					//
					// Admin commands below
					case "generateclearancecode": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							long codeLong = rnd.nextLong();
							String code = "";
							while (true) {
								while (codeLong < 10000)
									codeLong = rnd.nextLong();
								code = Long.toString(codeLong, 16);
								synchronized (clearanceCodes) {
									if (!clearanceCodes.contains(code))
										break;
								}
								code = Long.toString(rnd.nextLong(), 16);
							}
							synchronized (clearanceCodes) {
								clearanceCodes.add(code);
							}
							EventBus.getInstance()
									.dispatchEvent(new MiscModerationEvent("clearancecode.generated",
											"Admin Clearance Code Generated", Map.of(),
											client.getPlayer().getAccountID(), null));
							systemMessage("Clearance code generated: " + code + "\nIt will expire in 2 minutes.", cmd,
									client);
							final String cFinal = code;
							Thread th = new Thread(() -> {
								for (int i = 0; i < 12000; i++) {
									synchronized (clearanceCodes) {
										if (!clearanceCodes.contains(cFinal))
											return;
									}
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
									}
								}
								synchronized (clearanceCodes) {
									clearanceCodes.remove(cFinal);
								}
							}, "Clearance code expiry");
							th.setDaemon(true);
							th.start();
							return true;
						}
					}
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

							// Get permissions
							String permLevel2 = "member";
							if (acc.getSaveSharedInventory().containsItem("permissions")) {
								permLevel2 = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
										.get("permissionLevel").getAsString();
							}

							// Check
							if (acc.getSaveSharedInventory().containsItem("permissions")) {
								if (GameServer
										.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
												.get("permissionLevel").getAsString(), "developer")
										&& !GameServer.hasPerm(permLevel, "developer")) {
									systemMessage("Unable to demote higher-ranking users.", cmd, client);
									return true;
								}
							}

							// Make admin
							if (!acc.getSaveSharedInventory().containsItem("permissions"))
								acc.getSaveSharedInventory().setItem("permissions", new JsonObject());
							if (!acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
									.has("permissionLevel"))
								acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
										.remove("permissionLevel");
							acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
									.addProperty("permissionLevel", "admin");
							acc.getSaveSharedInventory().setItem("permissions",
									acc.getSaveSharedInventory().getItem("permissions"));

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getSaveSharedInventory().setItem("permissions",
											acc.getSaveSharedInventory().getItem("permissions"));
									break;
								}
							}

							// Log
							EventBus.getInstance().dispatchEvent(new MiscModerationEvent("permissions.update",
									"Made " + acc.getDisplayName() + " administrator!",
									Map.of("Former permission level", permLevel2, "New permission level", "admin"),
									client.getPlayer().getAccountID(), acc));

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

							// Check
							if (acc.getSaveSharedInventory().containsItem("permissions")) {
								if (GameServer
										.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
												.get("permissionLevel").getAsString(), "developer")
										&& !GameServer.hasPerm(permLevel, "developer")) {
									systemMessage("Unable to demote higher-ranking users.", cmd, client);
									return true;
								}
							}

							// Get permissions
							String permLevel2 = "member";
							if (acc.getSaveSharedInventory().containsItem("permissions")) {
								permLevel2 = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
										.get("permissionLevel").getAsString();
							}

							// Make moderator
							if (!acc.getSaveSharedInventory().containsItem("permissions"))
								acc.getSaveSharedInventory().setItem("permissions", new JsonObject());
							if (!acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
									.has("permissionLevel"))
								acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
										.remove("permissionLevel");
							acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
									.addProperty("permissionLevel", "moderator");
							acc.getSaveSharedInventory().setItem("permissions",
									acc.getSaveSharedInventory().getItem("permissions"));

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getSaveSharedInventory().setItem("permissions",
											acc.getSaveSharedInventory().getItem("permissions"));
									break;
								}
							}

							// Log
							EventBus.getInstance().dispatchEvent(new MiscModerationEvent("permissions.update",
									"Made " + acc.getDisplayName() + " moderator!",
									Map.of("Former permission level", permLevel2, "New permission level", "moderator"),
									client.getPlayer().getAccountID(), acc));

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

							// Get permissions
							String permLevel2 = "member";
							if (acc.getSaveSharedInventory().containsItem("permissions")) {
								permLevel2 = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
										.get("permissionLevel").getAsString();
							}

							// Take permissions away
							if (acc.getSaveSharedInventory().containsItem("permissions")) {
								if (GameServer
										.hasPerm(acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
												.get("permissionLevel").getAsString(), "developer")
										&& !GameServer.hasPerm(permLevel, "developer")) {
									systemMessage("Unable to remove permissions from higher-ranking users.", cmd,
											client);
									return true;
								}
								acc.getSaveSharedInventory().deleteItem("permissions");
							}

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getSaveSharedInventory().deleteItem("permissions");
									break;
								}
							}

							// Find online player
							for (Player plr : Centuria.gameServer.getPlayers()) {
								if (plr.account.getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.account.getSaveSharedInventory().deleteItem("permissions");
									plr.hasModPerms = false;
									break;
								}
							}

							// Log
							EventBus.getInstance().dispatchEvent(new MiscModerationEvent("permissions.update",
									"Removed all permissions from " + acc.getDisplayName() + "!",
									Map.of("Former permission level", permLevel2, "New permission level", "member"),
									client.getPlayer().getAccountID(), acc));

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
							for (Player plr : Centuria.gameServer.getPlayers()) {
								// Dispatch event
								EventBus.getInstance().dispatchEvent(new AccountDisconnectEvent(plr.account,
										args.size() >= 1 ? args.get(0) : null, DisconnectType.SERVER_SHUTDOWN));
							}
							Centuria.updateShutdown();
							return true;
						} else {
							break;
						}
					}
					case "shutdownserver": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							// Shut down the server
							for (Player plr : Centuria.gameServer.getPlayers()) {
								// Dispatch event
								EventBus.getInstance().dispatchEvent(new AccountDisconnectEvent(plr.account,
										args.size() >= 1 ? args.get(0) : null, DisconnectType.SERVER_SHUTDOWN));
							}
							Centuria.disconnectPlayersForShutdown();
							System.exit(0);
							return true;
						} else {
							break;
						}
					}
					case "stopserver": {
						// Check perms
						if (GameServer.hasPerm(permLevel, "admin")) {
							// Shut down the server
							for (Player plr : Centuria.gameServer.getPlayers()) {
								// Dispatch event
								EventBus.getInstance().dispatchEvent(new AccountDisconnectEvent(plr.account,
										"Server has been shut down.", DisconnectType.SERVER_SHUTDOWN));
							}
							Centuria.disconnectPlayersForShutdown();
							System.exit(0);
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
								if (!plr.account.getSaveSharedInventory().containsItem("permissions")
										|| !GameServer.hasPerm(
												plr.account.getSaveSharedInventory().getItem("permissions")
														.getAsJsonObject().get("permissionLevel").getAsString(),
												"admin")) {
									// Dispatch event
									EventBus.getInstance().dispatchEvent(new AccountDisconnectEvent(plr.account,
											args.size() >= 1 ? args.get(0) : null, DisconnectType.MAINTENANCE));

									plr.client.sendPacket("%xt%ua%-1%__FORCE_RELOGIN__%");
								}
							}

							// Wait a bit
							int i = 0;
							while (Stream.of(Centuria.gameServer.getPlayers())
									.filter(plr -> !plr.account.getSaveSharedInventory().containsItem("permissions")
											|| !GameServer.hasPerm(
													plr.account.getSaveSharedInventory().getItem("permissions")
															.getAsJsonObject().get("permissionLevel").getAsString(),
													"admin"))
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
								if (!plr.account.getSaveSharedInventory().containsItem("permissions")
										|| !GameServer.hasPerm(
												plr.account.getSaveSharedInventory().getItem("permissions")
														.getAsJsonObject().get("permissionLevel").getAsString(),
												"admin")) {
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

							// Get permissions
							String permLevel2 = "member";
							if (acc.getSaveSharedInventory().containsItem("permissions")) {
								permLevel2 = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
										.get("permissionLevel").getAsString();
							}

							// Make developer
							if (!acc.getSaveSharedInventory().containsItem("permissions"))
								acc.getSaveSharedInventory().setItem("permissions", new JsonObject());
							if (!acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
									.has("permissionLevel"))
								acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
										.remove("permissionLevel");
							acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
									.addProperty("permissionLevel", "developer");
							acc.getSaveSharedInventory().setItem("permissions",
									acc.getSaveSharedInventory().getItem("permissions"));

							// Find online player
							for (ChatClient plr : client.getServer().getClients()) {
								if (plr.getPlayer().getDisplayName().equals(args.get(0))) {
									// Update inventory
									plr.getPlayer().getSaveSharedInventory().setItem("permissions",
											acc.getSaveSharedInventory().getItem("permissions"));
									break;
								}
							}

							// Log
							EventBus.getInstance().dispatchEvent(new MiscModerationEvent("permissions.update",
									"Made " + acc.getDisplayName() + " developer!",
									Map.of("Former permission level", permLevel2, "New permission level", "developer"),
									client.getPlayer().getAccountID(), acc));

							// Completed
							systemMessage("Made " + acc.getDisplayName() + " developer.", cmd, client);
							return true;
						} else {
							break;
						}
					}
					case "setplayertag": {
						// Tag management
						String id = "";
						if (args.size() < 1) {
							systemMessage("Missing argument: tag ID", cmd, client);
							return true;
						}

						// Parse arguments
						id = args.get(0);
						if (!id.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid ID
							systemMessage("Invalid argument: ID: invalid tag ID", cmd, client);
							return true;
						}

						// Tag value
						JsonObject value = new JsonObject();
						if (args.size() >= 3) {
							try {
								value = JsonParser.parseString(args.get(2)).getAsJsonObject();
							} catch (Exception e) {
								// Invalid value
								systemMessage("Invalid argument: value: invalid JSON data", cmd, client);
								return true;
							}
						}

						// Find player
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 2)
							player = args.get(1);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Update
						acc.setAccountTag(id, value);
						systemMessage("Tag updated successfully.", cmd, client);
						return true;
					}
					case "removeplayertag": {
						// Tag management
						String id = "";
						if (args.size() < 1) {
							systemMessage("Missing argument: tag ID", cmd, client);
							return true;
						}

						// Parse arguments
						id = args.get(0);
						if (!id.matches("^[A-Za-z0-9_\\-. ]+")) {
							// Invalid ID
							systemMessage("Invalid argument: ID: invalid tag ID", cmd, client);
							return true;
						}

						// Find player
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 2)
							player = args.get(1);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Update
						if (acc.getAccountTag(id) == null) {
							// Tag not found
							systemMessage("Specified tag could not be found for this player.", cmd, client);
							return true;
						}
						acc.deleteAccountTag(id);
						systemMessage("Tag removed successfully.", cmd, client);
						return true;
					}

					case "coords":
					case "coordsof": {
						// Coordinate tool

						// Find player
						String player = client.getPlayer().getDisplayName();
						if (args.size() >= 1)
							player = args.get(0);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						Player plr = acc.getOnlinePlayerInstance();
						if (plr == null || !plr.roomReady) {
							// Player not found
							systemMessage("Specified player is not online or not fully in world yet.", cmd, client);
							return true;
						}

						// Show
						systemMessage("Coordinates of " + acc.getDisplayName() + ":" //
								+ "\n - Room ID: " + plr.room //
								+ "\n - Level ID: " + plr.levelID //
								+ "\n - Level type: " + plr.levelType //
								+ "\n - Position XYZ: " + plr.lastPos.x + " " + plr.lastPos.y + " " + plr.lastPos.z //
								+ "\n - Rotation XYZW: " + plr.lastRot.x + " " + plr.lastRot.y + " " + plr.lastRot.z
								+ " " + plr.lastRot.w + " " //
								, cmd, client);
						return true;
					}

					case "tp": {
						// Teleport tool

						// Find player
						String player = client.getPlayer().getDisplayName();
						if (args.size() == 2 || args.size() >= 4) {
							player = args.get(0);
							args.remove(0);
						}
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						Player plr = acc.getOnlinePlayerInstance();
						if (plr == null || !plr.roomReady) {
							// Player not found
							systemMessage("Specified player is not online or not fully in world yet.", cmd, client);
							return true;
						}

						// Determine mode
						if (args.size() >= 3) {
							try {
								// Coordinates
								if (!args.get(0).matches("^[\\-0-9\\.]+$")) {
									systemMessage("Invalid argument: X: invalid value", cmd, client);
									return true;
								}
								if (!args.get(1).matches("^[\\-0-9\\.]+$")) {
									systemMessage("Invalid argument: Y: invalid value", cmd, client);
									return true;
								}
								if (!args.get(2).matches("^[\\-0-9\\.]+$")) {
									systemMessage("Invalid argument: Z: invalid value", cmd, client);
									return true;
								}
								plr.teleportDestination = null;
								plr.targetPos = new Vector3(Double.parseDouble(args.get(0)),
										Double.parseDouble(args.get(1)), Double.parseDouble(args.get(2)));
								plr.targetRot = plr.lastRot;
								plr.teleportToRoom(plr.levelID, plr.levelType, 0, plr.room, "");
								systemMessage("Teleported " + plr.account.getDisplayName() + " to " + plr.targetPos.x
										+ " " + plr.targetPos.y + " " + plr.targetPos.z, cmd, client);
							} catch (Exception e) {
								e.printStackTrace();
								systemMessage("Error: " + e, cmd, client);
							}
						} else {
							// Player
							if (args.size() < 1) {
								systemMessage("Missing argument: target player or XYZ coordinates", cmd, client);
								return true;
							}
							String target = args.get(0);
							uuid = AccountManager.getInstance().getUserByDisplayName(target);
							if (uuid == null) {
								// Player not found
								systemMessage("Specified target account could not be located.", cmd, client);
								return true;
							}
							acc = AccountManager.getInstance().getAccount(uuid);
							Player plrTarget = acc.getOnlinePlayerInstance();
							if (plrTarget == null || !plrTarget.roomReady) {
								// Player not found
								systemMessage("Specified target player is not online or not fully in world yet.", cmd,
										client);
								return true;
							}

							// Handle teleport
							plr.teleportDestination = plrTarget.account.getAccountID();
							plr.targetPos = plrTarget.lastPos;
							plr.targetRot = plrTarget.lastRot;
							plr.teleportToRoom(plrTarget.levelID, plrTarget.levelType, 0, plrTarget.room, "");
							systemMessage("Teleported " + plr.account.getDisplayName() + " to "
									+ plrTarget.account.getDisplayName(), cmd, client);
						}
						return true;
					}

					case "tpserverto": {
						// Teleport tool

						// Player
						if (args.size() < 1) {
							systemMessage("Missing argument: target player", cmd, client);
							return true;
						}
						String target = args.get(0);
						String uuid = AccountManager.getInstance().getUserByDisplayName(target);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified target account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
						Player plrTarget = acc.getOnlinePlayerInstance();
						if (plrTarget == null || !plrTarget.roomReady) {
							// Player not found
							systemMessage("Specified target player is not online or not fully in world yet.", cmd,
									client);
							return true;
						}

						// Find players in room
						for (Player plr : Centuria.gameServer.getPlayers()) {
							// Check ready
							if (!plr.roomReady || plr.account.getAccountID().equals(plrTarget.account.getAccountID()))
								continue;

							// Handle teleport
							plr.teleportDestination = plrTarget.account.getAccountID();
							plr.targetPos = plrTarget.lastPos;
							plr.targetRot = plrTarget.lastRot;
							plr.teleportToRoom(plrTarget.levelID, plrTarget.levelType, 0, plrTarget.room, "");
							systemMessage("Teleported " + plr.account.getDisplayName() + " to "
									+ plrTarget.account.getDisplayName(), cmd, client);
						}

						// Done
						systemMessage("Bulk-teleport completed!", cmd, client);
						return true;
					}

					case "tpall": {
						// Teleport tool

						// Find players in room
						for (Player plr : Centuria.gameServer.getPlayers()) {
							// Check ready
							if (!plr.roomReady)
								continue;

							// Determine mode
							if (args.size() >= 3) {
								try {
									// Get current
									Player plrS = client.getPlayer().getOnlinePlayerInstance();
									if (plrS == null || !plrS.roomReady) {
										// Player not found
										systemMessage("Your player is not online or not fully in world yet.", cmd,
												client);
										return true;
									}

									// Verify room
									if (!plr.room.equals(plrS.room))
										continue;

									// Coordinates
									if (!args.get(0).matches("^[\\-0-9\\.]+$")) {
										systemMessage("Invalid argument: X: invalid value", cmd, client);
										return true;
									}
									if (!args.get(1).matches("^[\\-0-9\\.]+$")) {
										systemMessage("Invalid argument: Y: invalid value", cmd, client);
										return true;
									}
									if (!args.get(2).matches("^[\\-0-9\\.]+$")) {
										systemMessage("Invalid argument: Z: invalid value", cmd, client);
										return true;
									}
									plr.teleportDestination = null;
									plr.targetPos = new Vector3(Double.parseDouble(args.get(0)),
											Double.parseDouble(args.get(1)), Double.parseDouble(args.get(2)));
									plr.targetRot = plr.lastRot;
									plr.teleportToRoom(plr.levelID, plr.levelType, 0, plr.room, "");
									systemMessage("Teleported " + plr.account.getDisplayName() + " to "
											+ plr.targetPos.x + " " + plr.targetPos.y + " " + plr.targetPos.z, cmd,
											client);
								} catch (Exception e) {
									e.printStackTrace();
									systemMessage("Error: " + e, cmd, client);
								}
							} else {
								// Player
								if (args.size() < 1) {
									systemMessage("Missing argument: target player or XYZ coordinates", cmd, client);
									return true;
								}
								String target = args.get(0);
								String uuid = AccountManager.getInstance().getUserByDisplayName(target);
								if (uuid == null) {
									// Player not found
									systemMessage("Specified target account could not be located.", cmd, client);
									return true;
								}
								CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
								Player plrTarget = acc.getOnlinePlayerInstance();
								if (plrTarget == null || !plrTarget.roomReady) {
									// Player not found
									systemMessage("Specified target player is not online or not fully in world yet.",
											cmd, client);
									return true;
								}

								// Verify room
								if (plr.account.getAccountID().equals(plrTarget.account.getAccountID())
										|| !plr.room.equals(plrTarget.room))
									continue;

								// Handle teleport
								plr.teleportDestination = plrTarget.account.getAccountID();
								plr.targetPos = plrTarget.lastPos;
								plr.targetRot = plrTarget.lastRot;
								plr.teleportToRoom(plrTarget.levelID, plrTarget.levelType, 0, plrTarget.room, "");
								systemMessage("Teleported " + plr.account.getDisplayName() + " to "
										+ plrTarget.account.getDisplayName(), cmd, client);
							}
						}

						// Done
						systemMessage("Bulk-teleport completed!", cmd, client);
						return true;
					}

					case "gatherallplayers": {
						// Skip if enabled
						if (GatheringRoomProvider.enabled) {
							systemMessage(
									"The gathering has already been started, you can disable it with 'endgathering'",
									cmd, client);
							return true;
						}

						// Enable
						GatheringRoomProvider.enabled = true;

						// Teleport all players
						for (Player plr : Centuria.gameServer.getPlayers()) {
							// Skip rooms that arent compatible
							if (plr.getRoom() == null && plr.room != null)
								continue;

							// Teleport if ready
							if (plr.roomReady || (plr.room != null && plr.pendingRoom != null
									&& plr.pendingRoom.equals(plr.room))) {
								// Teleport to room
								GameRoom room = ((GameServer) plr.client.getServer()).getRoomManager()
										.findBestRoom(Integer.valueOf(plr.levelID), plr);
								if (room.getObject(PrivateInstanceContainer.class) == null)
									plr.pendingPrivateMessage = "A server-wide gathering was started!\n\nAll players have been placed in a gathering room which overrides all other rooms, we apologize for the inconvenience if progress was lost!";
								String roomID = room.getID();
								plr.targetPos = new Vector3(plr.lastPos.x, plr.lastPos.y, plr.lastPos.z);
								plr.targetRot = new Quaternion(plr.lastRot.x, plr.lastRot.y, plr.lastRot.z,
										plr.lastRot.w);
								plr.teleportToRoom(plr.levelID, plr.levelType, 0, roomID, "");
							} else if (plr.room == null || (plr.room != null && plr.pendingRoom != null
									&& !plr.pendingRoom.equals(plr.room))) {
								// Update pending room
								String pending = plr.pendingRoom;
								String newRoom = ((GameServer) plr.client.getServer()).getRoomManager()
										.findBestRoom(Integer.valueOf(plr.levelID), plr).getID();
								plr.pendingRoom = newRoom;

								// Update chat client just in case
								ChatClient chClient = Centuria.chatServer.getClient(plr.account.getAccountID());
								if (chClient != null) {
									// Leave old room
									if (chClient.isInRoom(pending))
										chClient.leaveRoom(pending);

									// Join room
									if (!chClient.isInRoom(newRoom))
										chClient.joinRoom(newRoom, ChatRoomTypes.ROOM_CHAT);
								}
							}
						}

						// Done
						systemMessage("Bulk-teleport completed!", cmd, client);
						return true;
					}

					case "endgathering": {
						// Skip if enabled
						if (!GatheringRoomProvider.enabled) {
							systemMessage("There is no gathering at present time", cmd, client);
							return true;
						}

						// Disable
						GatheringRoomProvider.enabled = false;

						// Teleport all players
						for (Player plr : Centuria.gameServer.getPlayers()) {
							// Skip rooms that arent compatible
							if (plr.getRoom() == null && plr.room != null)
								continue;

							// Get current room
							GameRoom cRoom = plr.getRoom();
							if (cRoom != null && cRoom.getObject(PrivateInstanceContainer.class) != null)
								continue; // Skip those in private instances

							// Teleport if ready
							if (plr.roomReady || (plr.room != null && plr.pendingRoom != null
									&& plr.pendingRoom.equals(plr.room))) {
								// Teleport to room
								plr.pendingPrivateMessage = "Apologies for the interruption, the server gathering has ended and the server needed to teleport everyone back to rooms to function correctly.\n\nWe apologize for the inconvenience!";
								String roomID = ((GameServer) plr.client.getServer()).getRoomManager()
										.findBestRoom(Integer.valueOf(plr.levelID), plr).getID();
								plr.targetPos = new Vector3(plr.lastPos.x, plr.lastPos.y, plr.lastPos.z);
								plr.targetRot = new Quaternion(plr.lastRot.x, plr.lastRot.y, plr.lastRot.z,
										plr.lastRot.w);
								plr.teleportToRoom(plr.levelID, plr.levelType, 0, roomID, "");
							} else if (plr.room == null || (plr.room != null && plr.pendingRoom != null
									&& !plr.pendingRoom.equals(plr.room))) {
								// Update pending room
								String pending = plr.pendingRoom;
								String newRoom = ((GameServer) plr.client.getServer()).getRoomManager()
										.findBestRoom(Integer.valueOf(plr.levelID), plr).getID();
								plr.pendingRoom = newRoom;

								// Update chat client just in case
								ChatClient chClient = Centuria.chatServer.getClient(plr.account.getAccountID());
								if (chClient != null) {
									// Leave old room
									if (chClient.isInRoom(pending))
										chClient.leaveRoom(pending);

									// Join room
									if (!chClient.isInRoom(newRoom))
										chClient.joinRoom(newRoom, ChatRoomTypes.ROOM_CHAT);
								}
							}
						}

						// Done
						systemMessage("Bulk-teleport completed!", cmd, client);
						return true;
					}

					case "tpm": {
						try {
							// Teleports a player to a map.
							String defID = "";
							if (args.size() < 1) {
								systemMessage("Missing argument: map ID", cmd, client);
								return true;
							}

							// Parse arguments
							defID = args.get(0);

							// Check room
							String room = null;
							if (args.size() >= 2)
								room = args.get(1);

							// Check type
							String type = "0";
							if (args.size() >= 3)
								type = args.get(2);

							// Find player
							String player = client.getPlayer().getDisplayName();
							if (args.size() >= 4)
								player = args.get(3);
							String uuid = AccountManager.getInstance().getUserByDisplayName(player);
							if (uuid == null) {
								// Player not found
								systemMessage("Specified account could not be located.", cmd, client);
								return true;
							}
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

							// Teleport
							Player plr = acc.getOnlinePlayerInstance();
							if (plr != null) {
								// Find room
								String roomID;
								if (room == null)
									roomID = ((GameServer) plr.client.getServer()).getRoomManager()
											.findBestRoom(Integer.valueOf(defID), plr).getID();
								else
									roomID = ((GameServer) plr.client.getServer()).getRoomManager()
											.getOrCreateRoom(Integer.valueOf(defID), room).getID();

								// Teleport
								plr.teleportToRoom(Integer.valueOf(defID), Integer.valueOf(type), -1, roomID, "");
							} else {
								// Player not found
								systemMessage("Specified player is not online.", cmd, client);
								return true;
							}
						} catch (Exception e) {
							e.printStackTrace();
							systemMessage("Error: " + e, cmd, client);
						}

						return true;
					}
					case "xpinfo": {
						// XP info
						// Parse arguments
						String player = client.getPlayer().getDisplayName();
						if (args.size() > 0) {
							player = args.get(0);
						}
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Display info
						try {
							systemMessage("XP details:\n" + "Level: " + acc.getLevel().getLevel() + "\nXP: "
									+ acc.getLevel().getCurrentXP() + " / " + acc.getLevel().getLevelupXPCount()
									+ "\nTotal XP: " + acc.getLevel().getTotalXP(), cmd, client);
						} catch (Exception e) {
							systemMessage("Error: " + e, cmd, client);
						}

						return true;
					}
					case "takexp": {
						// Take XP
						// Parse arguments
						String player = client.getPlayer().getDisplayName();
						if (args.size() < 1) {
							systemMessage("Missing argument: xp amount", cmd, client);
							return true;
						}
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

						// Take xp
						try {
							int xp = Integer.parseInt(args.get(0));
							if (xp < 0) {
								systemMessage("Invalid XP amount: " + xp, cmd, client);
								return true;
							}
							acc.getLevel().removeXP(xp);
							systemMessage("Removed " + xp + " XP from " + acc.getDisplayName() + ".", cmd, client);
						} catch (Exception e) {
							systemMessage("Error: " + e, cmd, client);
						}

						return true;
					}
					case "resetxp": {
						// Reset XP
						// Parse arguments
						String player = client.getPlayer().getDisplayName();
						if (args.size() > 0) {
							player = args.get(0);
						}
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Take xp
						try {
							acc.getLevel().resetLevelXP();
							systemMessage("Resetted level XP of " + acc.getDisplayName() + ".", cmd, client);
						} catch (Exception e) {
							systemMessage("Error: " + e, cmd, client);
						}

						return true;
					}
					case "takelevels": {
						// Take levels
						// Parse arguments
						String player = client.getPlayer().getDisplayName();
						if (args.size() < 1) {
							systemMessage("Missing argument: levels to remove", cmd, client);
							return true;
						}
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

						// Take xp
						try {
							int levels = Integer.parseInt(args.get(0));
							if (acc.getLevel().getLevel() <= levels) {
								systemMessage("Invalid amount of levels to remove: " + levels + " (user is at "
										+ acc.getLevel().getLevel() + ")", cmd, client);
								return true;
							}
							acc.getLevel().setLevel(acc.getLevel().getLevel() - levels);
							systemMessage("Removed " + levels + " levels from " + acc.getDisplayName() + ".", cmd,
									client);
							acc.kickDirect(uuid, "Levels were changed, relog required.");
						} catch (Exception e) {
							systemMessage("Error: " + e, cmd, client);
						}

						return true;
					}
					case "addxp": {
						// Add XP
						// Parse arguments
						if (GameServer.hasPerm(permLevel, "admin")) {
							String player = client.getPlayer().getDisplayName();
							if (args.size() < 1) {
								systemMessage("Missing argument: xp amount", cmd, client);
								return true;
							}
							if (args.size() > 1) {
								player = args.get(1);
							}
							if (!player.equals("*")) {
								String uuid = AccountManager.getInstance().getUserByDisplayName(player);
								if (uuid == null) {
									// Player not found
									systemMessage("Specified account could not be located.", cmd, client);
									return true;
								}
								CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

								// Add xp
								try {
									int xp = Integer.parseInt(args.get(0));
									if (xp < 0) {
										systemMessage("Invalid XP amount: " + xp, cmd, client);
										return true;
									}
									acc.getLevel().addXP(xp);
									systemMessage("Given " + xp + " XP to " + acc.getDisplayName() + ".", cmd, client);
								} catch (Exception e) {
									systemMessage("Error: " + e, cmd, client);
									e.printStackTrace();
								}
							} else {
								final String cmdF = cmd;
								AccountManager.getInstance().runForAllAccounts(acc -> {
									// Add xp
									try {
										int xp = Integer.parseInt(args.get(0));
										if (xp < 0) {
											systemMessage("Invalid XP amount: " + xp, cmdF, client);
										}
										acc.getLevel().addXP(xp);
										systemMessage("Given " + xp + " XP to " + acc.getDisplayName() + ".", cmdF,
												client);
									} catch (Exception e) {
										systemMessage("Error: " + e, cmdF, client);
										e.printStackTrace();
									}
								});
								return true;
							}

							return true;
						}
					}
					case "addlevels": {
						// Add levels
						// Parse arguments
						if (GameServer.hasPerm(permLevel, "admin")) {
							String player = client.getPlayer().getDisplayName();
							if (args.size() < 1) {
								systemMessage("Missing argument: levels to add", cmd, client);
								return true;
							}
							if (args.size() > 1) {
								player = args.get(1);
							}
							if (!player.equals("*")) {
								String uuid = AccountManager.getInstance().getUserByDisplayName(player);
								if (uuid == null) {
									// Player not found
									systemMessage("Specified account could not be located.", cmd, client);
									return true;
								}
								CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

								// Add levels
								try {
									int levels = Integer.parseInt(args.get(0));
									if (levels < 0) {
										systemMessage("Invalid XP amount: " + levels, cmd, client);
										return true;
									}
									acc.getLevel().addLevel(levels);
									systemMessage("Given " + levels + " levels to " + acc.getDisplayName() + ".", cmd,
											client);
								} catch (Exception e) {
									systemMessage("Error: " + e, cmd, client);
								}
							} else {
								final String cmdF = cmd;
								AccountManager.getInstance().runForAllAccounts(acc -> {
									// Add levels
									try {
										int levels = Integer.parseInt(args.get(0));
										if (levels < 0) {
											systemMessage("Invalid XP amount: " + levels, cmdF, client);
										}
										acc.getLevel().addLevel(levels);
										systemMessage("Given " + levels + " levels to " + acc.getDisplayName() + ".",
												cmdF, client);
									} catch (Exception e) {
										systemMessage("Error: " + e, cmdF, client);
									}
								});
								return true;
							}

							return true;
						}
					}
					case "resetalllevels": {
						// Reset all levels
						// Parse arguments
						if (GameServer.hasPerm(permLevel, "admin")) {
							if (args.size() < 1 || !args.get(0).equals("confirm")) {
								systemMessage(
										"This command will wipe all xp of all players, are you sure you want to continue?\nAdd 'confirm' to the command to confirm your action.",
										cmd, client);
								return true;
							}

							final String cmdF = cmd;
							AccountManager.getInstance().runForAllAccounts(acc -> {
								// Reset level
								try {
									acc.getLevel().resetLevelXP();
									systemMessage("Resetted level XP of " + acc.getDisplayName() + ".", cmdF, client);
								} catch (Exception e) {
									systemMessage("Error: " + e, cmdF, client);
								}
							});

							return true;
						}
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
								if (acc.getOnlinePlayerInstance() == null) {
									systemMessage("Error: player not online", cmd, client);
									return true;
								}
								acc.getOnlinePlayerInstance().client.sendPacket(packet);
								systemMessage("Packet has been sent.", cmd, client);
							} catch (Exception e) {
								systemMessage("Error: " + e, cmd, client);
							}

							return true;
						}
					}
					case "questskip": {
						// Skips quests
						int count = 1;
						if (args.size() > 0)
							try {
								count = Integer.parseInt(args.get(0));
							} catch (Exception e) {
								return true;
							}

						// Parse arguments
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
							if (acc.getOnlinePlayerInstance() == null) {
								systemMessage("Error: player not online", cmd, client);
								return true;
							}
							if (QuestManager.getActiveQuest(acc) == null) {
								systemMessage("Error: no further quests", cmd, client);
								return true;
							}
							int c = 0;
							for (int i = 0; i < count; i++) {
								c++;
								if (!QuestManager.finishQuest(acc.getOnlinePlayerInstance(),
										Integer.parseInt(QuestManager.getActiveQuest(acc))))
									break;
							}
							systemMessage(
									"Skipped " + c + " quests, now at: "
											+ QuestManager
													.getQuest(QuestManager.getActiveQuest(client.getPlayer())).name,
									cmd, client);
						} catch (Exception e) {
							systemMessage("Error: " + e, cmd, client);
						}

						return true;
					}
					case "takeitem": {
						try {
							int defID = 0;
							int quantity = 1;
							String player = "";
							String uuid = client.getPlayer().getAccountID();

							if (args.size() < 1) {
								systemMessage("Missing argument: itemDefId", cmd, client);
								return true;
							}

							defID = Integer.valueOf(args.get(0));
							if (args.size() >= 2) {
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
								systemMessage("You cannot remove 0 or less quantity of/or an item ID of 0 or below.",
										cmd, client);
								return true;
							}

							// find account
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

							// give item to the command sender..
							var onlinePlayer = acc.getOnlinePlayerInstance();
							var result = acc.getSaveSpecificInventory().getItemAccessor(onlinePlayer).remove(defID,
									quantity);

							if (result)
								systemMessage(
										"Removed " + acc.getDisplayName() + " " + quantity + " of item " + defID
												+ ", remaining: " + acc.getSaveSpecificInventory()
														.getItemAccessor(onlinePlayer).getCountOfItem(defID),
										cmd, client);
							else
								systemMessage("Failed to remove item.", cmd, client);
							return true;
						} catch (Exception e) {
							systemMessage("Error: " + e, cmd, client);
							return true;
						}
					}
					case "giveitem":
						if (GameServer.hasPerm(permLevel, "admin")
								|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars
								|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes
								|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
								|| client.getPlayer().getSaveSpecificInventory()
										.getSaveSettings().allowGiveItemFurnitureItems
								|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemMods
								|| client.getPlayer().getSaveSpecificInventory()
										.getSaveSettings().allowGiveItemResources
								|| client.getPlayer().getSaveSpecificInventory()
										.getSaveSettings().allowGiveItemSanctuaryTypes) {
							try {
								int defID = 0;
								int quantity = 1;
								String player = "";
								String uuid = client.getPlayer().getAccountID();

								if (args.size() < 1) {
									systemMessage("Missing argument: itemDefId", cmd, client);
									return true;
								}

								defID = Integer.valueOf(args.get(0));
								if (args.size() >= 2) {
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

								// find account
								CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

								// give item to the command sender..
								var onlinePlayer = acc.getOnlinePlayerInstance();
								var result = acc.getSaveSpecificInventory().getItemAccessor(onlinePlayer).add(defID,
										quantity);

								if (result.length > 0)
									systemMessage(
											"Gave " + acc.getDisplayName() + " " + quantity + " of item " + defID + ".",
											cmd, client);
								else
									systemMessage("Failed to add item.", cmd, client);
								return true;
							} catch (Exception e) {
								systemMessage("Error: " + e, cmd, client);
								return true;
							}
						}
						break;
					}
				}

				//
				// Remove filtered items command
				if (cmd.equals("removeallfiltereditems")) {
					// Check mode and permissions
					if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
							|| client.getPlayer().getSaveSpecificInventory()
									.getSaveSettings().allowGiveItemFurnitureItems
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemMods
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
							|| client.getPlayer().getSaveSpecificInventory()
									.getSaveSettings().allowGiveItemSanctuaryTypes
							|| GameServer.hasPerm(permLevel, "moderator")) {
						// Run command
						try {
							// Parse arguments if any and check perms
							String player = client.getPlayer().getDisplayName();
							if (args.size() > 1 && GameServer.hasPerm(permLevel, "moderator")) {
								player = args.get(1);
							}

							// Find ID
							String uuid = AccountManager.getInstance().getUserByDisplayName(player);
							if (uuid == null) {
								// Player not found
								systemMessage("Specified account could not be located.", cmd, client);
								return true;
							}

							// Find account
							CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
							if (acc == null) {
								// Player not found
								systemMessage("Specified account could not be located.", cmd, client);
								return true;
							}

							// Load filter
							InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
									.getResourceAsStream("creativeitemfilter.json");
							JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
									.getAsJsonObject().get("Items").getAsJsonObject();
							strm.close();

							// Remove items
							int removedItems = 0;
							for (String id : helper.keySet()) {
								// Find how many exist in the inventory
								ItemAccessor accessor = acc.getSaveSpecificInventory()
										.getItemAccessor(acc.getOnlinePlayerInstance());
								int defID = Integer.parseInt(id);
								int currentCount = accessor.getCountOfItem(defID);

								if (currentCount > 0) {
									// Remove items
									accessor.remove(defID, currentCount);
									removedItems += currentCount;
								}
							}

							// Show result
							systemMessage("Removed " + removedItems + " item" + (removedItems == 1 ? "" : "s")
									+ " from the inventory.\n\nNote: a relog may be required for this to take effect.",
									cmd, client);

							return true;
						} catch (Exception e) {
							systemMessage("Error: " + e, cmd, client);
							return true;
						}
					}
				}

				//
				// User giveitem command
				if (cmd.equals("giveitem")) {
					try {
						int defID = 0;
						int quantity = 1;
						String uuid = client.getPlayer().getAccountID();

						if (args.size() < 1) {
							systemMessage("Missing argument: itemDefId", cmd, client);
							return true;
						}

						defID = Integer.valueOf(args.get(0));
						if (args.size() >= 2) {
							quantity = Integer.valueOf(args.get(1));
						}

						// funny stuff check
						if (quantity <= 0 || defID <= 0) {
							systemMessage("You cannot give 0 or less quantity of/or an item ID of 0 or below.", cmd,
									client);
							return true;
						}

						// check max item limit (hardcoded 100 for creative mode)
						int current = client.getPlayer().getSaveSpecificInventory().getItemAccessor(null)
								.getCountOfItem(defID);
						if (!client.getPlayer().getSaveSpecificInventory().getItemAccessor(null).isQuantityBased(defID)
								&& quantity + current > 100) {
							systemMessage("You cannot have more than 100 of that item via commands.", cmd, client);
							return true;
						}

						// check item
						if (ItemAccessor.getInventoryTypeOf(defID) == null
								|| (!ItemAccessor.getInventoryTypeOf(defID).equals("100")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("104")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("111")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("103")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("102")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("10")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("2")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("1"))) {
							systemMessage("Invalid item defID. Please make sure you can actually obtain this item.",
									cmd, client);
							return true;
						}

						// check perms and item type
						if ((ItemAccessor.getInventoryTypeOf(defID).equals("100") && !client.getPlayer()
								.getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("104") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("111") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("103") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemResources)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("102") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemFurnitureItems)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("10") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemSanctuaryTypes)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("2") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemMods)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("1") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars)) {
							systemMessage("Invalid item defID. Please make sure you can actually obtain this item.",
									cmd, client);
							return true;
						}

						// find account
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						// give item to the command sender..
						var onlinePlayer = acc.getOnlinePlayerInstance();
						var result = acc.getSaveSpecificInventory().getItemAccessor(onlinePlayer).add(defID, quantity);

						if (result.length > 0)
							systemMessage("Gave " + acc.getDisplayName() + " " + quantity + " of item " + defID + ".",
									cmd, client);
						else
							systemMessage("Failed to add item.", cmd, client);
						return true;
					} catch (Exception e) {
						systemMessage("Error: " + e, cmd, client);
						return true;
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
					message += "\nYou do not need the symbols on the command itself, its only for informational purposes. Secondly, if there are quotes (\"\") around an argument, make sure to actually include them in the command.";
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
		res.addProperty("conversationType", client.getRoom(room).getType());
		res.addProperty("conversationId", room);
		res.addProperty("message", "Issued chat command: " + cmd + ":\n[system] " + message);
		res.addProperty("source", client.getPlayer().getAccountID());// Time format
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		res.addProperty("sentAt", fmt.format(new Date()));
		res.addProperty("eventId", "chat.postMessage");
		res.addProperty("success", true);
		client.sendPacket(res);

		// Log
		Centuria.logger.info(client.getPlayer().getDisplayName() + " executed chat command: " + cmd + ": " + message);
	}

}