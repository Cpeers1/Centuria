package org.asf.centuria.networking.chatserver.networking;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.asf.centuria.dms.DMManager;
import org.asf.centuria.dms.PrivateChatMessage;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class HistoryPacket extends AbstractChatPacket {

	public String convo;
	public int cursor;
	public int pageSize;
	public boolean includeMessages;

	@Override
	public String id() {
		return "conversations.history";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new HistoryPacket();
	}

	@Override
	public void parse(JsonObject data) {
		convo = data.get("conversationId").getAsString();
		pageSize = data.get("pageSize").getAsInt();
		if (data.has("cursor") && !data.get("cursor").getAsString().isEmpty())
			cursor = data.get("cursor").getAsInt();
		includeMessages = data.get("include_messages").getAsBoolean();
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {
		// Send response
		JsonObject res = new JsonObject();

		// Time format
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Load messages
		int cursorCurrent = cursor;
		int messageOffset = cursorCurrent * pageSize;
		int dmHistorySize = 0;
		DMManager manager = DMManager.getInstance();
		if (client.isInRoom(convo) && client.getRoom(convo).getType().equalsIgnoreCase(ChatRoomTypes.PRIVATE_CHAT)
				&& manager.dmExists(convo)) {
			if (includeMessages) {
				JsonArray msgs = new JsonArray();
				int indexInPage = 0;
				PrivateChatMessage[] messages = manager.getDMHistory(convo, client.getPlayer().getAccountID());
				dmHistorySize = messages.length;
				for (int i = messages.length - 1 - messageOffset; i >= 0; i--) {
					PrivateChatMessage msg = messages[i];

					// Build message object
					JsonObject obj = new JsonObject();
					obj.addProperty("body", msg.content);
					obj.addProperty("conversation_id", convo);
					obj.addProperty("conversation_type", "private");
					obj.add("mask", null);
					try {
						obj.addProperty("message_id",
								UUID.nameUUIDFromBytes((fmt.format(new Date(msg.sentAt)) + convo + msg.content).getBytes("UTF-8")).toString());
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					obj.addProperty("sent_at", fmt.format(new Date(msg.sentAt)));
					obj.addProperty("source", msg.source);
					msgs.add(obj);

					// Increase index
					indexInPage++;
					if (indexInPage >= pageSize)
						break;
				}
				res.add("messages", msgs);
			} else
				dmHistorySize = manager.getDMHistory(convo, client.getPlayer().getAccountID()).length;
		} else if (includeMessages)
			res.add("messages", new JsonArray());

		res.addProperty("eventId", "conversations.history");
		if ((cursorCurrent + 1) * pageSize < dmHistorySize)
			res.addProperty("cursor", cursorCurrent + 1);
		res.addProperty("success", true);
		client.sendPacket(res);
		return true;
	}

}
