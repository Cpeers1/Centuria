package org.asf.emuferal.friendlist.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import javax.print.attribute.standard.DateTimeAtCompleted;

import org.asf.emuferal.friendlist.FriendListManager;
import org.asf.emuferal.friendlist.FriendListEntry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileBasedFriendListManager extends FriendListManager {

	private ArrayList<String> activeIDs = new ArrayList<String>();
	
	//TODO: Make this to be able to be configured, so larger servers can put files on different drives
	private static String friendListPath = "friendlists";
	
	//constants for easy coding and best practice.
	private static String followingListPropertyName = "following";
	private static String followerListPropertyName = "followers";
	private static String blockedListPropertyName = "blocked";
	
	private static String playerEntryIdPropertyName = "playerID";
	private static String playerEntryAddedAtPropertyName = "addedAt";

	@Override
	public void openFriendList(String playerID) {
		try {
			if (!new File(friendListPath).exists())
				new File(friendListPath).mkdirs();
			
			if (!friendListExists(playerID)) {
				//construct new friend list object
				JsonObject friendList = new JsonObject();
				
				friendList.add(followingListPropertyName, new JsonArray());
				friendList.add(followerListPropertyName, new JsonArray());
				friendList.add(blockedListPropertyName, new JsonArray());
				
				Files.writeString(Path.of(friendListPath + "/" + playerID + ".json"), friendList.toString());
			}
		} catch (Exception e) {
		}
	}

	@Override
	public boolean friendListExists(String playerID) {
		try {
			return new File(friendListPath + "/" + playerID + ".json").exists();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public FriendListEntry[] getFollowingList(String playerID) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse following list
			FileReader reader = new FileReader(friendListPath + "/" + playerID + ".json");
			JsonObject friendsList = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = friendsList.get(followingListPropertyName).getAsJsonArray();
			reader.close();

			ArrayList<FriendListEntry> followingEntries = new ArrayList<FriendListEntry>();
			for (JsonElement ele : data) {
				JsonObject entry = ele.getAsJsonObject();
				FriendListEntry followingEntry = new FriendListEntry();
				followingEntry.playerID = entry.get(playerEntryIdPropertyName).getAsString();
				followingEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
				followingEntries.add(followingEntry);
			}
			return followingEntries.toArray(t -> new FriendListEntry[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public FriendListEntry[] getFollowerList(String playerID) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse following list
			FileReader reader = new FileReader(friendListPath + "/" + playerID + ".json");
			JsonObject friendsList = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = friendsList.get(followerListPropertyName).getAsJsonArray();
			reader.close();

			ArrayList<FriendListEntry> followerEntries = new ArrayList<FriendListEntry>();
			for (JsonElement ele : data) {
				JsonObject entry = ele.getAsJsonObject();
				FriendListEntry followerEntry = new FriendListEntry();
				followerEntry.playerID = entry.get(playerEntryIdPropertyName).getAsString();
				followerEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
				followerEntries.add(followerEntry);
			}
			return followerEntries.toArray(t -> new FriendListEntry[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public FriendListEntry[] getBlockedList(String playerID) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse following list
			FileReader reader = new FileReader(friendListPath + "/" + playerID + ".json");
			JsonObject friendsList = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = friendsList.get(blockedListPropertyName).getAsJsonArray();
			reader.close();

			ArrayList<FriendListEntry> blockedEntries = new ArrayList<FriendListEntry>();
			for (JsonElement ele : data) {
				JsonObject entry = ele.getAsJsonObject();
				FriendListEntry blockedEntry = new FriendListEntry();
				blockedEntry.playerID = entry.get(playerEntryIdPropertyName).getAsString();
				blockedEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
				blockedEntries.add(blockedEntry);
			}
			return blockedEntries.toArray(t -> new FriendListEntry[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addFollowingPlayer(String playerID, FriendListEntry playerToAdd) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse DM
			FileReader reader = new FileReader(friendListPath + "/" + playerID + ".json");
			JsonObject friendList = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = friendList.get(followingListPropertyName).getAsJsonArray();
			reader.close();
			
			// Add player into follow list
			JsonObject newEntry = new JsonObject();
			newEntry.addProperty(playerEntryIdPropertyName, playerToAdd.playerID);
			newEntry.addProperty(playerEntryAddedAtPropertyName, playerToAdd.addedAt);
			data.add(newEntry);

			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(playerID);
			Files.writeString(Path.of("friendListPath/" + playerID + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void addFollowerPlayer(String playerID, FriendListEntry playerToAdd) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse DM
			FileReader reader = new FileReader(friendListPath + "/" + playerID + ".json");
			JsonObject friendList = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = friendList.get(followerListPropertyName).getAsJsonArray();
			reader.close();
			
			// Add player into follow list
			JsonObject newEntry = new JsonObject();
			newEntry.addProperty(playerEntryIdPropertyName, playerToAdd.playerID);
			newEntry.addProperty(playerEntryAddedAtPropertyName, playerToAdd.addedAt);
			data.add(newEntry);

			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(playerID);
			Files.writeString(Path.of("friendListPath/" + playerID + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public void addBlockedPlayer(String playerID, FriendListEntry playerToAdd) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse DM
			FileReader reader = new FileReader(friendListPath + "/" + playerID + ".json");
			JsonObject friendList = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray data = friendList.get(blockedListPropertyName).getAsJsonArray();
			reader.close();
			
			// Add player into follow list
			JsonObject newEntry = new JsonObject();
			newEntry.addProperty(playerEntryIdPropertyName, playerToAdd.playerID);
			newEntry.addProperty(playerEntryAddedAtPropertyName, playerToAdd.addedAt);
			data.add(newEntry);

			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(playerID);
			Files.writeString(Path.of("friendListPath/" + UUID.fromString(playerID) + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}
	}

}
