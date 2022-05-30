package org.asf.emuferal.social.impl;

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

import org.asf.emuferal.social.SocialEntry;
import org.asf.emuferal.social.SocialManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileBasedSocialManager extends SocialManager {

	private ArrayList<String> activeIDs = new ArrayList<String>();
	
	//TODO: Make this to be able to be configured, so larger servers can put files on different drives
	private static String socialListPath = "sociallist";
	
	//constants for easy coding and best practice.
	private static String playerEntryAddedAtPropertyName = "addedAt";
	private static String playerEntryUpdatedAtPropertyName = "addedAt";
	private static String playerEntryFavouritedPropertyName = "favourite";
	private static String playerEntryBlockedPropertyName = "isBlocked";
	private static String playerEntryFollowingPropertyName = "isFollowing";
	private static String playerEntryFollowerPropertyName = "isFollower";


	@Override
	public void openSocialList(String playerID) {
		try {
			if (!new File(socialListPath).exists())
				new File(socialListPath).mkdirs();
			
			if (!socialListExists(playerID)) {
				//construct new social list object
				JsonObject friendList = new JsonObject();
				
				Files.writeString(Path.of(socialListPath + "/" + playerID + ".json"), friendList.toString());
			}
		} catch (Exception e) {
		}
	}
	

	@Override
	public void deleteSocialList(String playerID) {
		try {
			if (!new File(socialListPath).exists())
				new File(socialListPath).mkdirs();
			
			if (socialListExists(playerID)) {
				//delete social list object
				new File(socialListPath).delete();
			}
		} catch (Exception e) {
		}
	}

	@Override
	public boolean socialListExists(String playerID) {
		try {
			return new File(socialListPath + "/" + playerID + ".json").exists();
		} catch (Exception e) {
			if (System.getProperty("debugMode") != null) {
				System.err.println("[FRIENDLIST] ERROR IN RETRIEVING SOCIAL LIST : " + e.getMessage() + " | " + e.getStackTrace() + " )");
			}
			return false;
		}
	}

	@Override
	public SocialEntry[] getSocialList(String playerID) {
		if (!socialListExists(playerID))
			throw new IllegalArgumentException("Social list not found");

		try {
			// Parse following list
			JsonObject socialList = parseFriendList(playerID);

			ArrayList<SocialEntry> socialEntries = new ArrayList<SocialEntry>();
			
			//get a list of entries
			for (var ele : socialList.entrySet()) {
				
				JsonObject entry = ele.getValue().getAsJsonObject();
				SocialEntry socialEntry = new SocialEntry();
				socialEntry.playerID = ele.getKey();
				socialEntry.addedAt = entry.get(this.playerEntryAddedAtPropertyName).getAsString();
				socialEntry.updatedAt = entry.get(this.playerEntryUpdatedAtPropertyName).getAsString();
				socialEntry.favorite = entry.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
				socialEntry.isFollowing = entry.get(this.playerEntryFollowingPropertyName).getAsBoolean();
				socialEntry.isFollower = entry.get(this.playerEntryFollowingPropertyName).getAsBoolean();
				socialEntry.isBlocked = entry.get(this.playerEntryBlockedPropertyName).getAsBoolean();
				socialEntries.add(socialEntry);
			}
			
			return socialEntries.toArray(t -> new SocialEntry[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public SocialEntry[] getFollowingPlayers(String playerID) {
		if (!socialListExists(playerID))
			throw new IllegalArgumentException("Social list not found");

		try {
			// Parse following list
			JsonObject socialList = parseFriendList(playerID);

			ArrayList<SocialEntry> socialEntries = new ArrayList<SocialEntry>();
			
			//get a list of entries
			for (var ele : socialList.entrySet()) {
				
				//only add if the following property is true
				if(ele.getValue().getAsJsonObject().get(this.playerEntryFollowingPropertyName).getAsBoolean()) {
					JsonObject entry = ele.getValue().getAsJsonObject();
					SocialEntry socialEntry = new SocialEntry();
					socialEntry.playerID = ele.getKey();
					socialEntry.addedAt = entry.get(this.playerEntryAddedAtPropertyName).getAsString();
					socialEntry.updatedAt = entry.get(this.playerEntryUpdatedAtPropertyName).getAsString();
					socialEntry.favorite = entry.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
					socialEntry.isFollowing = entry.get(this.playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isFollower = entry.get(this.playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isBlocked = entry.get(this.playerEntryBlockedPropertyName).getAsBoolean();
					socialEntries.add(socialEntry);
				}

			}
			
			return socialEntries.toArray(t -> new SocialEntry[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public SocialEntry[] getFollowerPlayers(String playerID) {
		if (!socialListExists(playerID))
			throw new IllegalArgumentException("Social list not found");

		try {
			// Parse following list
			JsonObject socialList = parseFriendList(playerID);

			ArrayList<SocialEntry> socialEntries = new ArrayList<SocialEntry>();
			
			//get a list of entries
			for (var ele : socialList.entrySet()) {
				
				//only add if the follower property is true
				if(ele.getValue().getAsJsonObject().get(this.playerEntryFollowerPropertyName).getAsBoolean()) {
					JsonObject entry = ele.getValue().getAsJsonObject();
					SocialEntry socialEntry = new SocialEntry();
					socialEntry.playerID = ele.getKey();
					socialEntry.addedAt = entry.get(this.playerEntryAddedAtPropertyName).getAsString();
					socialEntry.updatedAt = entry.get(this.playerEntryUpdatedAtPropertyName).getAsString();
					socialEntry.favorite = entry.get(this.playerEntryFavouritedPropertyName).getAsBoolean();
					socialEntry.isFollowing = entry.get(this.playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isFollower = entry.get(this.playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isBlocked = entry.get(this.playerEntryBlockedPropertyName).getAsBoolean();
					socialEntries.add(socialEntry);
				}
			}
			
			return socialEntries.toArray(t -> new SocialEntry[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setFollowingPlayer(String sourcePlayerID, String targetPlayerID, boolean following) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Social list not found");

		try {
			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(this.playerEntryFollowingPropertyName);
				playerObject.addProperty(this.playerEntryFollowingPropertyName, following);
				
				//update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());
				
				playerObject.remove(this.playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(this.playerEntryUpdatedAtPropertyName, updatedAt);
			}
			else
			{
				//create a new entry for this player
				socialList.add(targetPlayerID, this.createNewPlayerEntry(following, false, false, false));
			}
			 			
			saveToDisk(sourcePlayerID, socialList);
		} catch (IOException e) {
			if (activeIDs.contains(sourcePlayerID))
				activeIDs.remove(sourcePlayerID);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void setFollowerPlayer(String sourcePlayerID, String targetPlayerID, boolean follower) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Social list not found");

		try {
			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(this.playerEntryFollowerPropertyName);
				playerObject.addProperty(this.playerEntryFollowerPropertyName, follower);
				
				//update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());
				
				playerObject.remove(this.playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(this.playerEntryUpdatedAtPropertyName, updatedAt);
			}
			else
			{
				//create a new entry for this player
				socialList.add(targetPlayerID, this.createNewPlayerEntry(false, follower, false, false));
			}
			 			
			saveToDisk(sourcePlayerID, socialList);
		} catch (IOException e) {
			if (activeIDs.contains(sourcePlayerID))
				activeIDs.remove(sourcePlayerID);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setBlockedPlayer(String sourcePlayerID, String targetPlayerID, boolean blocked) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Social list not found");

		try {
			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(this.playerEntryBlockedPropertyName);
				playerObject.addProperty(this.playerEntryBlockedPropertyName, blocked);
				
				//update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());
				
				playerObject.remove(this.playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(this.playerEntryUpdatedAtPropertyName, updatedAt);
			}
			else
			{
				//create a new entry for this player
				socialList.add(targetPlayerID, this.createNewPlayerEntry(false, false, blocked, false));
			}
			 			
			saveToDisk(sourcePlayerID, socialList);
		} catch (IOException e) {
			if (activeIDs.contains(sourcePlayerID))
				activeIDs.remove(sourcePlayerID);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void setFavoritePlayer(String sourcePlayerID, String targetPlayerID, boolean favorite) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Social list not found");

		try {
			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(this.playerEntryFavouritedPropertyName);
				playerObject.addProperty(this.playerEntryFavouritedPropertyName, favorite);
				
				//update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());
				
				playerObject.remove(this.playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(this.playerEntryUpdatedAtPropertyName, updatedAt);
			}
			else
			{
				//create a new entry for this player (should never happen for favorite but a fallback)
				socialList.add(targetPlayerID, this.createNewPlayerEntry(false, false, false, favorite));
			}
			 			
			saveToDisk(sourcePlayerID, socialList);
		} catch (IOException e) {
			if (activeIDs.contains(sourcePlayerID))
				activeIDs.remove(sourcePlayerID);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Boolean getPlayerIsFollowing(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//read the property
				
				return playerElement.getAsJsonObject().get(this.playerEntryFollowingPropertyName).getAsBoolean();
			}
			else
			{
				//auto no
				return false;
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public Boolean getPlayerIsFollower(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//read the property
				
				return playerElement.getAsJsonObject().get(this.playerEntryFollowerPropertyName).getAsBoolean();
			}
			else
			{
				//auto no
				return false;
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Boolean getPlayerIsBlocked(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//read the property	
				return playerElement.getAsJsonObject().get(this.playerEntryBlockedPropertyName).getAsBoolean();
			}
			else
			{
				//auto no
				return false;
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Boolean getPlayerIsFavorite(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);
			
			//does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);
			
			if(playerElement != null)
			{
				//read the property	
				return playerElement.getAsJsonObject().get(this.playerEntryFavouritedPropertyName).getAsBoolean();
			}
			else
			{
				//auto no
				return false;
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private JsonObject parseFriendList(String playerID) throws IOException
	{
		// Parse friend list
		FileReader reader = new FileReader(socialListPath + "/" + playerID + ".json");
		JsonObject friendList = JsonParser.parseReader(reader).getAsJsonObject();
		reader.close();
		
		return friendList;
	}
	
	private void saveToDisk(String sourcePlayerID, JsonObject socialList) throws IOException
	{
		// Save to disk
		while (activeIDs.contains(sourcePlayerID))
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		activeIDs.add(sourcePlayerID);
		Files.writeString(Path.of(this.socialListPath + "/" + sourcePlayerID + ".json"), socialList.toString());
		activeIDs.remove(sourcePlayerID);
	}
	
	private JsonObject createNewPlayerEntry(boolean following, boolean follower, boolean blocked, boolean favorite)
	{
		JsonObject newEntry = new JsonObject();
		
		SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		String addedAt = fmt.format(new Date());
		
		newEntry.addProperty(this.playerEntryAddedAtPropertyName, addedAt);
		newEntry.addProperty(this.playerEntryUpdatedAtPropertyName, addedAt);
		newEntry.addProperty(this.playerEntryFavouritedPropertyName, false);
		newEntry.addProperty(this.playerEntryFollowingPropertyName, following);
		newEntry.addProperty(this.playerEntryFollowerPropertyName, follower);
		newEntry.addProperty(this.playerEntryBlockedPropertyName, blocked);
		
		return newEntry;
	}

}
