package org.asf.emuferal.entities.inventory;

import org.asf.emuferal.enums.inventory.InspirationCombineStatus;

public class InspirationCombineResult {

	public InspirationCombineStatus combineStatus;
	public int enigmaDefId;
	
	public InspirationCombineResult(InspirationCombineStatus combineStatus, int enigmaDefId)
	{
		this.combineStatus = combineStatus;
		this.enigmaDefId = enigmaDefId;
	}
}
