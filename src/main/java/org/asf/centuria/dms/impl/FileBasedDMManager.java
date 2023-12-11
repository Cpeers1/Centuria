package org.asf.centuria.dms.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.dms.PrivateChatMessage;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.chat.ChatConversationDeletionWarningEvent;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.social.SocialManager;
import org.asf.connective.tasks.AsyncTaskManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileBasedDMManager extends DMManager {

	private static ArrayList<String> activeIDs = new ArrayList<String>();

	static {
		// Start watchdog
		AsyncTaskManager.runAsync(() -> {
			if (!new File("dms").exists())
				new File("dms").mkdirs();
			while (true) {
				// Go through DMs
				for (File f : new File("dms").listFiles()) {
					if (f.isFile() && f.getName().endsWith(".json")) {
						String dmID = f.getName().replace(".json", "");
						try {
							UUID.fromString(dmID);
							dmExpiryVerificationLogic(dmID);
						} catch (Exception e) {
						}
					}
				}
				try {
					Thread.sleep(24 * 60 * 60 * 1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
	}

	private static void dmExpiryVerificationLogic(String dmID) {
		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + dmID + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			reader.close();

			// Check
			if (!dm.has("lastUpdate")) {
				// Update DM
				dm.addProperty("lastUpdate", System.currentTimeMillis());
				dm.addProperty("warnedExpiry", false);
				Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
				return;
			}

			// Check expiry
			long timeSinceLastMessage = System.currentTimeMillis() - dm.get("lastUpdate").getAsLong();
			if (timeSinceLastMessage >= (30 * 24 * 60 * 60 * 1000l)) {
				// 30 days of inactivity
				// Save warning if needed
				if (!dm.get("warnedExpiry").getAsBoolean()) {
					// Create message
					SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					String msgT = "WARNING! This private chat has been inactive for over 30 days, should it remain inactive for 30 more days, it will be deleted! Please send a message if you wish this chat to remain to exist. (this was sent on "
							+ fmt.format(new Date()) + " UTC";

					// Send notification to all participants that are connected
					fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					for (String participant : getInstance().getDMParticipants(dmID)) {
						ChatClient client = Centuria.chatServer.getClient(participant);
						if (client != null) {
							JsonObject res = new JsonObject();
							res.addProperty("conversationType", "private");
							res.addProperty("conversationId", dmID);
							res.addProperty("message", msgT);
							res.addProperty("source", new UUID(0, 0).toString());
							res.addProperty("sentAt", fmt.format(new Date()));
							res.addProperty("eventId", "chat.postMessage");
							res.addProperty("success", true);
							client.sendPacket(res);
						}
					}

					// Dispatch event
					EventBus.getInstance().dispatchEvent(new ChatConversationDeletionWarningEvent(dmID));

					// Save message
					JsonArray data = dm.get("messages").getAsJsonArray();

					// Add message
					JsonObject msg = new JsonObject();
					msg.addProperty("c", msgT);
					msg.addProperty("s", new UUID(0, 0).toString());
					msg.addProperty("a", System.currentTimeMillis());
					data.add(msg);

					// Mark warned
					dm.addProperty("warnedExpiry", true);

					// Save to disk
					while (activeIDs.contains(dmID))
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							break;
						}
					activeIDs.add(dmID);
					Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
					activeIDs.remove(dmID);
				}
			}

			if (timeSinceLastMessage >= (60 * 24 * 60 * 60 * 1000l)) {
				// 2 months of inactivity
				// Delete this private chat

				// Create message
				String msgT = "This private chat has been deleted due to inactivity and wont accept further messages";

				// Send notification to all participants that are connected
				SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				for (String participant : getInstance().getDMParticipants(dmID)) {
					ChatClient client = Centuria.chatServer.getClient(participant);
					if (client != null) {
						JsonObject res = new JsonObject();
						res.addProperty("conversationType", "private");
						res.addProperty("conversationId", dmID);
						res.addProperty("message", msgT);
						res.addProperty("source", new UUID(0, 0).toString());
						res.addProperty("sentAt", fmt.format(new Date()));
						res.addProperty("eventId", "chat.postMessage");
						res.addProperty("success", true);
						client.sendPacket(res);
					}
				}

				// Delete the conversation
				getInstance().deleteDM(dmID);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void openDM(String dmID, String[] participants) {
		try {
			if (!new File("dms").exists())
				new File("dms").mkdirs();
			if (!dmExists(dmID)) {
				JsonObject dm = new JsonObject();
				JsonArray participantObjects = new JsonArray();
				for (String p : participants)
					participantObjects.add(p);
				dm.add("participants", participantObjects);
				dm.add("messages", new JsonArray());
				dm.addProperty("lastUpdate", System.currentTimeMillis());
				dm.addProperty("warnedExpiry", false);
				Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			}
		} catch (Exception e) {
		}
	}

	@Override
	public boolean dmExists(String dmID) {
		try {
			return new File("dms/" + UUID.fromString(dmID) + ".json").exists();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public PrivateChatMessage[] getDMHistory(String dmID, String requester) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("messages").getAsJsonArray();
			reader.close();

			ArrayList<PrivateChatMessage> messages = new ArrayList<PrivateChatMessage>();
			for (JsonElement ele : data) {
				JsonObject msg = ele.getAsJsonObject();
				String source;
				if (msg.has("source"))
					source = msg.get("source").getAsString();
				else
					source = msg.get("s").getAsString();

				if (SocialManager.getInstance().socialListExists(requester)
						&& SocialManager.getInstance().getPlayerIsBlocked(requester, source))
					continue;

				PrivateChatMessage message = new PrivateChatMessage();
				message.content = msg.has("content") ? msg.get("content").getAsString() : msg.get("c").getAsString();
				message.source = source;
				if (msg.has("sentAt")) {
					// Parse old format
					SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					message.sentAt = 0;
					try {
						message.sentAt = fmt.parse(msg.get("sentAt").getAsString()).getTime();
					} catch (ParseException e) {
					}
				} else
					message.sentAt = msg.get("a").getAsLong();
				messages.add(message);
			}
			return messages.toArray(t -> new PrivateChatMessage[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void saveDMMessge(String dmID, PrivateChatMessage message) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("messages").getAsJsonArray();
			reader.close();

			// Add message
			JsonObject msg = new JsonObject();
			msg.addProperty("c", message.content);
			msg.addProperty("s", message.source);
			msg.addProperty("a", message.sentAt);
			data.add(msg);

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			dm.addProperty("lastUpdate", System.currentTimeMillis()); // Update
			dm.addProperty("warnedExpiry", false);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			if (activeIDs.contains(dmID))
				activeIDs.remove(dmID);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] getDMParticipants(String dmID) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		ArrayList<String> participants = new ArrayList<String>();
		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("participants").getAsJsonArray();
			reader.close();

			// Add participants
			for (JsonElement ele : data) {
				participants.add(ele.getAsString());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return participants.toArray(t -> new String[t]);
	}

	@Override
	public void deleteDM(String dmID) {
		if (dmExists(dmID))
			new File("dms/" + UUID.fromString(dmID) + ".json").delete();
	}

	@Override
	public void addParticipant(String dmID, String participant) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("participants").getAsJsonArray();
			reader.close();

			// Add participant
			data.add(participant);

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			dm.addProperty("lastUpdate", System.currentTimeMillis()); // Update
			dm.addProperty("warnedExpiry", false);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void removeParticipant(String dmID, String participant) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = dm.get("participants").getAsJsonArray();
			reader.close();

			// Remove participant
			for (JsonElement ele : data) {
				if (ele.getAsString().equals(participant)) {
					data.remove(ele);
					break;
				}
			}

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			dm.addProperty("lastUpdate", System.currentTimeMillis()); // Update
			dm.addProperty("warnedExpiry", false);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void updateDMParticipants(String dmID, String[] participants) {
		if (!dmExists(dmID))
			throw new IllegalArgumentException("DM not found");

		try {
			// Parse DM
			FileReader reader = new FileReader("dms/" + UUID.fromString(dmID) + ".json");
			JsonObject dm = JsonParser.parseReader(reader).getAsJsonObject();
			reader.close();

			// Update participants
			JsonArray participantObjects = new JsonArray();
			for (String p : participants)
				participantObjects.add(p);
			dm.add("participants", participantObjects);

			// Save to disk
			while (activeIDs.contains(dmID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(dmID);
			dm.addProperty("lastUpdate", System.currentTimeMillis()); // Update
			dm.addProperty("warnedExpiry", false);
			Files.writeString(Path.of("dms/" + UUID.fromString(dmID) + ".json"), dm.toString());
			activeIDs.remove(dmID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
