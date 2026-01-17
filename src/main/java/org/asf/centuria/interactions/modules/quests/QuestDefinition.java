package org.asf.centuria.interactions.modules.quests;

import java.util.ArrayList;

public class QuestDefinition {

	public String defID;
	public String name;
	public int questLocation;

	public int levelOverrideID;
	public ArrayList<QuestObjective> objectives = new ArrayList<QuestObjective>();

}
