package org.asf.centuria.entities.inspiration;

import org.asf.centuria.enums.inspiration.InspirationCombineStatus;

public class InspirationCombineResult {

	public InspirationCombineStatus combineStatus;
	public String enigmaDefId;

	public InspirationCombineResult(InspirationCombineStatus combineStatus, String enigmaDefId) {
		this.combineStatus = combineStatus;
		this.enigmaDefId = enigmaDefId;
	}
}
