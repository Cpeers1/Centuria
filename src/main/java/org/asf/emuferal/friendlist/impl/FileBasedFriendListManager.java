package org.asf.emuferal.friendlist.impl;

import java.io.File;
import java.io.FileNotFoundException;
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
	private static String playerEntryFavouritedPropertyName = "favourite";

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
			if (System.getProperty("debugMode") != null) {
				System.err.println("[FRIENDLIST] ERROR IN RETRIEVING FRIEND LIST : " + e.getMessage() + " | " + e.getStackTrace() + " )");
			}
			return false;
		}
	}

	@Override
	public FriendListEntry[] getFollowingList(String playerID) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse following list
			JsonObject friendsList = parseFriendList(playerID);
			JsonArray data = friendsList.get(followingListPropertyName).getAsJsonArray();


			ArrayList<FriendListEntry> followingEntries = new ArrayList<FriendListEntry>();
			for (JsonElement ele : data) {
				JsonObject entry = ele.getAsJsonObject();
				FriendListEntry followingEntry = new FriendListEntry();
				followingEntry.playerID = entry.get(playerEntryIdPropertyName).getAsString();
				followingEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
				followingEntry.favorite = entry.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
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
			JsonObject friendsList = parseFriendList(playerID);
			JsonArray data = friendsList.get(followerListPropertyName).getAsJsonArray();

			ArrayList<FriendListEntry> followerEntries = new ArrayList<FriendListEntry>();
			for (JsonElement ele : data) {
				JsonObject entry = ele.getAsJsonObject();
				FriendListEntry followerEntry = new FriendListEntry();
				followerEntry.playerID = entry.get(playerEntryIdPropertyName).getAsString();
				followerEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
				followerEntry.favorite = entry.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
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
			JsonObject friendsList = parseFriendList(playerID);
			JsonArray data = friendsList.get(blockedListPropertyName).getAsJsonArray();

			ArrayList<FriendListEntry> blockedEntries = new ArrayList<FriendListEntry>();
			for (JsonElement ele : data) {
				JsonObject entry = ele.getAsJsonObject();
				FriendListEntry blockedEntry = new FriendListEntry();
				blockedEntry.playerID = entry.get(playerEntryIdPropertyName).getAsString();
				blockedEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
				blockedEntry.favorite = entry.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
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
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(followingListPropertyName).getAsJsonArray();
			
			// Add player into follow list
			JsonObject newEntry = new JsonObject();
			newEntry.addProperty(playerEntryIdPropertyName, playerToAdd.playerID);
			newEntry.addProperty(playerEntryAddedAtPropertyName, playerToAdd.addedAt);
			newEntry.addProperty(this.playerEntryFavouritedPropertyName, false);
			data.add(newEntry);

			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + playerID + ".json"), friendList.toString());
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
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(followerListPropertyName).getAsJsonArray();
			
			// Add player into follow list
			JsonObject newEntry = new JsonObject();
			newEntry.addProperty(playerEntryIdPropertyName, playerToAdd.playerID);
			newEntry.addProperty(playerEntryAddedAtPropertyName, playerToAdd.addedAt);
			newEntry.addProperty(this.playerEntryFavouritedPropertyName, false);
			data.add(newEntry);

			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + playerID + ".json"), friendList.toString());
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
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(blockedListPropertyName).getAsJsonArray();

			// Add player into follow list
			JsonObject newEntry = new JsonObject();
			newEntry.addProperty(playerEntryIdPropertyName, playerToAdd.playerID);
			newEntry.addProperty(playerEntryAddedAtPropertyName, playerToAdd.addedAt);
			newEntry.addProperty(this.playerEntryFavouritedPropertyName, false);
			data.add(newEntry);

			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + UUID.fromString(playerID) + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Boolean getPlayerIsFollowing(String playerID, String playerIDToCheck) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(this.followingListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			
			Boolean match = false;
			for(JsonElement entry : data)
			{
				if(entry.getAsJsonObject().get(this.playerEntryIdPropertyName).getAsString().equals(playerIDToCheck))
				{
					match = true;
					break; 
				}
			}
			
			return match;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Boolean getPlayerIsFollower(String playerID, String playerIDToCheck) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(this.followerListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			
			Boolean match = false;
			for(JsonElement entry : data)
			{
				if(entry.getAsJsonObject().get(this.playerEntryIdPropertyName).getAsString().equals(playerIDToCheck))
				{
					match = true;
					break; 
				}
			}
			
			return match;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Boolean getPlayerIsBlocked(String playerID, String playerIDToCheck) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(blockedListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			
			Boolean match = false;
			for(JsonElement entry : data)
			{
				if(entry.getAsJsonObject().get(this.playerEntryIdPropertyName).getAsString().equals(playerIDToCheck))
				{
					match = true;
					break; 
				}
			}
			
			return match;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void removeFollowingPlayer(String playerID, String playerIDToRemove) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(this.followingListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			Boolean match = false;
			JsonElement foundEntry = null;
			for(JsonElement entry : data)
			{
				var entryAsJsonObject = entry.getAsJsonObject();
				if(entryAsJsonObject.get(this.playerEntryIdPropertyName).getAsString().equals(playerIDToRemove))
				{
					match = true;
					foundEntry = entry;
					break; 
				}
			}	
			//remove the entry..
			
			data.remove(foundEntry);
			
			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + UUID.fromString(playerID) + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}	
	}

	@Override
	public void removeFollowerPlayer(String playerID, String playerIDToRemove) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(this.followerListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			Boolean match = false;
			JsonElement foundEntry = null;
			for(JsonElement entry : data)
			{
				var entryAsJsonObject = entry.getAsJsonObject();
				if(entryAsJsonObject.get(this.playerEntryIdPropertyName).getAsString().equals(playerIDToRemove))
				{
					match = true;
					foundEntry = entry;
					break; 
				}
			}
			
			if(!match) return;
			
			//remove the entry..
			
			data.remove(foundEntry);
			
			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + UUID.fromString(playerID) + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}	
	}

	@Override
	public void removeBlockedPlayer(String playerID, String playerIDToRemove) {
		// TODO Auto-generated method stub
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(this.followerListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			Boolean match = false;
			JsonElement foundEntry = null;
			for(JsonElement entry : data)
			{
				var entryAsJsonObject = entry.getAsJsonObject();
				if(entryAsJsonObject.get(this.playerEntryIdPropertyName).getAsString().equals(playerIDToRemove))
				{
					match = true;
					foundEntry = entry;
					break; 
				}
			}
			
			if(!match) return;
			
			//remove the entry..
			
			data.remove(foundEntry);
			
			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + UUID.fromString(playerID) + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}	
	}

	@Override
	public void toggleFollowingPlayerAsFavorite(String playerID, String targetPlayerID) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(this.followingListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			Boolean match = false;
			JsonElement foundEntry = null;
			for(JsonElement entry : data)
			{
				var entryAsJsonObject = entry.getAsJsonObject();
				if(entryAsJsonObject.get(this.playerEntryIdPropertyName).getAsString().equals(targetPlayerID))
				{
					match = true;
					foundEntry = entry;
					break; 
				}
			}
			
			if(!match) return;
			
			//what's the current favorite status? invert it
			var foundEntryJsonObject = foundEntry.getAsJsonObject();
			Boolean favoriteStatus = foundEntryJsonObject.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
			foundEntryJsonObject.remove(this.playerEntryFavouritedPropertyName);  
			foundEntryJsonObject.addProperty(this.playerEntryFavouritedPropertyName, !favoriteStatus);
			
			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + UUID.fromString(playerID) + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}	
	}

	@Override
	public void toggleFollowerPlayerAsFavorite(String playerID, String targetPlayerID) {
		if (!friendListExists(playerID))
			throw new IllegalArgumentException("Friend list not found");

		try {
			// Parse friend list
			JsonObject friendList = parseFriendList(playerID);
			JsonArray data = friendList.get(this.followerListPropertyName).getAsJsonArray();

			// Check for entries with the player ID..
			Boolean match = false;
			JsonElement foundEntry = null;
			for(JsonElement entry : data)
			{
				var entryAsJsonObject = entry.getAsJsonObject();
				if(entryAsJsonObject.get(this.playerEntryIdPropertyName).getAsString().equals(targetPlayerID))
				{
					match = true;
					foundEntry = entry;
					break; 
				}
			}
			
			if(!match) return;
			
			//what's the current favorite status? invert it
			var foundEntryJsonObject = foundEntry.getAsJsonObject();
			Boolean favoriteStatus = foundEntryJsonObject.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
			foundEntryJsonObject.remove(this.playerEntryFavouritedPropertyName);  
			foundEntryJsonObject.addProperty(this.playerEntryFavouritedPropertyName, !favoriteStatus);
			
			// Save to disk
			while (activeIDs.contains(playerID))
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			
			activeIDs.add(playerID);
			Files.writeString(Path.of(this.friendListPath + "/" + UUID.fromString(playerID) + ".json"), friendList.toString());
			activeIDs.remove(playerID);
		} catch (IOException e) {
			if (activeIDs.contains(playerID))
				activeIDs.remove(playerID);
			throw new RuntimeException(e);
		}			
	}
	
	private JsonObject parseFriendList(String playerID) throws IOException
	{
		// Parse friend list
		FileReader reader = new FileReader(friendListPath + "/" + playerID + ".json");
		JsonObject friendList = JsonParser.parseReader(reader).getAsJsonObject();
		reader.close();
		
		return friendList;
	}

}
