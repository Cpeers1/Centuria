package org.asf.centuria.accounts.highlevel.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.TwiggleAccessor;
import org.asf.centuria.entities.components.generic.TimeStampComponent;
import org.asf.centuria.entities.components.twiggles.TwiggleComponent;
import org.asf.centuria.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.centuria.entities.twiggles.TwiggleWorkParameters;
import org.asf.centuria.enums.inventory.InventoryType;
import org.asf.centuria.enums.twiggles.TwiggleState;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public class TwiggleAccessorImpl extends TwiggleAccessor {

	public TwiggleAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public TwiggleItem addNewTwiggle() {
		try {
			var twiggleInv = getTwiggleInv();

			var newTwiggleItem = createNewTwiggle();

			twiggleInv.add(newTwiggleItem.toJsonObject());

			inventory.setItem(Integer.toString(InventoryType.Twiggles.invTypeId), twiggleInv);

			return newTwiggleItem;
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public boolean removeTwiggle() {
		var twiggleInv = getTwiggleInv();
		
		//find a twiggle that isn't working
		try {
			
			TwiggleItem selectedTwiggle = null;
			for(var twiggle : twiggleInv)
			{
				TwiggleItem twiggleItem = new TwiggleItem();
				twiggleItem.fromJsonObject(twiggle.getAsJsonObject());
	
				if(twiggleItem.getTwiggleComponent().workType == TwiggleState.None)
				{
					selectedTwiggle = twiggleItem;
					break;
				}
			}
			
			if(selectedTwiggle == null)
				return false;
			
			//remove it 
			//TODO: BLAH bad bad 
			return removeTwiggle(selectedTwiggle.uuid);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		

	}
	
	@Override
	public boolean removeTwiggle(String twiggleInvId) {
		var twiggleInv = getTwiggleInv();
		
		try {
			
			JsonElement selectedTwiggleElement = null;
			for(var twiggle : twiggleInv)
			{
				TwiggleItem twiggleItem = new TwiggleItem();
				twiggleItem.fromJsonObject(twiggle.getAsJsonObject());
	
				if(twiggleItem.getTwiggleComponent().workType == TwiggleState.None && twiggleItem.uuid == twiggleInvId)
				{
					selectedTwiggleElement = twiggle;
					break;
				}
			}
			
			if(selectedTwiggleElement == null)
				return false;
			
			//remove it 
			twiggleInv.remove(selectedTwiggleElement);
			inventory.setItem(Integer.toString(InventoryType.Twiggles.invTypeId), twiggleInv);
			
			return true;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public TwiggleItem setTwiggleWork(TwiggleState workType, long workEndTime) {
		var twiggleInv = getTwiggleInv();
		
		try {
			
			//Get the first nonworking twiggle
			TwiggleItem selectedTwiggle = null;
			int index = 0;
			for(var twiggle : twiggleInv)
			{
				TwiggleItem twiggleItem = new TwiggleItem();
				twiggleItem.fromJsonObject(twiggle.getAsJsonObject());
	
				if(twiggleItem.getTwiggleComponent().workType == TwiggleState.None)
				{
					selectedTwiggle = twiggleItem;
					break;
				}
				
				index++;
			}
			
			if(selectedTwiggle == null) return null;
			
			//Set it to work
			selectedTwiggle.getTwiggleComponent().workType = workType;
			selectedTwiggle.getTwiggleComponent().workEndTime = workEndTime;
			
			selectedTwiggle.getTimeStampComponent().stamp();
			
			//remove old twiggle item
			twiggleInv.remove(index);
			
			//add new twiggle item
			twiggleInv.add(selectedTwiggle.toJsonObject());
			
			return selectedTwiggle;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public TwiggleItem setTwiggleWork(TwiggleState workType, long workEndTime, TwiggleWorkParameters twiggleWorkParameters) {
		var twiggleInv = getTwiggleInv();
		
		try {
			
			//Get the first nonworking twiggle
			TwiggleItem selectedTwiggle = null;
			int index = 0;
			for(var twiggle : twiggleInv)
			{
				TwiggleItem twiggleItem = new TwiggleItem();
				twiggleItem.fromJsonObject(twiggle.getAsJsonObject());
	
				if(twiggleItem.getTwiggleComponent().workType == TwiggleState.None)
				{
					selectedTwiggle = twiggleItem;
					break;
				}
				
				index++;
			}
			
			if(selectedTwiggle == null) return null;
			
			//Set it to work
			selectedTwiggle.getTwiggleComponent().workType = workType;
			selectedTwiggle.getTwiggleComponent().workEndTime = workEndTime;
			selectedTwiggle.getTwiggleComponent().twiggleWorkParams = twiggleWorkParameters;
			
			selectedTwiggle.getTimeStampComponent().stamp();
			
			//remove old twiggle item
			twiggleInv.remove(index);
			
			//add new twiggle item
			twiggleInv.add(selectedTwiggle.toJsonObject());
			
			return selectedTwiggle;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public TwiggleItem[] getAllTwiggles() {
		List<TwiggleItem> twiggles = new ArrayList<TwiggleItem>();
		var twiggleInv = getTwiggleInv();
		
		try
		{			
			for(var twiggle : twiggleInv)
			{
				TwiggleItem twiggleItem = new TwiggleItem();
				twiggleItem.fromJsonObject(twiggle.getAsJsonObject());
				twiggles.add(twiggleItem);
			}
			
			return (TwiggleItem[]) twiggles.toArray();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public TwiggleItem getTwiggle(String twiggleInvId) {
		var twiggleJsonObject = inventory.getAccessor().findInventoryObject(Integer.toString(InventoryType.Twiggles.invTypeId), twiggleInvId);
		if(twiggleJsonObject == null) return null;
		
		try {		
			
			TwiggleItem twiggleItem = new TwiggleItem();
			twiggleItem.fromJsonObject(twiggleJsonObject);
			
			return twiggleItem;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public TwiggleItem clearTwiggleWork(String twiggleInvId) {
		//TODO
		return null;
	}

	@Override
	public void giveDefaultTwiggles() {

		var twiggleInv = getTwiggleInv();

		var twigglesToAdd = DEFAULT_TWIGGLE_AMOUNT - twiggleInv.size();

		if (twigglesToAdd <= 0) return;

		for (int i = 0; i < twigglesToAdd; i++) {
			addNewTwiggle();
		}
	}

	private TwiggleItem createNewTwiggle() throws JsonSyntaxException, UnsupportedEncodingException, IOException {
		// create a new twiggle item

		var twiggleItem = new TwiggleItem(TWIGGLE_DEF_ID, UUID.randomUUID().toString());

		var twiggleComponent = new TwiggleComponent();
		var timeStamp = new TimeStampComponent(System.currentTimeMillis());

		twiggleItem.setTwiggleComponent(twiggleComponent);
		twiggleItem.setTimeStampComponent(timeStamp);

		return twiggleItem;
	}
	
	private JsonArray getTwiggleInv()
	{
		String invId = Integer.toString(InventoryType.Twiggles.invTypeId);

		// find Twiggle Inv
		if (!inventory.containsItem(invId))
			inventory.setItem(invId, new JsonArray());

		return inventory.getItem(invId).getAsJsonArray();
	}


}
