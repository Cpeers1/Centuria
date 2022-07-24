package org.asf.emuferal.util;

import java.util.HashMap;
import java.util.Map;

public class SanctuaryWorkCalculator {
	//TODO: hate this hate this refactor it
	private static Map<Integer, Long> stageToTimeAmountMap = new HashMap<Integer, Long>();
	private static Map<Integer, Long> enlargenIndexToTimeAmountMap = new HashMap<Integer, Long>();
	
	//Loading from charts is slow, im just going to hardcode this, it won't ever change
	private static Map<Integer, Map<Integer, Integer>> stageToItemCostsMap = new HashMap<Integer, Map<Integer, Integer>>();
	
	static {	
		Map<Integer, Integer> costs;
		
		//second room = 5 minutes	
		stageToTimeAmountMap.put(1, 5l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 300); 
		costs.put(6696, 5);
		costs.put(6705, 10);
		costs.put(6695, 10);
		
		stageToItemCostsMap.put(1, costs);
		
		//third room = 15 minutes
		stageToTimeAmountMap.put(2, 15l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 900); 
		costs.put(6696, 10);
		costs.put(6705, 20);
		costs.put(6695, 20);
		
		stageToItemCostsMap.put(2, costs);
		
		//fourth room = 45 minutes
		stageToTimeAmountMap.put(3, 45l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 2700); 
		costs.put(6696, 20);
		costs.put(6705, 40);
		costs.put(6695, 35);
		
		stageToItemCostsMap.put(3, costs);
		
		//fifth room = 90 minutes
		stageToTimeAmountMap.put(4, 90l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 5400); 
		costs.put(6696, 40);
		costs.put(6699, 20);
		costs.put(6695, 50);
		
		stageToItemCostsMap.put(4, costs);
		
		//sixth room = 300 minutes
		stageToTimeAmountMap.put(5, 300l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 18000); 
		costs.put(6696, 70);
		costs.put(6705, 60);
		costs.put(6695, 75);
		
		stageToItemCostsMap.put(5, costs);
		
		//seventh room = 600 minutes
		stageToTimeAmountMap.put(6, 600l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 36000); 
		costs.put(6696, 100);
		costs.put(6699, 20);
		costs.put(6695, 100);
		
		stageToItemCostsMap.put(6, costs);
		
		//eighth room = 1080 minutes
		stageToTimeAmountMap.put(7, 1080l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 64800); 
		costs.put(6696, 150);
		costs.put(6705, 100);
		costs.put(6695, 100);
		
		stageToItemCostsMap.put(7, costs);
		
		//ninth room = 2160 minutes
		stageToTimeAmountMap.put(8, 2160l * 60000l); 
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 129600); 
		costs.put(6696, 200);
		costs.put(6699, 40);
		costs.put(6703, 300);
		
		stageToItemCostsMap.put(8, costs);
		
		stageToTimeAmountMap.put(9, 2880l * 60000l); //tenth (final) room = 2880 minutes
		
		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 172800); 
		costs.put(6696, 250);
		costs.put(6699, 60);
		costs.put(6703, 400);
		
		stageToItemCostsMap.put(9, costs);
		
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
	
	public static Map<Integer, Integer> getCostForStageUp(int stage)
	{
		return stageToItemCostsMap.get(stage);
	}

}
