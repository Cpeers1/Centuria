package org.asf.centuria.seasonpasses;

import java.util.ArrayList;

/**
 * 
 * Season Pass Definition
 * 
 * @author Sky Swimmer
 *
 */
public class SeasonPassDefinition {

	public String name;
	public String objectName;
	public String passDefID;

	public long startDate;
	public long endDate;

	public String premiumPassID;

	public ArrayList<String> tiers = new ArrayList<String>();
	public ArrayList<String> challenges = new ArrayList<String>();

}
