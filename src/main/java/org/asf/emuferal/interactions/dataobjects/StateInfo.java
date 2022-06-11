package org.asf.emuferal.interactions.dataobjects;

import java.util.HashMap;

public class StateInfo {

	public String[] params;
	public String actorId;
	public HashMap<String, HashMap<String, StateInfo>> branches = new HashMap<String, HashMap<String, StateInfo>>();

}
