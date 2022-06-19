package org.asf.emuferal.accounts.highlevel.impl;

import java.io.InputStream;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.InspirationAccessor;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InspirationAccessorImpl extends InspirationAccessor 
{
	private static JsonObject helper;
	static {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/inspirationHelper.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("inspirations")
					.getAsJsonObject();
			strm.close();
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
		// TODO Auto-generated method stub
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
	//TODO
}
