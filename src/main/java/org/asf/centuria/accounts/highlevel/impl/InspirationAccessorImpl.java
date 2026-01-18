package org.asf.centuria.accounts.highlevel.impl;

import java.io.InputStream;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.InspirationAccessor;
import org.asf.centuria.accounts.highlevel.ItemAccessor;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.entities.inspiration.InspirationCombineResult;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.inspiration.InspirationCombineStatus;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InspirationAccessorImpl extends InspirationAccessor {
	private static JsonObject helper;
	private static JsonObject enigmas;

	static {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("inspirations.json");

			var partial = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();

			helper = partial.get("inspirations").getAsJsonObject();
			strm.close();

			strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("recipes/enigmarecipes.json");

			enigmas = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("EnigmaRecipes").getAsJsonObject();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public InspirationAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public boolean hasInspiration(String defID) {
		return inventory.getAccessor().hasInventoryObjectByDefId("8", defID);
	}

	@Override
	public void removeInspiration(String id) {
		// i don't think inspirations are ever really removed anyway
	}

	@Override
	public JsonObject getInspirationData(String id) {
		return inventory.getAccessor().findInventoryObjectByItemId("8", id);
	}

	@Override
	public String addInspiration(String defID) {
		String iID = null;

		// Generate object
		// Check existence
		if (helper.has(defID)) {
			// Add item
			iID = inventory.getAccessor().createInventoryObject("8", defID,
					new ItemComponent("Inspiration", new JsonObject()));
		}

		// Return ID
		return iID;
	}

	@Override
	public void giveDefaultInspirations() {
		// give defaults

		int[] defaultInspirationsIds = new int[] {
				// Defaults

				9954, // meme
				10450, // astral oil
				10467, // barrel
				10458, // chair
				10441, // glasses
				11221, // gloves
				5528, // hat
				10447, // mask
				10460, // table
				5533, // tail
				10437, // visor
				10428, // bottle
				10344, // picture
				10425, // rat tail
				10359, // cloth
				10463, // jewel
				10427, // wall
				10454, // talon
				10456, // mesh
				5524 // feather
		};

		for (int defaultInspiration : defaultInspirationsIds) {
			// add the default inspiration
			if (!this.hasInspiration(Integer.toString(defaultInspiration))) {
				this.addInspiration(Integer.toString(defaultInspiration));
			}
		}
	}

	@Override
	public InspirationCombineResult combineInspirations(String[] inspirations, Player player) {
		// check if there's an enigma that can be crafted from the three inspirations
		// given

		JsonObject result = null;
		String resultID = null;

		for (String enigmaID : enigmas.keySet()) {
			// enigma info
			JsonObject enigmaData = enigmas.get(enigmaID).getAsJsonObject();
			if (!enigmaData.has("recipe"))
				continue;
			var recipeDefIds = enigmaData.get("recipe").getAsJsonObject().get("_defIDs").getAsJsonArray();
			if (recipeDefIds.size() != inspirations.length)
				continue; // Not the correct amount

			// need matches == inspirations length
			// TODO: refactor this is an ugly af solution
			int matches = 0;
			for (var recipeItemId : recipeDefIds) {
				String rid = recipeItemId.getAsString();

				for (var inspiration : inspirations) {
					if (inspiration.equals(rid)) {
						matches++;
					}
				}
			}

			if (matches >= inspirations.length) {
				result = enigmaData;
				resultID = enigmaID;
			}
		}

		// no result - bad combine
		if (result == null) {
			return new InspirationCombineResult(InspirationCombineStatus.InvalidCombo, "0");
		}

		// Give item if not owned
		if (!inventory.getAccessor().hasInventoryObjectByDefId(ItemAccessor.getInventoryTypeOf(resultID), resultID))
			inventory.getItemAccessor(player).add(resultID);
		else
			return new InspirationCombineResult(InspirationCombineStatus.AlreadyOwned, resultID);

		return new InspirationCombineResult(InspirationCombineStatus.Successful, resultID);
	}

	@Override
	public String getEnigmaResult(String enigma) {
		// Find enigma
		for (String enigmaID : enigmas.keySet()) {
			JsonObject enigmaData = enigmas.get(enigmaID).getAsJsonObject();
			if (enigmaID.equals(enigma)) {
				// Return result
				return enigmaData.get("resultItemId").getAsString();
			}
		}

		return null;
	}

}
