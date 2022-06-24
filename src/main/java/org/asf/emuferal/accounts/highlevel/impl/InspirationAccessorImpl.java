package org.asf.emuferal.accounts.highlevel.impl;

import java.io.InputStream;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.InspirationAccessor;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.entities.inventory.InspirationCombineResult;
import org.asf.emuferal.enums.inventory.InspirationCombineStatus;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InspirationAccessorImpl extends InspirationAccessor 
{
	private static JsonObject helper;
	private static JsonArray enigmaRecipes;
	
	static {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/inspirationHelper.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("inspirations")
					.getAsJsonObject();
			strm.close();
			
			strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("recipes/enigmarecipes.json");
			
			enigmaRecipes = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
					.getAsJsonObject().get("EnigmaRecipes").getAsJsonArray();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}



	public InspirationAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public boolean hasInspiration(int defID) {
		return inventory.getAccessor().hasInventoryObject("8", defID);
	}

	@Override
	public void removeInspiration(String id) {
		//i don't think inspirations are ever really removed anyway
	}

	@Override
	public JsonObject getInspirationData(String id) {
		return inventory.getAccessor().findInventoryObject("8", id);
	}

	@Override
	public String addInspiration(int defID) {	
		String iID = null;

		// Generate object
		// Check existence
		if (helper.has(Integer.toString(defID))) {
			// Add item
			iID = inventory.getAccessor().createInventoryObject("8", defID,
					new ItemComponent("Inspiration", new JsonObject()));
		}

		// Return ID
		return iID;
	}

	@Override
	public void giveDefaultInspirations() {
		
		//give defaults
		
		//TODO: move to helper
		int[] defaultInspirationsIds = new int[] {
				9954, //meme
				10450, //astral oil
				10467, //barrel
				10458, //chair
				10441, //glasses
				11221, //gloves
				5528, //hat
				10447, //mask
				10460, //table
				5533, //tail
				10437, //visor
				10428, //bottle
				10344, //picture
				10425, //rat tail
				10359, //cloth
				10463, //jewel
				10427, //wall
				10454, //talon
				10456, //mesh
				5524 //feather
		};
		
		for(int defaultInspiration : defaultInspirationsIds)
		{
			//add the default inspiration
			if(!this.hasInspiration(defaultInspiration))
			{
				this.addInspiration(defaultInspiration);				
			}
		}
	}

	@Override
	public InspirationCombineResult combineInspirations(int[] inspirations, Player player) {

		//check if there's an enigma that can be crafted from the three inspirations given
		
		JsonObject result = null;
		
		for(var recipe : enigmaRecipes)
		{
			var recipeDefIds = recipe.getAsJsonObject()
					.get("recipe").getAsJsonObject()
					.get("_defIDs").getAsJsonArray();
			
			//need matches == inspirations length
			//TODO: refactor this is an ugly af solution
			int matches = 0;
			for(var recipeItemId : recipeDefIds)
			{
				var rid = Integer.parseInt(recipeItemId.getAsString());
				
				for(var inspiration : inspirations)
				{
					if(inspiration == rid)
					{
						matches++;
					}
				}
			}
						
			if(matches >= inspirations.length)
			{
				result = recipe.getAsJsonObject();
			}
		}
		
		//no result - bad combine
		if(result == null)
		{
			return new InspirationCombineResult(InspirationCombineStatus.InvalidCombo, 0);
		}
		
		//get resulting item
		
		int resultID = Integer.parseInt(result.get("resultItemId").getAsString());
		
		//TODO: check if the player has it already
			
		inventory.getItemAccessor(player).add(resultID);
		
		return new InspirationCombineResult(InspirationCombineStatus.Successful, resultID);
	}
	
	
}
