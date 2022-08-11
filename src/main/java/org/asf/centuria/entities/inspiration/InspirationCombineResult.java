package org.asf.centuria.entities.inspiration;

import org.asf.centuria.enums.inspiration.InspirationCombineStatus;

public class InspirationCombineResult {

	public InspirationCombineStatus combineStatus;
	public int enigmaDefId;

	public InspirationCombineResult(InspirationCombineStatus combineStatus, int enigmaDefId) {
		this.combineStatus = combineStatus;
		this.enigmaDefId = enigmaDefId;
	}
}
