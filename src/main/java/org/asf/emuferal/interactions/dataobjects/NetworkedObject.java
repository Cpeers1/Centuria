package org.asf.emuferal.interactions.dataobjects;

import java.util.HashMap;

public class NetworkedObject {

	public String objectName;
	public int localType;

	public ObjectInfo primaryObjectInfo;
	public ObjectInfo subObjectInfo;

	public LocationInfo locationInfo;
	public HashMap<String, HashMap<String, StateInfo>> stateInfo = new HashMap<String, HashMap<String, StateInfo>>();

}
