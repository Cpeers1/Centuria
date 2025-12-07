package org.asf.centuria.networking.chatserver.networking;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.voicechatserver.VoiceChatClient;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.packets.xt.gameserver.room.RoomJoinPacket;
import org.asf.centuria.social.SocialManager;
import org.asf.connective.tasks.AsyncTaskManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SendMessage extends AbstractChatPacket {

	private static String[] nameBlacklist = new String[] { "kit", "kitsendragn", "kitsendragon", "fera", "fero",
			"wwadmin", "ayli", "komodorihero", "wwsam", "blinky", "fer.ocity" };

	private static String NIL_UUID = new UUID(0, 0).toString();
	private static ArrayList<String> muteWords = new ArrayList<String>();
	private static ArrayList<String> filterWords = new ArrayList<String>();
	private static ArrayList<String> alwaysfilterWords = new ArrayList<String>();
	private static ArrayList<String> flagWords = new ArrayList<String>();

	public static ArrayList<String> clearanceCodes = new ArrayList<String>();
	private static Random rnd = new Random();

	public static String[] getInvalidWords() {
		ArrayList<String> fullList = new ArrayList<String>();
		fullList.addAll(muteWords);
		fullList.addAll(filterWords);
		fullList.addAll(alwaysfilterWords);
		return fullList.toArray(t -> new String[t]);
	}

	static {
		reloadFilter();
	}

	private static void reloadFilter() {
		muteWords.clear();
		filterWords.clear();
		alwaysfilterWords.clear();
		flagWords.clear();

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
					data = data.replace("  ", " ");

				for (String word : data.split(";"))
					if (!word.isEmpty())
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
					data = data.replace("  ", " ");

				for (String word : data.split(";"))
					if (!word.isEmpty())
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
					data = data.replace("  ", " ");

				for (String word : data.split(";"))
					if (!word.isEmpty())
						alwaysfilterWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Load flagged words
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/flagwords.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", " ");

				for (String word : data.split(";"))
					if (!word.isEmpty())
						flagWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Load local filters
		if (!new File("textfilter").exists()) {
			new File("textfilter").mkdirs();
			try {
				Files.writeString(Path.of("textfilter/filter.txt"), "");
				Files.writeString(Path.of("textfilter/alwaysfilter.txt"), "");
				Files.writeString(Path.of("textfilter/instamute.txt"), "");
				Files.writeString(Path.of("textfilter/flagwords.txt"), "");
			} catch (IOException e) {
			}
		}
		try {
			filterLastChange = Files.getLastModifiedTime(Path.of("textfilter/filter.txt")).toMillis();
			alwaysFilterLastChange = Files.getLastModifiedTime(Path.of("textfilter/alwaysfilter.txt")).toMillis();
			instaMuteLastChange = Files.getLastModifiedTime(Path.of("textfilter/instamute.txt")).toMillis();
			if (new File("textfilter/flagwords.txt").exists())
				flagWordsLastChange = Files.getLastModifiedTime(Path.of("textfilter/flagwords.txt")).toMillis();

			// Load filter
			try {
				InputStream strm = new FileInputStream("textfilter/filter.txt");
				String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
				for (String line : lines.split("\n")) {
					if (line.isEmpty() || line.startsWith("#"))
						continue;

					String data = line.trim();
					while (data.contains("  "))
						data = data.replace("  ", " ");

					for (String word : data.split(";"))
						if (!word.isEmpty())
							filterWords.add(word.toLowerCase());
				}
				strm.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Load ban words
			try {
				InputStream strm = new FileInputStream("textfilter/instamute.txt");
				String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
				for (String line : lines.split("\n")) {
					if (line.isEmpty() || line.startsWith("#"))
						continue;

					String data = line.trim();
					while (data.contains("  "))
						data = data.replace("  ", " ");

					for (String word : data.split(";"))
						if (!word.isEmpty())
							muteWords.add(word.toLowerCase());
				}
				strm.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Load always filtered words
			try {
				InputStream strm = new FileInputStream("textfilter/alwaysfilter.txt");
				String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
				for (String line : lines.split("\n")) {
					if (line.isEmpty() || line.startsWith("#"))
						continue;

					String data = line.trim();
					while (data.contains("  "))
						data = data.replace("  ", " ");

					for (String word : data.split(";"))
						if (!word.isEmpty())
							alwaysfilterWords.add(word.toLowerCase());
				}
				strm.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Load flagged words
			try {
				if (new File("textfilter/flagwords.txt").exists()) {
					InputStream strm = new FileInputStream("textfilter/flagwords.txt");
					String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
					for (String line : lines.split("\n")) {
						if (line.isEmpty() || line.startsWith("#"))
							continue;

						String data = line.trim();
						while (data.contains("  "))
							data = data.replace("  ", " ");

						for (String word : data.split(";"))
							if (!word.isEmpty())
								flagWords.add(word.toLowerCase());
					}
					strm.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
		}
	}

	private static long filterLastChange;
	private static long alwaysFilterLastChange;
	private static long instaMuteLastChange;
	private static long flagWordsLastChange;

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
			if (!client.isRoomPrivate(room) || !manager.dmExists(room)) {
				// Check if sanctuary
				if (room.startsWith("sanctuary_")) {
					if (!gameClient.room.equals(room)) {
						// Invalid
						return true;
					}
				} else {
					// Check level
					if (!room.equalsIgnoreCase("room_" + gameClient.levelID)) {
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
		if (message.startsWith(">")) {
			String cmd = message.substring(1).trim();
			if (handleCommand(cmd, client))
				return true;
		}

		// Log
		if (!client.isRoomPrivate(room))
			Centuria.logger.info("Chat: " + client.getPlayer().getDisplayName() + ": " + message + " ["
					+ formatRoomName(client, room) + "]");

		// Check times of the filter update
		try {
			long filterLastChange = Files.getLastModifiedTime(Path.of("textfilter/filter.txt")).toMillis();
			long alwaysFilterLastChange = Files.getLastModifiedTime(Path.of("textfilter/alwaysfilter.txt")).toMillis();
			long instaMuteLastChange = Files.getLastModifiedTime(Path.of("textfilter/instamute.txt")).toMillis();
			long flagWordsLastChange = (new File("textfilter/flagwords.txt").exists()
					? Files.getLastModifiedTime(Path.of("textfilter/flagwords.txt")).toMillis()
					: 0);
			if (SendMessage.filterLastChange != filterLastChange
					|| SendMessage.alwaysFilterLastChange != alwaysFilterLastChange
					|| SendMessage.instaMuteLastChange != instaMuteLastChange
					|| SendMessage.flagWordsLastChange != flagWordsLastChange) {
				// Reload
				Centuria.logger.info("Updating chat filter...");
				reloadFilter();
			}
		} catch (IOException e) {
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
		if (acc.getSaveSharedInventory().containsItem("penalty") && acc.getSaveSharedInventory().getItem("penalty")
				.getAsJsonObject().get("type").getAsString().equals("mute")) {
			JsonObject muteInfo = acc.getSaveSharedInventory().getItem("penalty").getAsJsonObject();
			if (muteInfo.get("unmuteTimestamp").getAsLong() == -1
					|| muteInfo.get("unmuteTimestamp").getAsLong() > System.currentTimeMillis()) {
				// Time format
				SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

				// Get reason
				String reason = null;
				if (muteInfo.has("reason"))
					reason = muteInfo.get("reason").getAsString();

				// Send failure
				JsonObject res = new JsonObject();
				res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
				res.addProperty("conversationId", room);
				res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>" + message
						+ "</noparse></color><noparse>");
				res.addProperty("messagePlain", "[!] " + message);
				if (GameServer.hasPerm(permLevel, "moderator"))
					res.addProperty("originalMessage", message); // Only for mods
//				res.add("messageParts, new JsonArray())); // Not present, so not sent
				res.addProperty("alertingMessage", true); // This is a moderator alerting message
				res.addProperty("criticalAlertingMessage", true); // Critical, should be red
				res.addProperty("blockedMessage", true); // The message was blocked, should be red highlighting
				res.addProperty("source", client.getPlayer().getAccountID());
				res.addProperty("sentAt", fmt.format(new Date()));
				res.addProperty("eventId", "chat.postMessage");
				res.addProperty("success", true);
				client.sendPacket(res);

				// Broadcast to moderators unless its a private chat
				if (!client.isRoomPrivate(room)) {
					for (ChatClient receiver : client.getServer().getClients()) {
						// Fetch receiver moderator perms
						String permLevel2 = "member";
						if (receiver.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
							permLevel2 = receiver.getPlayer().getSaveSharedInventory().getItem("permissions")
									.getAsJsonObject().get("permissionLevel").getAsString();
						}

						// Check if in room
						if (receiver.isInRoom(room) && GameServer.hasPerm(permLevel2, "moderator")
								&& !receiver.getPlayer().getAccountID().equals(client.getPlayer().getAccountID())) {
							// Check limbo player
							Player gameClient = receiver.getPlayer().getOnlinePlayerInstance();
							if (gameClient != null && (!gameClient.roomReady || gameClient.room == null))
								continue;

							// Send to mod
							res = new JsonObject();
							res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
							res.addProperty("conversationId", room);
							res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
									+ message + "</noparse></color><noparse>");
							res.addProperty("messagePlain", "[!] " + message);
							res.addProperty("originalMessage", message);
//							res.add("messageParts, new JsonArray())); // Not present, so not sent
							res.addProperty("alertingMessage", true); // This is a moderator alerting message
							res.addProperty("criticalAlertingMessage", true); // Critical, should be red
							res.addProperty("blockedMessage", true); // The message was blocked, should be red
																		// highlighting
							res.addProperty("source", client.getPlayer().getAccountID());
							res.addProperty("sentAt", fmt.format(new Date()));
							res.addProperty("eventId", "chat.postMessage");
							res.addProperty("success", true);
							receiver.sendPacket(res);
						} else if (!receiver.isInRoom(room)
								&& !receiver.getPlayer().getAccountID().equals(client.getPlayer().getAccountID())) {
							// Not in room

							// Check moderator client
							if (receiver.getObject(ModeratorClient.class) != null) {
								// Send through centuria moderator protocol
								res = new JsonObject();
								res.addProperty("eventId", "centuria.moderatorclient.postedMessageInOtherRoom");
								res.addProperty("conversationType", "room");
								res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
										+ message + "</noparse></color><noparse>");
								res.addProperty("messagePlain", "[!] " + message);
								res.addProperty("originalMessage", message);
//								res.add("messageParts, new JsonArray())); // Not present, so not sent
								res.addProperty("alertingMessage", true); // This is a moderator alerting
																			// message
								res.addProperty("criticalAlertingMessage", true); // Critical, should be red
																					// exclamation
																					// mark
								res.addProperty("blockedMessage", true); // The message was blocked, should be
																			// red
																			// highlighting
								res.addProperty("source", client.getPlayer().getAccountID());
								res.addProperty("sentAt", fmt.format(new Date()));
								res.addProperty("success", true);

								// Send message
								receiver.sendPacket(res);
							}
						}
					}
				}

				// System message
				res = new JsonObject();
				res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
				res.addProperty("conversationId", room);
				res.addProperty("message", "You are muted and cannot send messages in chat."
						+ (reason != null ? "\nReason: " + reason : ""));
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
		for (String word : message.split(" ")) {
			if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				// Mod log

				// Apply filter and get result
				FilterResult filter = runFilter(true, true, message, muteWords, "red");

				// Check if private
				if (client.isRoomPrivate(room)) {
					// Private chat, need more details
					// And strip away the message
					EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.mute",
							"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
							Map.of("Private chat room", formatRoomName(client, room), "Matched word(s)",
									filter.matchedWordsString, "Primary reason for filtering",
									"Filtered for offensive behaviour, slurs and similar insults are not allowed.",
									"Room", room, "Resulting action", "muted"),
							"SYSTEM", client.getPlayer()));
				} else {
					EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.mute",
							"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
							Map.of("Chat message", message, "Matched word(s)", filter.matchedWordsString,
									"Primary reason for filtering",
									"Filtered for offensive behaviour, slurs and similar insults are not allowed.",
									"Room", formatRoomName(client, room), "Resulting action", "muted"),
							"SYSTEM", client.getPlayer()));
				}

				// Send failure
				SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				JsonObject res = new JsonObject();
				res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
				res.addProperty("conversationId", room);
				if (GameServer.hasPerm(permLevel, "moderator")) {
					// Only for mods
					res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
							+ filter.highlightedMessage + "</noparse></color><noparse>");
					res.addProperty("messagePlain", "[!] " + filter.highlightedMessagePlain);
					res.addProperty("originalMessage", message);
					res.add("messageParts", filter.messagePartsJson);
				} else {
					res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
							+ filter.filteredMessage + "</noparse></color><noparse>");
					res.addProperty("messagePlain", "[!] " + filter.filteredMessage);
				}
				res.addProperty("alertingMessage", true); // This is a moderator alerting message
				res.addProperty("criticalAlertingMessage", true); // Critical, should be red
				res.addProperty("blockedMessage", true); // The message was blocked, should be red highlighting
				res.addProperty("source", client.getPlayer().getAccountID());
				res.addProperty("sentAt", fmt.format(new Date()));
				res.addProperty("eventId", "chat.postMessage");
				res.addProperty("success", true);
				client.sendPacket(res);

				// Broadcast to moderators unless its a private chat
				if (!client.isRoomPrivate(room)) {
					for (ChatClient receiver : client.getServer().getClients()) {
						// Fetch receiver moderator perms
						String permLevel2 = "member";
						if (receiver.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
							permLevel2 = receiver.getPlayer().getSaveSharedInventory().getItem("permissions")
									.getAsJsonObject().get("permissionLevel").getAsString();
						}

						// Check if in room
						if (receiver.isInRoom(room) && GameServer.hasPerm(permLevel2, "moderator")
								&& !receiver.getPlayer().getAccountID().equals(client.getPlayer().getAccountID())) {
							// Check limbo player
							Player gameClient = receiver.getPlayer().getOnlinePlayerInstance();
							if (gameClient != null && (!gameClient.roomReady || gameClient.room == null))
								continue;

							// Send to mod
							res = new JsonObject();
							res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
							res.addProperty("conversationId", room);
							res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
									+ filter.highlightedMessage + "</noparse></color><noparse>");
							res.addProperty("messagePlain", "[!] " + filter.highlightedMessagePlain);
							res.addProperty("originalMessage", message);
							res.add("messageParts", filter.messagePartsJson);
							res.addProperty("alertingMessage", true); // This is a moderator alerting message
							res.addProperty("criticalAlertingMessage", true); // Critical, should be red
							res.addProperty("blockedMessage", true); // The message was blocked, should be red
																		// highlighting
							res.addProperty("source", client.getPlayer().getAccountID());
							res.addProperty("sentAt", fmt.format(new Date()));
							res.addProperty("eventId", "chat.postMessage");
							res.addProperty("success", true);
							receiver.sendPacket(res);
						} else if (!receiver.isInRoom(room)
								&& !receiver.getPlayer().getAccountID().equals(client.getPlayer().getAccountID())) {
							// Not in room

							// Check moderator client
							if (receiver.getObject(ModeratorClient.class) != null) {
								// Send through centuria moderator protocol
								res = new JsonObject();
								res.addProperty("eventId", "centuria.moderatorclient.postedMessageInOtherRoom");
								res.addProperty("conversationType", "room");
								res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
										+ filter.highlightedMessage + "</noparse></color><noparse>");
								res.addProperty("messagePlain", "[!] " + filter.highlightedMessagePlain);
								res.addProperty("originalMessage", message);
								res.add("messageParts", filter.messagePartsJson);
								res.addProperty("alertingMessage", true); // This is a moderator alerting
																			// message
								res.addProperty("criticalAlertingMessage", true); // Critical, should be red
																					// exclamation
																					// mark
								res.addProperty("blockedMessage", true); // The message was blocked, should be
																			// red
																			// highlighting
								res.addProperty("source", client.getPlayer().getAccountID());
								res.addProperty("sentAt", fmt.format(new Date()));
								res.addProperty("success", true);

								// Send message
								receiver.sendPacket(res);
							}
						}
					}
				}

				// Mute
				client.getPlayer().mute(0, 0, 30, "SYSTEM",
						"Filtered for offensive behaviour, slurs and similar insults are not allowed.");

				// Send system message
				res = new JsonObject();
				res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
				res.addProperty("conversationId", room);
				res.addProperty("message",
						"You have been automatically muted in public chat for violating server rules, mute will last 30 minutes.\nReason: Filtered for offensive behaviour, slurs and similar insults are not allowed.\nWe request you to keep your chat respectful, safe and clean!");
				res.addProperty("source", NIL_UUID);
				res.addProperty("sentAt", fmt.format(new Date()));
				res.addProperty("eventId", "chat.postMessage");
				res.addProperty("success", true);
				client.sendPacket(res);
				return true;
			}
		}

		// Fire event
		ChatMessageBroadcastEvent evt2 = new ChatMessageBroadcastEvent(client.getServer(), client.getPlayer(), client,
				message, room);
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
		if (client.isInRoom(room)) {
			// Build filterlists
			ArrayList<String> filterDefaultList = new ArrayList<String>();
			filterDefaultList.addAll(muteWords);
			filterDefaultList.addAll(alwaysfilterWords);
			ArrayList<String> filterStrictModeList = new ArrayList<String>();
			filterStrictModeList.addAll(filterDefaultList);
			filterStrictModeList.addAll(filterWords);
			ArrayList<String> filterFlagList = new ArrayList<String>();
			filterFlagList.addAll(filterWords);
			filterFlagList.addAll(flagWords);

			// Run filters
			FilterResult filterDefault = runFilter(true, true, message, filterDefaultList, "red"); // Default
			FilterResult filterStrictMode = runFilter(true, true, message, filterStrictModeList, "red"); // Strict-mode
			FilterResult filterFlagged = runFilter(true, true, message, filterFlagList, "orange"); // Words to flag to the team
			FilterResult filterFlaggedRaw = runFilter(true, true, message, flagWords, "orange"); // Words to flag to the team

			// Gather result
			boolean filteredUserStrictMode = filterStrictMode.wasFiltered;
			boolean filteredDefaultSeverity = filterDefault.wasFiltered;
			boolean filteredFlagged = filterFlagged.wasFiltered;
			boolean filteredFlaggedWithoutStrictmode = filterFlaggedRaw.wasFiltered;
			int filterSettingSelf = 0;
			UserVarValue valS = client.getPlayer().getSaveSpecificInventory().getUserVarAccesor()
					.getPlayerVarValue(9362, 0); // the setting for the filter ingame, if the user wishes to have a
													// stricter filter enabled.
			if (valS != null)
				filterSettingSelf = valS.value;

			// Check severity and if we need to mute
			if (filteredDefaultSeverity) {
				// Get/create memory
				ChatFilterMemory mem = client.getObject(ChatFilterMemory.class);
				if (mem == null) {
					mem = new ChatFilterMemory();
					client.addObject(mem);
				}

				// Update
				if (System.currentTimeMillis() - mem.lastFlag > (3 * 60 * 60 * 1000)) {
					mem.lastFlag = 0;
					mem.flagCount = 0;
				}
				mem.lastFlag = System.currentTimeMillis();
				mem.flagCount++;

				// Check count
				if (mem.flagCount >= 4) {
					// Mod log

					// Check if private
					if (client.isRoomPrivate(room)) {
						// Private chat, need more details
						// And strip away the message
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.mute",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Private chat room", formatRoomName(client, room), "Matched word(s)",
										filterDefault.matchedWordsString, "Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "muted",
										"Reason for mute", "Continued breaches of chat rules after 2 warnings."),
								"SYSTEM", client.getPlayer()));
					} else {
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.mute",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Chat message", message, "Matched word(s)", filterDefault.matchedWordsString,
										"Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "muted",
										"Reason for mute", "Continued breaches of chat rules after 2 warnings."),
								"SYSTEM", client.getPlayer()));
					}

					// Send failure
					SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					JsonObject res = new JsonObject();
					res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
					res.addProperty("conversationId", room);
					if (GameServer.hasPerm(permLevel, "moderator")) {
						// Only for mods
						res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
								+ filterDefault.highlightedMessage + "</noparse></color><noparse>");
						res.addProperty("messagePlain", "[!] " + filterDefault.highlightedMessagePlain);
						res.addProperty("originalMessage", message);
						res.add("messageParts", filterDefault.messagePartsJson);
					} else {
						res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
								+ filterDefault.filteredMessage + "</noparse></color><noparse>");
						res.addProperty("messagePlain", "[!] " + filterDefault.filteredMessage);
					}
					res.addProperty("alertingMessage", true); // This is a moderator alerting message
					res.addProperty("criticalAlertingMessage", true); // Critical, should be red
					res.addProperty("blockedMessage", true); // The message was blocked, should be red highlighting
					res.addProperty("source", client.getPlayer().getAccountID());
					res.addProperty("sentAt", fmt.format(new Date()));
					res.addProperty("eventId", "chat.postMessage");
					res.addProperty("success", true);
					client.sendPacket(res);

					// Broadcast to moderators unless its a private chat
					if (!client.isRoomPrivate(room)) {
						for (ChatClient receiver : client.getServer().getClients()) {
							// Fetch receiver moderator perms
							String permLevel2 = "member";
							if (receiver.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
								permLevel2 = receiver.getPlayer().getSaveSharedInventory().getItem("permissions")
										.getAsJsonObject().get("permissionLevel").getAsString();
							}

							// Check if in room
							if (receiver.isInRoom(room) && GameServer.hasPerm(permLevel2, "moderator")
									&& !receiver.getPlayer().getAccountID().equals(client.getPlayer().getAccountID())) {
								// Check limbo player
								Player gameClient = receiver.getPlayer().getOnlinePlayerInstance();
								if (gameClient != null && (!gameClient.roomReady || gameClient.room == null))
									continue;

								// Send to mod
								res = new JsonObject();
								res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
								res.addProperty("conversationId", room);
								res.addProperty("message", "</noparse><color=red>[!] </color><color=orange><noparse>"
										+ filterDefault.highlightedMessage + "</noparse></color><noparse>");
								res.addProperty("messagePlain", "[!] " + filterDefault.highlightedMessagePlain);
								res.addProperty("originalMessage", message);
								res.add("messageParts", filterDefault.messagePartsJson);
								res.addProperty("alertingMessage", true); // This is a moderator alerting message
								res.addProperty("criticalAlertingMessage", true); // Critical, should be red
								res.addProperty("blockedMessage", true); // The message was blocked, should be red
																			// highlighting
								res.addProperty("source", client.getPlayer().getAccountID());
								res.addProperty("sentAt", fmt.format(new Date()));
								res.addProperty("eventId", "chat.postMessage");
								res.addProperty("success", true);
								receiver.sendPacket(res);
							} else if (!receiver.isInRoom(room)
									&& !receiver.getPlayer().getAccountID().equals(client.getPlayer().getAccountID())) {
								// Not in room

								// Check moderator client
								if (receiver.getObject(ModeratorClient.class) != null) {
									// Send through centuria moderator protocol
									res = new JsonObject();
									res.addProperty("eventId", "centuria.moderatorclient.postedMessageInOtherRoom");
									res.addProperty("conversationType", "room");
									res.addProperty("message",
											"</noparse><color=red>[!] </color><color=orange><noparse>"
													+ filterDefault.highlightedMessage + "</noparse></color><noparse>");
									res.addProperty("messagePlain", "[!] " + filterDefault.highlightedMessagePlain);
									res.addProperty("originalMessage", message);
									res.add("messageParts", filterDefault.messagePartsJson);
									res.addProperty("alertingMessage", true); // This is a moderator alerting
																				// message
									res.addProperty("criticalAlertingMessage", true); // Critical, should be red
																						// exclamation
																						// mark
									res.addProperty("blockedMessage", true); // The message was blocked, should be
																				// red
																				// highlighting
									res.addProperty("source", client.getPlayer().getAccountID());
									res.addProperty("sentAt", fmt.format(new Date()));
									res.addProperty("success", true);

									// Send message
									receiver.sendPacket(res);
								}
							}
						}
					}

					// Mute
					client.getPlayer().mute(0, 0, 30, "SYSTEM",
							"Due to your continued breaches of the chat rules, you have been muted for 30 minutes.");

					// Send system message
					res = new JsonObject();
					res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
					res.addProperty("conversationId", room);
					res.addProperty("message",
							"Your message was blocked because it may not be appropriate.\nReason: With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.\n\nDue to your continued breaches of the chat rules, you have been muted for 30 minutes.\nWe ask you to keep chat respectful, safe and clean!");
					res.addProperty("source", NIL_UUID);
					res.addProperty("sentAt", fmt.format(new Date()));
					res.addProperty("eventId", "chat.postMessage");
					res.addProperty("success", true);
					client.sendPacket(res);
					mem.lastFlag = 0;
					mem.flagCount = 0;
					return true;
				}
			}

			// Time format
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

			// If it is a DM, save message
			if (client.isRoomPrivate(room) && manager.dmExists(room)) {
				PrivateChatMessage msg = new PrivateChatMessage();
				msg.content = filterDefault.filteredMessage;
				msg.sentAt = fmt.format(new Date());
				msg.source = client.getPlayer().getAccountID();
				if (ocProxyName != null)
					msg.source = "plaintext:" + ocProxyName;
				manager.saveDMMessge(room, msg);
			}

			// Select message
			String messageIn = filterDefault.filteredMessage;
			if (filteredUserStrictMode && filterSettingSelf != 0)
				messageIn = filterStrictMode.filteredMessage; // Strict mode, source had strict enabled

			// Send to all in room
			SocialManager socialManager = SocialManager.getInstance();
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
								&& !client.isRoomPrivate(room))
							continue;

						// Check if the sender has blocked this receiver, if so, prevent the receiver
						// from receiving the message that was sent, also applies to moderator-sent
						// messages unless its a dm and the player is a moderator
						if (socialManager.getPlayerIsBlocked(client.getPlayer().getAccountID(),
								receiver.getPlayer().getAccountID())) {
							// Check mod perms and room type
							if (GameServer.hasPerm(permLevel, "moderator")) {
								if (client.isInRoom(room) && !client.isRoomPrivate(room)) {
									continue; // Blocked
								}
							} else
								continue; // Blocked
						}

						// Load filter settings of the recipient
						int filterSetting = 0;
						UserVarValue val = receiver.getPlayer().getSaveSpecificInventory().getUserVarAccesor()
								.getPlayerVarValue(9362, 0);
						if (val != null)
							filterSetting = val.value;

						// Get filter result
						boolean filteredToRecipient = filteredDefaultSeverity
								|| (filteredUserStrictMode && filterSetting != 0);
						boolean filterUseStrictModeForRecipient = filteredUserStrictMode && filterSetting != 0;

						// Select message
						String filteredMessage = messageIn;
						if (filterUseStrictModeForRecipient)
							filteredMessage = filterStrictMode.filteredMessage;

						// Send response
						JsonObject res = new JsonObject();

						// Add properties
						res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
						res.addProperty("conversationId", room);

						// Add properties based on staff rank
						if ((GameServer.hasPerm(permLevel2, "moderator")
								|| receiver.getPlayer().getAccountID().equals(client.getPlayer().getAccountID()))
								&& (filteredDefaultSeverity || filteredUserStrictMode || filteredFlagged)) {
							// Is a moderator (or source) and a filter did trigger

							// Checks if moderator, we dont need to uncensor the message for non-mod
							boolean isModerator = GameServer.hasPerm(permLevel2, "moderator");

							// Determines if the flag is critical, if true, the exclamation is red,
							// otherwise its yellow to signify strict-mode, the box around the exclamation
							// mark is orange
							//
							// The message will be orange in both cases
							boolean isCriticalFlagged = filteredDefaultSeverity;

							// If this is true, the exclamation is green as its uncensored, but still
							// highlighted, the box around the exclamation mark is orange
							//
							// The message will be yellow
							boolean isAlertFlagged = filteredFlagged;

							// Check moderator
							if (!isModerator) {
								// Not a moderator, just highlight
								if (filteredToRecipient) {
									// Recipient was filtered

									// Check flag
									if (isCriticalFlagged)
										res.addProperty("message",
												"</noparse><color=orange>[<color=red>!</color>] </color><color=orange><noparse>"
														+ filteredMessage + "</noparse></color><noparse>");
									else
										res.addProperty("message",
												"</noparse><color=orange>[<color=yellow>!</color>] </color><color=orange><noparse>"
														+ filteredMessage + "</noparse></color><noparse>");

									// Add remaining
									res.addProperty("messagePlain", "[!] " + filteredMessage);
//									res.addProperty("originalMessage", message); // Not included for non-staff
									res.add("messageParts",
											filterUseStrictModeForRecipient ? filterStrictMode.messagePartsJson
													: filterDefault.messagePartsJson);
									res.addProperty("alertingMessage", true);
									res.addProperty("criticalAlertingMessage", isCriticalFlagged);
									res.addProperty("blockedMessage", false);
								} else {
									// Default
									res.addProperty("message", filteredMessage);
								}
							} else {
								// Is staff

								// Check flag
								if (isCriticalFlagged) {
									// Strict mode filter is used here as it includes non-strict during filtering,
									// it may catch more than the non-strict version
									res.addProperty("message",
											"</noparse><color=orange>[<color=red>!</color>] </color><color=orange><noparse>"
													+ (filteredUserStrictMode ? filterStrictMode.highlightedMessage
															: filterDefault.highlightedMessage)
													+ "</noparse></color><noparse>");
									res.addProperty("messagePlain",
											"[!] " + (filteredUserStrictMode ? filterStrictMode.highlightedMessagePlain
													: filterDefault.highlightedMessagePlain));
								} else if (filteredUserStrictMode) {
									// Strict mode filter is used here as it includes non-strict during filtering,
									// it may catch more than the non-strict version
									res.addProperty("message",
											"</noparse><color=orange>[<color=yellow>!</color>] </color><color=orange><noparse>"
													+ (filteredUserStrictMode ? filterStrictMode.highlightedMessage
															: filterDefault.highlightedMessage)
													+ "</noparse></color><noparse>");
									res.addProperty("messagePlain",
											"[!] " + (filteredUserStrictMode ? filterStrictMode.highlightedMessagePlain
													: filterDefault.highlightedMessagePlain));
								} else if (isAlertFlagged) {
									res.addProperty("message",
											"</noparse><color=orange>[<color=green>!</color>] </color><color=yellow><noparse>"
													+ filterFlagged.highlightedMessage + "</noparse></color><noparse>");
									res.addProperty("messagePlain", "[!] " + filterFlagged.highlightedMessagePlain);
								}

								// Add remaining fields
								res.addProperty("originalMessage", message);
								res.add("messageParts", filteredUserStrictMode ? filterStrictMode.messagePartsJson
										: filterDefault.messagePartsJson);
								res.addProperty("alertingMessage", true);
								res.addProperty("criticalAlertingMessage", isCriticalFlagged);
								res.addProperty("blockedMessage", false);
							}
						} else {
							// Default
							res.addProperty("message", filteredMessage);
						}

						// Add source and such
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
					// Moderator in other room
					if (receiver.getObject(ModeratorClient.class) != null) {
						// Send through centuria moderator protocol
						JsonObject res = new JsonObject();
						res.addProperty("eventId", "centuria.moderatorclient.postedMessageInOtherRoom");
						res.addProperty("conversationType", "room");
						res.addProperty("conversationId", room);

						// Determines if the flag is critical, if true, the exclamation is red,
						// otherwise its yellow to signify strict-mode, the box around the exclamation
						// mark is orange
						//
						// The message will be orange in both cases
						boolean isCriticalFlagged = filteredDefaultSeverity;

						// If this is true, the exclamation is green as its uncensored, but still
						// highlighted, the box around the exclamation mark is orange
						//
						// The message will be yellow
						boolean isAlertFlagged = filteredFlagged;

						// Check flag
						if (isCriticalFlagged) {
							// Strict mode filter is used here as it includes non-strict during filtering,
							// it may catch more than the non-strict version
							res.addProperty("message",
									"</noparse><color=orange>[<color=red>!</color>] </color><color=orange><noparse>"
											+ (filteredUserStrictMode ? filterStrictMode.highlightedMessage
													: filterDefault.highlightedMessage)
											+ "</noparse></color><noparse>");
							res.addProperty("messagePlain",
									"[!] " + (filteredUserStrictMode ? filterStrictMode.highlightedMessagePlain
											: filterDefault.highlightedMessagePlain));
						} else if (filteredUserStrictMode) {
							// Strict mode filter is used here as it includes non-strict during filtering,
							// it may catch more than the non-strict version
							res.addProperty("message",
									"</noparse><color=orange>[<color=yellow>!</color>] </color><color=orange><noparse>"
											+ (filteredUserStrictMode ? filterStrictMode.highlightedMessage
													: filterDefault.highlightedMessage)
											+ "</noparse></color><noparse>");
							res.addProperty("messagePlain",
									"[!] " + (filteredUserStrictMode ? filterStrictMode.highlightedMessagePlain
											: filterDefault.highlightedMessagePlain));
						} else if (isAlertFlagged) {
							res.addProperty("message",
									"</noparse><color=orange>[<color=green>!</color>] </color><color=yellow><noparse>"
											+ filterFlagged.highlightedMessage + "</noparse></color><noparse>");
							res.addProperty("messagePlain", "[!] " + filterFlagged.highlightedMessagePlain);
						}

						// Add remaining fields
						res.addProperty("originalMessage", message);
						res.add("messageParts", filteredUserStrictMode ? filterStrictMode.messagePartsJson
								: filterDefault.messagePartsJson);
						res.addProperty("alertingMessage", true);
						res.addProperty("criticalAlertingMessage", isCriticalFlagged);
						res.addProperty("blockedMessage", false);

						// Add source and such
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
				}
			}

			// Check censor
			if (filteredDefaultSeverity) {
				// Get/create memory
				ChatFilterMemory mem = client.getObject(ChatFilterMemory.class);

				// Check count
				if (mem.flagCount == 1) {
					// Send message
					JsonObject res = new JsonObject();
					res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
					res.addProperty("conversationId", room);
					res.addProperty("message",
							"Your message was censored because it may not be appropriate.\nReason: With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.\nWe ask you to keep chat respectful, safe and clean.");
					res.addProperty("source", NIL_UUID);
					res.addProperty("sentAt", fmt.format(new Date()));
					res.addProperty("eventId", "chat.postMessage");
					res.addProperty("success", true);
					client.sendPacket(res);

					// Mod log
					if (client.isRoomPrivate(room)) {
						// Private chat, need more details
						// And strip away the message
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.censored",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Private chat room", formatRoomName(client, room), "Matched word(s)",
										filterDefault.matchedWordsString, "Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "censored"),
								"SYSTEM", client.getPlayer()));
					} else {
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.censored",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Chat message", message, "Matched word(s)", filterDefault.matchedWordsString,
										"Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "censored"),
								"SYSTEM", client.getPlayer()));
					}
				} else if (mem.flagCount == 2) {
					// Send message
					JsonObject res = new JsonObject();
					res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
					res.addProperty("conversationId", room);
					res.addProperty("message",
							"Your message was censored because it may not be appropriate.\nReason: With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.\n\nThis is your first warning, if you continue to breach the chat rules, your account will be muted.\nWe ask you to keep chat respectful, safe and clean.");
					res.addProperty("source", NIL_UUID);
					res.addProperty("sentAt", fmt.format(new Date()));
					res.addProperty("eventId", "chat.postMessage");
					res.addProperty("success", true);
					client.sendPacket(res);

					// Mod log
					if (client.isRoomPrivate(room)) {
						// Private chat, need more details
						// And strip away the message
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.censored",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Private chat room", formatRoomName(client, room), "Matched word(s)",
										filterDefault.matchedWordsString, "Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "first warning"),
								"SYSTEM", client.getPlayer()));
					} else {
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.censored",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Chat message", message, "Matched word(s)", filterDefault.matchedWordsString,
										"Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "first warning"),
								"SYSTEM", client.getPlayer()));
					}
				} else if (mem.flagCount == 3) {
					// Send message
					JsonObject res = new JsonObject();
					res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
					res.addProperty("conversationId", room);
					res.addProperty("message",
							"Your message was censored because it may not be appropriate.\nReason: With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.\n\nThis is your LAST warning, the next breach of chat rules will result in a mute.\nWe ask you to keep chat respectful, safe and clean.");
					res.addProperty("source", NIL_UUID);
					res.addProperty("sentAt", fmt.format(new Date()));
					res.addProperty("eventId", "chat.postMessage");
					res.addProperty("success", true);
					client.sendPacket(res);

					// Mod log
					if (client.isRoomPrivate(room)) {
						// Private chat, need more details
						// And strip away the message
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.censored",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Private chat room", formatRoomName(client, room), "Matched word(s)",
										filterDefault.matchedWordsString, "Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "final warning"),
								"SYSTEM", client.getPlayer()));
					} else {
						EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.censored",
								"Chat filter has flagged player " + client.getPlayer().getDisplayName() + "!",
								Map.of("Chat message", message, "Matched word(s)", filterDefault.matchedWordsString,
										"Primary reason for filtering",
										"With our software being for a target audience that includes minors, we do not permit NSFW terms in chat.",
										"Room", formatRoomName(client, room), "Resulting action", "final warning"),
								"SYSTEM", client.getPlayer()));
					}
				}
			} else if (filteredUserStrictMode && filterSettingSelf != 0) {
				// Send message
				JsonObject res = new JsonObject();
				res.addProperty("conversationType", client.isRoomPrivate(room) ? "private" : "room");
				res.addProperty("conversationId", room);
				res.addProperty("message",
						"Your message was censored because of your current settings.\nIf you wish to not have this message flagged, please change your game's chat settings.");
				res.addProperty("source", NIL_UUID);
				res.addProperty("sentAt", fmt.format(new Date()));
				res.addProperty("eventId", "chat.postMessage");
				res.addProperty("success", true);
				client.sendPacket(res);
			}

			// Check if flagged
			if (filteredFlaggedWithoutStrictmode && !client.isRoomPrivate(room)) {
				// Alert staff if needed

				// Check if staff is present
				boolean hasStaffInRoom = false;
				for (ChatClient c : client.getServer().getClients()) {
					// Fetch receiver moderator perms
					String permLevel2 = "member";
					if (c.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
						permLevel2 = c.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
								.get("permissionLevel").getAsString();
					}
					if (GameServer.hasPerm(permLevel2, "moderator") && c.isInRoom(room)) {
						hasStaffInRoom = true;
						break;
					}
				}

				// Mod log
				EventBus.getInstance().dispatchEvent(new MiscModerationEvent("chatfilter.alert",
						"Chat filter alert! Player " + client.getPlayer().getDisplayName()
								+ " sent a message that was flagged by the system!",
						Map.of("Chat message", message, "Matched word(s)", filterDefault.matchedWordsString,
								"Primary reason for alerting",
								"The server has been configured to alert staff for when these specific word(s) are mentioned ingame.",
								"Room", formatRoomName(client, room), "Resulting action",
								"no action taken, only alerting staff"),
						"SYSTEM", client.getPlayer(), !hasStaffInRoom));
			}
		}

		return true;
	}

	private static class MessagePart {
		public String part;
		public boolean censored;

		public JsonObject partJson;
	}

	private static class FilterResult {
		public boolean wasFiltered;

		public String originalMessage;
		public String highlightedMessage;
		public String highlightedMessagePlain;
		public String filteredMessage;

		public String[] matchedWords;
		public MessagePart[] messageParts;
		public JsonArray messagePartsJson;

		public String matchedWordsString;
	}

	private static FilterResult runFilter(boolean moderationHighlight, boolean includePartJson, String message,
			ArrayList<String> filter, String highlightColor) {
		FilterResult result = new FilterResult();
		result.originalMessage = message;

		String matchedWords = "";
		String filteredMessage = "";
		String highlightedMessage = "";
		String highlightedMessagePlain = "";
		ArrayList<MessagePart> messageParts = new ArrayList<MessagePart>();
		ArrayList<String> matchList = new ArrayList<String>();
		MessagePart currentPart = null;
		for (String mword : message.split(" ")) {
			if (filter.contains(mword.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				// Add match
				if (!matchList.contains(mword.toLowerCase())) {
					// Add matched word
					if (matchedWords.isEmpty())
						matchedWords = mword;
					else
						matchedWords += ", " + mword;
					matchList.add(mword.toLowerCase());
				}

				// Tag
				String tag = "";
				for (int i = 0; i < mword.length(); i++) {
					tag += "#";
				}

				// Add message part
				if (filteredMessage.isEmpty()) {
					filteredMessage = tag;
				} else {
					filteredMessage += " " + tag;
				}

				// Check if highlight is enabled
				if (moderationHighlight) {
					// Create highlight
					String highlight = "</noparse><color="+highlightColor+"><noparse>" + mword + "</noparse></color><noparse>";

					// Add message part
					if (highlightedMessage.isEmpty()) {
						highlightedMessage = highlight;
						highlightedMessagePlain = "[!]" + mword + "[!]";
					} else {
						highlightedMessage += " " + highlight;
						highlightedMessagePlain += " [!]" + mword + "[!]";
					}
				}

				// Update part entry
				if (currentPart != null && !currentPart.censored) {
					if (includePartJson) {
						currentPart.partJson = new JsonObject();
						currentPart.partJson.addProperty("text", currentPart.part);
						currentPart.partJson.addProperty("censored", currentPart.censored);
					}
					messageParts.add(currentPart);
					currentPart = null;
				}
				if (currentPart == null) {
					currentPart = new MessagePart();
					currentPart.censored = true;
					currentPart.part = mword;
				} else {
					currentPart.part += " " + mword;
				}

				// Mark filtered
				result.wasFiltered = true;
			} else {
				// Add part
				if (filteredMessage.isEmpty()) {
					filteredMessage = mword;
				} else {
					filteredMessage += " " + mword;
				}

				// Moderation highlight part
				if (moderationHighlight) {
					if (highlightedMessage.isEmpty()) {
						highlightedMessage = mword;
						highlightedMessagePlain = mword;
					} else {
						highlightedMessage += " " + mword;
						highlightedMessagePlain += " " + mword;
					}
				}

				// Update part entry
				if (currentPart != null && currentPart.censored) {
					if (includePartJson) {
						currentPart.partJson = new JsonObject();
						currentPart.partJson.addProperty("text", currentPart.part);
						currentPart.partJson.addProperty("censored", currentPart.censored);
					}
					messageParts.add(currentPart);
					currentPart = null;
				}
				if (currentPart == null) {
					currentPart = new MessagePart();
					currentPart.censored = false;
					currentPart.part = mword;
				} else {
					currentPart.part += " " + mword;
				}
			}
		}
		if (currentPart != null) {
			if (includePartJson) {
				currentPart.partJson = new JsonObject();
				currentPart.partJson.addProperty("text", currentPart.part);
				currentPart.partJson.addProperty("censored", currentPart.censored);
			}
			messageParts.add(currentPart);
		}

		// Apply
		result.filteredMessage = filteredMessage;
		result.highlightedMessage = highlightedMessage;
		result.highlightedMessagePlain = highlightedMessagePlain;
		result.matchedWordsString = matchedWords;
		result.matchedWords = matchList.toArray(t -> new String[t]);
		result.messageParts = messageParts.toArray(t -> new MessagePart[t]);
		if (includePartJson) {
			result.messagePartsJson = new JsonArray();
			for (MessagePart part : messageParts)
				result.messagePartsJson.add(part.partJson);
		}

		return result;
	}

	private static class ChatFilterMemory {
		public long lastFlag = 0;
		public int flagCount = 0;
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
		if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemFurnitureItems
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemMods
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
				|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemSanctuaryTypes)
			commandMessages.add("restockInventory");

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
			commandMessages.add("setplayertag \"<tag id>\" [\"<player>\"] [\"<escaped  tag json data>\"]");
			commandMessages.add("removeplayertag \"<tag id>\" [\"<player>\"]");
			if (GameServer.hasPerm(permLevel, "admin")) {
				commandMessages.add("generateclearancecode");
				commandMessages.add("addxp <amount> [\"<player>\"]");
				commandMessages.add("addlevels <amount> [\"<player>\"]");
				commandMessages.add("resetalllevels [confirm]");
				commandMessages.add("tpm <levelDefID> [<levelType>] [<player>]");
				commandMessages.add("makeadmin \"<player>\"");
				commandMessages.add("makemoderator \"<player>\"");
				commandMessages.add("removeperms \"<player>\"");
				commandMessages.add("startmaintenance");
				commandMessages.add("endmaintenance");
				commandMessages.add("shutdownserver [\"<reason>\"]");
				commandMessages.add("updatewarning <minutes-remaining>");
				commandMessages.add("updateshutdown [\"<reason>\"]");
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
			commandMessages.add("tptosanctuary \"<sanctuary owner player name>\" [\"<target player>\"]");
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

		// Dice
		commandMessages.add(
				"roll [<amount of rolls>][d<size>] (eg. roll d20 (1 roll of a d20 dice), roll 2d42 (2 rolls of a d42 dice), roll d20 (rolls a single d20 dice), roll 10 (rolls 10 d20 dices))");

		// OC proxying
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
		commandMessages.add("oc stickyproxy \"<name>\"");
		commandMessages.add("oc stickyoff");
		commandMessages.add("oc show \"<name>\" [\"<player>\"]");
		commandMessages.add("oc list [\"<player>\"]");
		if (!GameServer.hasPerm(permLevel, "moderator")) {
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

				if (cmdId.equals("givebasicmaterials")) {
					if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
							|| GameServer.hasPerm(permLevel, "admin")) {
						var onlinePlayer = client.getPlayer().getOnlinePlayerInstance();

						if (onlinePlayer != null) {
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
						}
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

						if (onlinePlayer != null) {
							var accessor = client.getPlayer().getSaveSpecificInventory().getCurrencyAccessor();

							accessor.addLikes(onlinePlayer.client, 1000);
							accessor.addStarFragments(onlinePlayer.client, 1000);

							// TODO: Check result
							systemMessage("You have been given 1000 star fragments and likes. Have fun!", cmd, client);
						}
						return true;
					}
				} else if (cmdId.equals("restockinventory")) {
					if (client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemClothes
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemCurrency
							|| client.getPlayer().getSaveSpecificInventory()
									.getSaveSettings().allowGiveItemFurnitureItems
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemMods
							|| client.getPlayer().getSaveSpecificInventory().getSaveSettings().allowGiveItemResources
							|| client.getPlayer().getSaveSpecificInventory()
									.getSaveSettings().allowGiveItemSanctuaryTypes) {
						var onlinePlayer = client.getPlayer().getOnlinePlayerInstance();
						String cmdF = cmd;
						AsyncTaskManager.runAsync(() -> {
							systemMessage("Restocking inventory... Please be patient, your game may lag for a bit...",
									cmdF, client);
							Centuria.gameServer.stockInventory(onlinePlayer, client.getPlayer(),
									client.getPlayer().getSaveSpecificInventory(), true);
							systemMessage("Your inventory has been restocked! Have fun!", cmdF, client);
						});
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
					// FIXME: patch issue where this will break after completing all quests
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
				} else if (cmdId.equals("roll")) {
					// Check size
					int amount = 1;
					int size = 20;
					if (args.size() >= 1) {
						String sizeStr = args.get(0);
						if (sizeStr.toLowerCase().contains("d")) {
							String amountStr = sizeStr.substring(0, sizeStr.toLowerCase().indexOf("d"));
							String maxStr = sizeStr.substring(sizeStr.toLowerCase().indexOf("d") + 1);
							if (!maxStr.matches("^-?[0-9]+$")) {
								// Invalid argument
								systemMessage("Invalid argument: size: not a valid number", cmd, client);
								return true;
							}
							if (!amountStr.isEmpty() && !amountStr.matches("^-?[0-9]+$")) {
								// Invalid argument
								systemMessage("Invalid argument: amount: not a valid number", cmd, client);
								return true;
							}
							if (!amountStr.isEmpty()) {
								try {
									amount = Integer.parseInt(amountStr);
								} catch (Exception e) {
									// Invalid argument
									systemMessage("Invalid argument: amount: not a valid number", cmd, client);
									return true;
								}
							}
							try {
								size = Integer.parseInt(maxStr);
							} catch (Exception e) {
								// Invalid argument
								systemMessage("Invalid argument: size: not a valid number", cmd, client);
								return true;
							}

						} else {
							try {
								amount = Integer.parseInt(sizeStr);
							} catch (Exception e) {
								// Invalid argument
								systemMessage("Invalid argument: amount: not a valid number", cmd, client);
								return true;
							}
						}
						if (size < 1) {
							// Invalid argument
							systemMessage("Invalid argument: size: cannot be less than one", cmd, client);
							return true;
						}
						if (amount < 1) {
							// Invalid argument
							systemMessage("Invalid argument: amount: cannot roll less than one dice", cmd, client);
							return true;
						}
						if (amount > 10) {
							// Invalid argument
							systemMessage("Invalid argument: amount: cannot roll more than 10 dices at once", cmd,
									client);
							return true;
						}
					}

					// Roll
					String result = "";
					if (amount != 1) {
						for (int i = 0; i < amount; i++) {
							if (!result.isEmpty())
								result += "\n";
							result += "Dice roll #" + (i + 1) + ": rolled " + rnd.nextInt(1, size + 1);
						}
					} else
						result = "Rolled " + rnd.nextInt(1, size + 1);

					// Send result
					systemMessage(result, cmd, client);
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

						// Verify name with blacklist
						for (String nameB : nameBlacklist) {
							if (name.equalsIgnoreCase(nameB)) {
								// Reply with error
								systemMessage(
										"Invalid argument: name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}
						}

						// Verify name with filters
						for (String word : name.split(" ")) {
							if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}

							if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}

							if (alwaysfilterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}
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

						// Verify name with blacklist
						for (String nameB : nameBlacklist) {
							if (newName.equalsIgnoreCase(nameB)) {
								// Reply with error
								systemMessage(
										"Invalid argument: new name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}
						}

						// Verify name with filters
						for (String word : newName.split(" ")) {
							if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: new name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}

							if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: new name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}

							if (alwaysfilterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: new name: this name was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}
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
						for (String word : name.split(" ")) {
							if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage("Invalid argument: bio: this bio was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}

							if (alwaysfilterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage("Invalid argument: bio: this bio was blocked as it may be inappropriate",
										cmd + " " + task, client);
								return true;
							}
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
						for (String word : name.split(" ")) {
							if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: pronouns: these pronouns were blocked as they may be inappropriate",
										cmd + " " + task, client);
								return true;
							}

							if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: pronouns: these pronouns were blocked as they may be inappropriate",
										cmd + " " + task, client);
								return true;
							}

							if (alwaysfilterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
								// Reply with error
								systemMessage(
										"Invalid argument: pronouns: these pronouns were blocked as they may be inappropriate",
										cmd + " " + task, client);
								return true;
							}
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

						// Filter bio
						String bioFiltered = "";

						// Get OC
						OcProxyInfo oc = OcProxyInfo.ofUser(acc, name);

						// Load filter settings
						int filterSetting = 0;
						UserVarValue val = client.getPlayer().getSaveSpecificInventory().getUserVarAccesor()
								.getPlayerVarValue(9362, 0);
						if (val != null)
							filterSetting = val.value;

						// Check filter
						for (String word : oc.characterBio.split(" ")) {
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

							if (!bioFiltered.isEmpty())
								bioFiltered += " " + word;
							else
								bioFiltered = word;
						}

						// Show overview
						systemMessage("Overview of " + name + ":\n" + "\nName: " + oc.displayName + "\nPronouns: "
								+ oc.characterPronouns
								+ (uuid.equals(client.getPlayer().getAccountID())
										? "\nTrigger: </noparse><mark><noparse>" + oc.triggerPrefix + "message"
												+ oc.triggerSuffix + "</noparse></mark><noparse>"
										: "")
								+ "\n" + "\nBio:" + "\n" + bioFiltered, cmd + " " + task, client);

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
								plr.teleportToRoom(plr.levelID, plr.levelType, 0, plr.room,
										plr.room.startsWith("sanctuary_") ? plr.room.substring("sanctuary_".length())
												: "");
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
							plr.teleportToRoom(plrTarget.levelID, plrTarget.levelType, 0, plrTarget.room,
									plrTarget.room.startsWith("sanctuary_")
											? plrTarget.room.substring("sanctuary_".length())
											: "");
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
							plr.teleportToRoom(plrTarget.levelID, plrTarget.levelType, 0, plrTarget.room,
									plrTarget.room.startsWith("sanctuary_")
											? plrTarget.room.substring("sanctuary_".length())
											: "");
							systemMessage("Teleported " + plr.account.getDisplayName() + " to "
									+ plrTarget.account.getDisplayName(), cmd, client);
						}

						// Done
						systemMessage("Bulk-teleport completed!", cmd, client);
						return true;
					}

					case "tptosanctuary": {
						// Sanctuary teleport

						// Player
						if (args.size() < 1) {
							systemMessage("Missing argument: owner player name", cmd, client);
							return true;
						}
						String player = args.get(0);
						String uuid = AccountManager.getInstance().getUserByDisplayName(player);
						if (uuid == null) {
							// Player not found
							systemMessage("Specified account could not be located.", cmd, client);
							return true;
						}
						CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);

						// Get current
						Player plrS = client.getPlayer().getOnlinePlayerInstance();

						// Check
						if (args.size() >= 2) {
							String target = args.get(1);
							uuid = AccountManager.getInstance().getUserByDisplayName(target);
							if (uuid == null) {
								// Player not found
								systemMessage("Specified target account could not be located.", cmd, client);
								return true;
							}
							CenturiaAccount acc2 = AccountManager.getInstance().getAccount(uuid);
							plrS = acc2.getOnlinePlayerInstance();
						}

						// Check
						if (plrS == null || !plrS.roomReady) {
							// Player not found
							systemMessage("Player to teleport is not online or not fully in world yet.", cmd, client);
							return true;
						}

						// Teleport
						plrS.teleportToSanctuary(acc.getAccountID(), true);

						// Done
						systemMessage("Successfully teleported " + plrS.account.getDisplayName() + " to "
								+ acc.getDisplayName() + "'s sanctuary", cmd, client);
						return true;
					}

					case "tpall": {
						// Teleport tool

						// Get current
						Player plrS = client.getPlayer().getOnlinePlayerInstance();
						if (plrS == null || !plrS.roomReady) {
							// Player not found
							systemMessage("Your player is not online or not fully in world yet.", cmd, client);
							return true;
						}

						// Find players in room
						for (Player plr : Centuria.gameServer.getPlayers()) {
							// Check ready
							if (!plr.roomReady)
								continue;

							// Determine mode
							if (args.size() >= 3) {
								try {
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
									plr.teleportToRoom(plr.levelID, plr.levelType, 0, plr.room,
											plr.room.startsWith("sanctuary_")
													? plr.room.substring("sanctuary_".length())
													: "");
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
										|| !plr.room.equals(plrS.room))
									continue;

								// Handle teleport
								plr.teleportDestination = plrTarget.account.getAccountID();
								plr.targetPos = plrTarget.lastPos;
								plr.targetRot = plrTarget.lastRot;
								plr.teleportToRoom(plrTarget.levelID, plrTarget.levelType, 0, plrTarget.room,
										plrTarget.room.startsWith("sanctuary_")
												? plrTarget.room.substring("sanctuary_".length())
												: "");
								systemMessage("Teleported " + plr.account.getDisplayName() + " to "
										+ plrTarget.account.getDisplayName(), cmd, client);
							}
						}

						// Done
						systemMessage("Bulk-teleport completed!", cmd, client);
						return true;
					}

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
						ArrayList<Integer> levelIDs = new ArrayList<Integer>();
						HashMap<Player, String> playersInRooms = new HashMap<Player, String>();
						for (Player plr : Centuria.gameServer.getPlayers()) {
							if (!mapLessClients.contains(plr.account.getAccountID())
									&& (plr.roomReady || plr.levelID == 25280)) {
								// Increase count
								ingame++;

								// Add level if missing
								if (!levelIDs.contains(plr.levelID)) {
									levelIDs.add(plr.levelID);
								}

								// Add to room map
								if (plr.room != null)
									playersInRooms.put(plr, plr.room);
							}
						}

						// Build message
						String response = Centuria.gameServer.getPlayers().length + " player(s) connected, " + ingame
								+ " player(s) in world:";

						// Add each level
						ArrayList<String> playerIDs = new ArrayList<String>();
						for (int levelID : levelIDs) {
							// Determine map name
							String map = "UNKNOWN: " + levelID;
							if (levelID == 25280)
								map = "Tutorial [" + levelID + "]";
							else if (helper.has(Integer.toString(levelID)))
								map = helper.get(Integer.toString(levelID)).getAsString() + " [" + levelID + "]";

							// Players
							for (Player plr : playersInRooms.keySet()) {
								if (!mapLessClients.contains(plr.account.getAccountID())
										&& !playerIDs.contains(plr.account.getAccountID())) {
									// Check
									if (plr.levelID == levelID) {
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
							while (true) {
								try {
									if (clearanceCodes.contains(args.get(2))) {
										clearanceCodes.remove(args.get(2));
									} else {
										systemMessage("Error: invalid clearance code.", cmd, client);
										return true;
									}
									break;
								} catch (ConcurrentModificationException e) {
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
							while (true) {
								try {
									if (clearanceCodes.contains(args.get(1))) {
										clearanceCodes.remove(args.get(1));
									} else {
										systemMessage("Error: invalid clearance code.", cmd, client);
										return true;
									}
									break;
								} catch (ConcurrentModificationException e) {
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
								while (true) {
									try {
										if (clearanceCodes.contains(args.get(0))) {
											clearanceCodes.remove(args.get(0));
										} else {
											systemMessage("Error: invalid clearance code.", cmd, client);
											return true;
										}
										break;
									} catch (ConcurrentModificationException e) {
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
								try {
									if (!clearanceCodes.contains(code))
										break;
								} catch (ConcurrentModificationException e) {
								}
								code = Long.toString(rnd.nextLong(), 16);
							}
							clearanceCodes.add(code);
							EventBus.getInstance()
									.dispatchEvent(new MiscModerationEvent("clearancecode.generated",
											"Admin Clearance Code Generated", Map.of(),
											client.getPlayer().getAccountID(), null));
							systemMessage("Clearance code generated: " + code + "\nIt will expire in 2 minutes.", cmd,
									client);
							final String cFinal = code;
							Thread th = new Thread(() -> {
								for (int i = 0; i < 12000; i++) {
									try {
										if (!clearanceCodes.contains(cFinal))
											return;
									} catch (ConcurrentModificationException e) {
									}
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
									}
								}
								clearanceCodes.remove(cFinal);
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
									EventBus.getInstance().dispatchEvent(
											new AccountDisconnectEvent(plr.account, null, DisconnectType.MAINTENANCE));

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
						if (!id.matches("^[A-Za-z0-9_\\-. ]+$")) {
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
						if (!id.matches("^[A-Za-z0-9_\\-. ]+$")) {
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

								// Find player
								String player = client.getPlayer().getDisplayName();
								if (args.size() >= 3) {
									player = args.get(2);
								}
								String uuid = AccountManager.getInstance().getUserByDisplayName(player);
								if (uuid == null) {
									// Player not found
									systemMessage("Specified account could not be located.", cmd, client);
									return true;
								}
								CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
								Player plr = acc.getOnlinePlayerInstance();
								if (plr != null)
									plr.teleportToRoom(Integer.valueOf(defID), Integer.valueOf(type), -1,
											"room_" + defID, "");
								else {
									// Player not found
									systemMessage("Specified player is not online.", cmd, client);
									return true;
								}
							} catch (Exception e) {
								e.printStackTrace();
								systemMessage("Error: " + e, cmd, client);
							}
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
											+ QuestManager.getQuest(QuestManager.getActiveQuest(acc)).name,
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
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("1")
										&& !ItemAccessor.getInventoryTypeOf(defID).equals("7"))) {
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
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemAvatars)
								|| (ItemAccessor.getInventoryTypeOf(defID).equals("7") && !client.getPlayer()
										.getSaveSpecificInventory().getSaveSettings().allowGiveItemEnigmas)) {
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
				// User listplayers command
				if (cmd.equals("listplayers")) {
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

					// Find level IDs
					int ingame = 0;
					HashMap<Integer, Integer> levelIDs = new HashMap<Integer, Integer>();
					for (Player plr : Centuria.gameServer.getPlayers()) {
						if (plr.roomReady || plr.levelID == 25280) {
							// Increase count
							ingame++;

							// Add
							levelIDs.put(plr.levelID, levelIDs.getOrDefault(plr.levelID, 0) + 1);
						}
					}

					// Build message
					String message = "There are " + Centuria.gameServer.getPlayers().length
							+ " player(s) connected and " + ingame + " player(s) in world.";
					if (levelIDs.size() != 0) {
						message += "\n";
						message += "\n";
						for (int levelID : levelIDs.keySet()) {
							// Determine map name
							String map = "UNKNOWN: " + levelID;
							if (levelID == 25280)
								map = "Tutorial";
							else if (helper.has(Integer.toString(levelID)))
								map = helper.get(Integer.toString(levelID)).getAsString();
							message += map + ": " + levelIDs.get(levelID) + " players.";
							message += "\n";
						}
					}
					systemMessage(message, cmdId, client);
					return true;
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

	private static String formatRoomName(ChatClient client, String room) {
		if (client.isRoomPrivate(room)) {
			// Find recipient
			String recipient = client.getPlayer().getDisplayName();
			String[] participants = DMManager.getInstance().getDMParticipants(room);
			for (String p : participants) {
				if (!p.equals(client.getPlayer().getAccountID())) {
					// Check type
					if (p.startsWith("plaintext:")) {
						recipient = p.substring("plaintext:".length());
						break;
					} else {
						CenturiaAccount a = AccountManager.getInstance().getAccount(p);
						if (a != null)
							recipient = a.getDisplayName();
					}
				}
			}
			return "PM to " + recipient;
		} else {
			// Check room format
			if (room.startsWith("room_")) {
				// Public room

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

				String levelId = room.substring("room_".length());
				String map = "UNKNOWN: " + levelId;
				if (levelId.equals("25280"))
					map = "Tutorial";
				else if (helper.has(levelId))
					map = helper.get(levelId).getAsString() + " [" + levelId + "]";
				return map;
			} else if (room.startsWith("sanctuary_")) {
				// Sanctuary
				String owner = room.substring("sanctuary_".length());
				CenturiaAccount a = AccountManager.getInstance().getAccount(owner);
				if (a != null)
					owner = a.getDisplayName();
				return "Sanctuary of " + owner;
			}
			return "Unknown: " + room;
		}
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