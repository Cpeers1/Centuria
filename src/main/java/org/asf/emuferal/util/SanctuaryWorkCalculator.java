package org.asf.emuferal.util;

import java.util.HashMap;
import java.util.Map;

public class SanctuaryWorkCalculator {
	//TODO: hate this hate this refactor it
	private static Map<Integer, Long> stageToTimeAmountMap = new HashMap<Integer, Long>();
	private static Map<Integer, Long> enlargenIndexToTimeAmountMap = new HashMap<Integer, Long>();
	
	private static Map<Integer, Map> stageToItemCostsMap = new HashMap<Integer, HashMap<String, short>>();
	
	static {	
		stageToTimeAmountMap.put(1, 5l * 60000l); //second room = 5 minutes
		stageToTimeAmountMap.put(2, 15l * 60000l); //third room = 15 minutes
		stageToTimeAmountMap.put(3, 45l * 60000l); //fourth room = 45 minutes
		stageToTimeAmountMap.put(4, 90l * 60000l); //fifth room = 90 minutes
		stageToTimeAmountMap.put(5, 300l * 60000l); //sixth room = 300 minutes
		stageToTimeAmountMap.put(6, 600l * 60000l); //seventh room = 600 minutes
		stageToTimeAmountMap.put(7, 1080l * 60000l); //eighth room = 1080 minutes
		stageToTimeAmountMap.put(8, 2160l * 60000l); //ninth room = 2160 minutes
		stageToTimeAmountMap.put(9, 2880l * 60000l); //tenth (final) room = 2880 minutes
		
		enlargenIndexToTimeAmountMap.put(0, 2l * 60000l); //first room enlargen = 2 minutes
	}
	
	public static long getTimeForStageUp(int stage)
	{
		if(!stageToTimeAmountMap.containsKey(stage))
			throw new ArrayIndexOutOfBoundsException("The stage to time amount map contains no value for stage " + stage);
		
		return stageToTimeAmountMap.get(stage);
	}
	
	public static long getTimeForExpand(int expandArrayIndex)
	{
		if(!enlargenIndexToTimeAmountMap.containsKey(expandArrayIndex))
			throw new ArrayIndexOutOfBoundsException("The expand to time amount map contains no value for expand index " + expandArrayIndex);
		
		return enlargenIndexToTimeAmountMap.get(expandArrayIndex);
	}

}
