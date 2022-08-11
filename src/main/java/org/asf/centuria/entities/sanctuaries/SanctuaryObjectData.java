package org.asf.centuria.entities.sanctuaries;

import org.asf.centuria.entities.objects.WorldObjectPositionInfo;

public class SanctuaryObjectData {
	
	public WorldObjectPositionInfo positionInfo;
	public int gridId;
	public String parentId;
	public int state;

	public SanctuaryObjectData(WorldObjectPositionInfo objectPositionInfo, int gridId, String parentId, int state)
	{
		this.positionInfo = objectPositionInfo;
		this.gridId = gridId;
		this.parentId = parentId;
		this.state = state;
	}
}
