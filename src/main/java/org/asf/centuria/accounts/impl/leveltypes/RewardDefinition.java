package org.asf.centuria.accounts.impl.leveltypes;

import com.google.gson.JsonObject;

// Reward definitions
public class RewardDefinition {
	// Basics
	public int weight;
	public int itemQuantity;
	public int itemDefId;

	// Eval-based
	public String quantityEval;
	public String weightEval;

	// Randomized
	public int quantityMin = -1;
	public int quantityMax = -1;

	public RewardDefinition parse(JsonObject obj) {
		itemDefId = obj.get("itemDefId").getAsInt();
		if (obj.has("weight"))
			weight = obj.get("weight").getAsInt();
		if (obj.has("itemQuantity"))
			itemQuantity = obj.get("itemQuantity").getAsInt();
		if (obj.has("quantityMin"))
			quantityMin = obj.get("quantityMin").getAsInt();
		if (obj.has("quantityMax"))
			quantityMax = obj.get("quantityMax").getAsInt();
		if (obj.has("weightEval"))
			weightEval = obj.get("weightEval").getAsString();
		if (obj.has("quantityEval"))
			quantityEval = obj.get("quantityEval").getAsString();
		return this;
	}
}
