package org.asf.emuferal.accounts.highlevel.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.TwiggleAccessor;
import org.asf.emuferal.entities.components.generic.TimeStampComponent;
import org.asf.emuferal.entities.components.twiggles.TwiggleComponent;
import org.asf.emuferal.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.emuferal.entities.twiggles.TwiggleWorkParameters;
import org.asf.emuferal.enums.inventory.InventoryType;

import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;

public class TwiggleAccessorImpl extends TwiggleAccessor {

	public TwiggleAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public TwiggleItem addNewTwiggle() {
		try {
			String invId = Integer.toString(InventoryType.Twiggles.invTypeId);

			// find Twiggle Inv
			if (!inventory.containsItem(invId))
				inventory.setItem(invId, new JsonArray());

			var twiggleInv = inventory.getItem(invId).getAsJsonArray();

			var newTwiggleItem = createNewTwiggle();

			twiggleInv.add(newTwiggleItem.toJsonObject());

			inventory.setItem(invId, twiggleInv);

			return newTwiggleItem;
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void removeTwiggle() {
		// TODO Auto-generated method stub

	}

	@Override
	public TwiggleItem setTwiggleWork(int workType, long workEndTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TwiggleItem setTwiggleWork(int workType, long workEndTime, TwiggleWorkParameters twiggleWorkParameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TwiggleItem[] getAllTwiggles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TwiggleItem getTwiggle(String twiggleInvId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TwiggleItem clearTwiggleWork(String twiggleInvId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void giveDefaultTwiggles() {

		String invId = Integer.toString(InventoryType.Twiggles.invTypeId);

		// find Twiggle Inv
		if (!inventory.containsItem(invId))
			inventory.setItem(invId, new JsonArray());

		var twiggleInv = inventory.getItem(invId).getAsJsonArray();

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

}
