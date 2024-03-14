package org.asf.centuria.rooms.privateinstances.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.generic.Quaternion;
import org.asf.centuria.entities.generic.Vector3;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.rooms.GameRoom;
import org.asf.centuria.rooms.GameRoomManager;
import org.asf.centuria.rooms.privateinstances.PrivateInstance;
import org.asf.centuria.rooms.privateinstances.containervars.PrivateInstanceContainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FileBasedPrivateInstance implements PrivateInstance {

	private FileBasedPrivateInstanceManagerImpl privInstManager;
	private GameRoomManager roomManager;

	private HashMap<Integer, GameRoom> roomsByLevelID = new HashMap<Integer, GameRoom>();
	private File sourceFile;
	private JsonObject data;

	private String id;
	private String name;
	private String description;
	private String owner;
	private boolean allowInvites;

	private ArrayList<String> participants = new ArrayList<String>();

	public FileBasedPrivateInstance(FileBasedPrivateInstanceManagerImpl privInstManager, GameRoomManager roomManager,
			File sourceFile, String id, JsonObject data) {
		this.privInstManager = privInstManager;
		this.roomManager = roomManager;

		// Core fields
		this.sourceFile = sourceFile;
		this.data = data;

		// Details
		this.id = id;
		this.name = data.get("name").getAsString();
		this.description = data.get("description").getAsString();
		this.owner = data.get("owner").getAsString();
		this.allowInvites = data.get("allowInvites").getAsBoolean();

		// Populate participant list
		JsonArray participants = data.get("participants").getAsJsonArray();
		for (JsonElement part : participants)
			this.participants.add(part.getAsString());
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getOwnerID() {
		return owner;
	}

	@Override
	public String[] getParticipants() {
		return participants.toArray(t -> new String[t]);
	}

	@Override
	public boolean isParticipant(String participantID) {
		return participants.contains(participantID);
	}

	@Override
	public void setName(String name) {
		try {
			// Save
			String oName = this.name;
			this.name = name;
			data.addProperty("name", name);
			Files.writeString(sourceFile.toPath(), data.toString());

			// Log
			Centuria.logger.info("Updated name of private instance " + id + " (old name: " + oName + ") to: " + name);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setDescription(String description) {
		try {
			// Save
			String oDesc = this.description;
			this.description = description;
			data.addProperty("description", description);
			Files.writeString(sourceFile.toPath(), data.toString());

			// Log
			Centuria.logger.info("Updated description of private instance " + id + " (name: " + name + ") to:\n"
					+ description + "\n\nPrevious description:\n" + oDesc);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setOwnerID(String ownerID) {
		try {
			// Save
			String old = this.owner;
			this.owner = ownerID;
			data.addProperty("owner", ownerID);
			Files.writeString(sourceFile.toPath(), data.toString());

			// Log
			CenturiaAccount a = AccountManager.getInstance().getAccount(ownerID);
			CenturiaAccount o = AccountManager.getInstance().getAccount(old);
			if (a != null)
				Centuria.logger.info("Made user " + a.getAccountID() + " owner of private instance " + id + " (name: "
						+ name + "), previous owner was " + (o == null ? " ID " + old : o.getDisplayName()));
			else
				Centuria.logger.info("Made user ID " + ownerID + " owner of private instance " + id + " (name: " + name
						+ "), previous owner was " + (o == null ? " ID " + old : o.getDisplayName()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setParticipants(String[] participants) {
		// Gather changes
		ArrayList<String> oldParticipants = new ArrayList<String>(Arrays.asList(getParticipants()));
		ArrayList<String> newParticipants = new ArrayList<String>(Arrays.asList(participants));
		ArrayList<CenturiaAccount> addedParticipants = new ArrayList<CenturiaAccount>();
		ArrayList<CenturiaAccount> removedParticipants = new ArrayList<CenturiaAccount>();
		AccountManager accs = AccountManager.getInstance();
		for (String old : oldParticipants) {
			if (!newParticipants.contains(old)) {
				CenturiaAccount a = accs.getAccount(old);
				if (a != null)
					addedParticipants.add(a);
				else
					this.participants.remove(old);
			}
		}
		for (String nw : newParticipants) {
			if (!oldParticipants.contains(nw)) {
				CenturiaAccount a = accs.getAccount(nw);
				if (a != null)
					removedParticipants.add(a);
				else
					this.participants.remove(nw);
			}
		}

		// Check result size
		if (newParticipants.size() == 0) {
			// Delete
			delete();
			return;
		}

		// Update list
		// Remove users that left
		String msg = "";
		for (CenturiaAccount acc : removedParticipants) {
			if (!msg.isEmpty())
				msg += ", ";
			msg += acc.getDisplayName();
			userLeft(acc);
		}
		if (!msg.isBlank())
			Centuria.logger.info("User" + (removedParticipants.size() != 1 ? "s" : "") + " " + msg
					+ " was removed from private instance " + id + " (name: " + name + ")");

		// Add users that joined
		msg = "";
		for (CenturiaAccount acc : addedParticipants) {
			if (!msg.isEmpty())
				msg += ", ";
			msg += acc.getDisplayName();
			userJoined(acc);
		}
		if (!msg.isBlank())
			Centuria.logger.info("User" + (addedParticipants.size() != 1 ? "s" : "") + " " + msg
					+ " joined private instance " + id + " (name: " + name + ")");

		try {
			// Save
			JsonArray parts = new JsonArray();
			for (String id : this.participants)
				parts.add(id);
			data.add("participants", parts);
			Files.writeString(sourceFile.toPath(), data.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Check owner leave
		if (!newParticipants.contains(owner)) {
			// Update owner
			setOwnerID(newParticipants.get(0));
		}
	}

	protected void userJoined(CenturiaAccount acc) {
		// Add to list
		participants.add(acc.getAccountID());

		// Add to user storage
		JsonObject instancesLst;
		if (acc.getSaveSharedInventory().containsItem("privateinstances"))
			instancesLst = acc.getSaveSharedInventory().getItem("privateinstances").getAsJsonObject();
		else {
			instancesLst = new JsonObject();
			instancesLst.add("instances", new JsonArray());
		}

		// Get instance list
		JsonArray insts = instancesLst.get("instances").getAsJsonArray();

		// Find
		boolean found = false;
		for (JsonElement ele : insts) {
			if (ele.getAsString().equals(id)) {
				found = true;
				break;
			}
		}
		if (!found)
			insts.add(id);

		// Save
		acc.getSaveSharedInventory().setItem("privateinstances", instancesLst);
	}

	protected void userLeft(CenturiaAccount acc) {
		// Remove from list
		participants.remove(acc.getAccountID());

		// Remove to user storage
		JsonObject instancesLst;
		if (acc.getSaveSharedInventory().containsItem("privateinstances"))
			instancesLst = acc.getSaveSharedInventory().getItem("privateinstances").getAsJsonObject();
		else {
			instancesLst = new JsonObject();
			instancesLst.add("instances", new JsonArray());
		}

		// Get instance list
		JsonArray insts = instancesLst.get("instances").getAsJsonArray();

		// Find
		for (JsonElement ele : insts) {
			if (ele.getAsString().equals(id)) {
				// Remove
				insts.remove(ele);
				break;
			}
		}

		// If needed unset selected
		if (instancesLst.has("selected") && instancesLst.get("selected").getAsString().equals(id)) {
			// Remove
			instancesLst.remove("selected");
		}

		// Save
		acc.getSaveSharedInventory().setItem("privateinstances", instancesLst);

		// Remove from room if in it
		Player plr = acc.getOnlinePlayerInstance();
		if (plr != null) {
			GameRoom room = plr.getRoom();
			boolean isPendingRoom = false;
			if ((room == null && plr.room == null && plr.pendingRoom != null)
					|| (plr.room != null && plr.pendingRoom != null && !plr.room.equals(plr.pendingRoom))) {
				// Use pending room
				isPendingRoom = true;
				room = roomManager.getRoom(plr.pendingRoom);
			}
			if (room != null) {
				// Check if present
				boolean remove = false;
				synchronized (roomsByLevelID) {
					if (roomsByLevelID.containsKey(room.getLevelID())) {
						// Check ID
						if (roomsByLevelID.get(room.getLevelID()).getInstanceID().equals(room.getInstanceID())) {
							// Remove player from room
							remove = true;
						}
					}
				}
				if (remove) {
					// Check
					if (isPendingRoom && plr.room != null)
						isPendingRoom = false;

					// Remove from room
					if (!isPendingRoom) {
						// Remove now
						String roomID = roomManager.findBestRoom(plr.levelID, plr).getID();
						plr.targetPos = new Vector3(plr.lastPos.x, plr.lastPos.y, plr.lastPos.z);
						plr.targetRot = new Quaternion(plr.lastRot.x, plr.lastRot.y, plr.lastRot.z, plr.lastRot.w);
						plr.teleportToRoom(plr.levelID, plr.levelType, 0, roomID, "");
					} else {
						// Update pending room
						String pending = plr.pendingRoom;
						String newRoom = roomManager.findBestRoom(plr.pendingLevelID, plr).getID();
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
			}
		}
	}

	@Override
	public GameRoom getRoom(int levelID) {
		synchronized (roomsByLevelID) {
			if (!roomsByLevelID.containsKey(levelID)) {
				GameRoom room = roomManager.createRoom(levelID, "PRIV-", "");
				PrivateInstanceContainer cont = new PrivateInstanceContainer();
				cont.instance = this;
				room.addObject(cont);
				roomsByLevelID.put(levelID, room);
			}
			return roomsByLevelID.get(levelID);
		}
	}

	@Override
	public void delete() {
		// Delete the source file
		sourceFile.delete();
		synchronized (privInstManager.instancesCache) {
			if (privInstManager.instancesCache.containsKey(id))
				privInstManager.instancesCache.remove(id);
			Centuria.logger.info("Private instance deleted: " + id + " (name: " + name + ")");
		}

		// Remove all participants
		AccountManager accs = AccountManager.getInstance();
		for (String part : getParticipants()) {
			CenturiaAccount a = accs.getAccount(part);
			if (a != null)
				userLeft(a);
		}
	}

	@Override
	public void setAllowInvites(boolean allow) {
		try {
			// Save
			this.allowInvites = allow;
			data.addProperty("allowInvites", allow);
			Files.writeString(sourceFile.toPath(), data.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean allowInvites() {
		return allowInvites;
	}

}
