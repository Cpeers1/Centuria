package org.asf.centuria.social.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.asf.centuria.Centuria;
import org.asf.centuria.social.SocialEntry;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileBasedSocialManager extends SocialManager {

	private ArrayList<String> activeIDs = new ArrayList<String>();

	// TODO: Make this to be able to be configured, so larger servers can put files
	// on different drives
	private static String socialListPath = "sociallist";

	// constants for easy coding and best practice.
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
				// construct new social list object
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
				// delete social list object
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
			if (Centuria.debugMode) {
				System.err.println("[FRIENDLIST] ERROR IN RETRIEVING SOCIAL LIST : " + e.getMessage() + " | "
						+ e.getStackTrace() + " )");
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

			// get a list of entries
			for (var ele : socialList.entrySet()) {

				JsonObject entry = ele.getValue().getAsJsonObject();
				SocialEntry socialEntry = new SocialEntry();
				socialEntry.playerID = ele.getKey();
				socialEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
				socialEntry.updatedAt = entry.get(playerEntryUpdatedAtPropertyName).getAsString();
				socialEntry.favorite = entry.get(playerEntryFavouritedPropertyName).getAsBoolean();
				socialEntry.isFollowing = entry.get(playerEntryFollowingPropertyName).getAsBoolean();
				socialEntry.isFollower = entry.get(playerEntryFollowingPropertyName).getAsBoolean();
				socialEntry.isBlocked = entry.get(playerEntryBlockedPropertyName).getAsBoolean();
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

			// get a list of entries
			for (var ele : socialList.entrySet()) {

				// only add if the following property is true
				if (ele.getValue().getAsJsonObject().get(playerEntryFollowingPropertyName).getAsBoolean()) {
					JsonObject entry = ele.getValue().getAsJsonObject();
					SocialEntry socialEntry = new SocialEntry();
					socialEntry.playerID = ele.getKey();
					socialEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
					socialEntry.updatedAt = entry.get(playerEntryUpdatedAtPropertyName).getAsString();
					socialEntry.favorite = entry.get(playerEntryFavouritedPropertyName).getAsBoolean();
					socialEntry.isFollowing = entry.get(playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isFollower = entry.get(playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isBlocked = entry.get(playerEntryBlockedPropertyName).getAsBoolean();
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

			// get a list of entries
			for (var ele : socialList.entrySet()) {

				// only add if the follower property is true
				if (ele.getValue().getAsJsonObject().get(playerEntryFollowerPropertyName).getAsBoolean()) {
					JsonObject entry = ele.getValue().getAsJsonObject();
					SocialEntry socialEntry = new SocialEntry();
					socialEntry.playerID = ele.getKey();
					socialEntry.addedAt = entry.get(playerEntryAddedAtPropertyName).getAsString();
					socialEntry.updatedAt = entry.get(playerEntryUpdatedAtPropertyName).getAsString();
					socialEntry.favorite = entry.get(playerEntryFavouritedPropertyName).getAsBoolean();
					socialEntry.isFollowing = entry.get(playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isFollower = entry.get(playerEntryFollowingPropertyName).getAsBoolean();
					socialEntry.isBlocked = entry.get(playerEntryBlockedPropertyName).getAsBoolean();
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

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(playerEntryFollowingPropertyName);
				playerObject.addProperty(playerEntryFollowingPropertyName, following);

				// update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());

				playerObject.remove(playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(playerEntryUpdatedAtPropertyName, updatedAt);

				// check if the entry is empty
				if (!playerObject.get(playerEntryBlockedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFavouritedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowingPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowerPropertyName).getAsBoolean()) {
					// Delete the entry
					socialList.remove(targetPlayerID);
				}
			} else {
				// create a new entry for this player
				socialList.add(targetPlayerID, createNewPlayerEntry(following, false, false, false));
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

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(playerEntryFollowerPropertyName);
				playerObject.addProperty(playerEntryFollowerPropertyName, follower);

				// update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());

				playerObject.remove(playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(playerEntryUpdatedAtPropertyName, updatedAt);

				// check if the entry is empty
				if (!playerObject.get(playerEntryBlockedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFavouritedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowingPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowerPropertyName).getAsBoolean()) {
					// Delete the entry
					socialList.remove(targetPlayerID);
				}
			} else {
				// create a new entry for this player
				socialList.add(targetPlayerID, createNewPlayerEntry(false, follower, false, false));
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

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(playerEntryBlockedPropertyName);
				playerObject.addProperty(playerEntryBlockedPropertyName, blocked);

				// update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());

				playerObject.remove(playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(playerEntryUpdatedAtPropertyName, updatedAt);

				// check if the entry is empty
				if (!playerObject.get(playerEntryBlockedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFavouritedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowingPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowerPropertyName).getAsBoolean()) {
					// Delete the entry
					socialList.remove(targetPlayerID);
				}
			} else {
				// create a new entry for this player
				socialList.add(targetPlayerID, createNewPlayerEntry(false, false, blocked, false));
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

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// just use the existing element
				JsonObject playerObject = playerElement.getAsJsonObject();
				playerObject.remove(playerEntryFavouritedPropertyName);
				playerObject.addProperty(playerEntryFavouritedPropertyName, favorite);

				// update the updated at too
				SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
				fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				String updatedAt = fmt.format(new Date());

				playerObject.remove(playerEntryUpdatedAtPropertyName);
				playerObject.addProperty(playerEntryUpdatedAtPropertyName, updatedAt);

				// check if the entry is empty
				if (!playerObject.get(playerEntryBlockedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFavouritedPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowingPropertyName).getAsBoolean()
						&& !playerObject.get(playerEntryFollowerPropertyName).getAsBoolean()) {
					// Delete the entry
					socialList.remove(targetPlayerID);
				}
			} else {
				// create a new entry for this player (should never happen for favorite but a
				// fallback)
				socialList.add(targetPlayerID, createNewPlayerEntry(false, false, false, favorite));
			}

			saveToDisk(sourcePlayerID, socialList);
		} catch (IOException e) {
			if (activeIDs.contains(sourcePlayerID))
				activeIDs.remove(sourcePlayerID);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean getPlayerIsFollowing(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// read the property

				return playerElement.getAsJsonObject().get(playerEntryFollowingPropertyName).getAsBoolean();
			} else {
				// auto no
				return false;
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean getPlayerIsFollower(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// read the property

				return playerElement.getAsJsonObject().get(playerEntryFollowerPropertyName).getAsBoolean();
			} else {
				// auto no
				return false;
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean getPlayerIsBlocked(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// read the property
				return playerElement.getAsJsonObject().get(playerEntryBlockedPropertyName).getAsBoolean();
			} else {
				// auto no
				return false;
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean getPlayerIsFavorite(String sourcePlayerID, String targetPlayerID) {
		if (!socialListExists(sourcePlayerID))
			throw new IllegalArgumentException("Friend list not found");

		try {

			// Parse social list
			JsonObject socialList = parseFriendList(sourcePlayerID);

			// does the player exist in the list?
			JsonElement playerElement = socialList.get(targetPlayerID);

			if (playerElement != null) {
				// read the property
				return playerElement.getAsJsonObject().get(playerEntryFavouritedPropertyName).getAsBoolean();
			} else {
				// auto no
				return false;
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private JsonObject parseFriendList(String playerID) throws IOException {
		// Parse friend list
		FileReader reader = new FileReader(socialListPath + "/" + playerID + ".json");
		JsonObject friendList = JsonParser.parseReader(reader).getAsJsonObject();
		reader.close();

		return friendList;
	}

	private void saveToDisk(String sourcePlayerID, JsonObject socialList) throws IOException {
		// Save to disk
		while (activeIDs.contains(sourcePlayerID))
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		activeIDs.add(sourcePlayerID);
		Files.writeString(Path.of(socialListPath + "/" + sourcePlayerID + ".json"), socialList.toString());
		activeIDs.remove(sourcePlayerID);
	}

	private JsonObject createNewPlayerEntry(boolean following, boolean follower, boolean blocked, boolean favorite) {
		JsonObject newEntry = new JsonObject();

		SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		String addedAt = fmt.format(new Date());

		newEntry.addProperty(playerEntryAddedAtPropertyName, addedAt);
		newEntry.addProperty(playerEntryUpdatedAtPropertyName, addedAt);
		newEntry.addProperty(playerEntryFavouritedPropertyName, false);
		newEntry.addProperty(playerEntryFollowingPropertyName, following);
		newEntry.addProperty(playerEntryFollowerPropertyName, follower);
		newEntry.addProperty(playerEntryBlockedPropertyName, blocked);

		return newEntry;
	}

}
