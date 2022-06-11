package org.asf.emuferal.interactions.dataobjects;

import java.util.ArrayList;
import java.util.HashMap;

public class StateInfo {

	public String command;
	public String[] params;
	public String actorId;
	public HashMap<String, ArrayList<StateInfo>> branches = new HashMap<String, ArrayList<StateInfo>>();

}
