package org.asf.emuferal.entities.systems.inspiration;

import org.asf.emuferal.enums.systems.inspiration.InspirationCombineStatus;

public class InspirationCombineResult {

	public InspirationCombineStatus combineStatus;
	public int enigmaDefId;
	
	public InspirationCombineResult(InspirationCombineStatus combineStatus, int enigmaDefId)
	{
		this.combineStatus = combineStatus;
		this.enigmaDefId = enigmaDefId;
	}
}
