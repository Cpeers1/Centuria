package org.asf.centuria.util;

import java.util.HashMap;
import java.util.Map;

public class SanctuaryWorkCalculator {
	// TODO: hate this hate this refactor it
	private static Map<Integer, Long> stageToTimeAmountMap = new HashMap<Integer, Long>();
	private static Map<Integer, Long> enlargenIndexToTimeAmountMap = new HashMap<Integer, Long>();

	// Loading from charts is slow, im just going to hardcode this, it won't ever
	// change
	private static Map<Integer, Map<Integer, Integer>> stageToItemCostsMap = new HashMap<Integer, Map<Integer, Integer>>();
	private static Map<Integer, Map<Integer, Integer>> enlargenIndexToItemCostsMap = new HashMap<Integer, Map<Integer, Integer>>();

	static {
		Map<Integer, Integer> costs;

		// second room = 5 minutes
		stageToTimeAmountMap.put(1, 5l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 300);
		costs.put(6696, 5);
		costs.put(6705, 10);
		costs.put(6695, 10);

		stageToItemCostsMap.put(1, costs);

		// third room = 15 minutes
		stageToTimeAmountMap.put(2, 15l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 900);
		costs.put(6696, 10);
		costs.put(6705, 20);
		costs.put(6695, 20);

		stageToItemCostsMap.put(2, costs);

		// fourth room = 45 minutes
		stageToTimeAmountMap.put(3, 45l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 2700);
		costs.put(6696, 20);
		costs.put(6705, 40);
		costs.put(6695, 35);

		stageToItemCostsMap.put(3, costs);

		// fifth room = 90 minutes
		stageToTimeAmountMap.put(4, 90l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 5400);
		costs.put(6696, 40);
		costs.put(6699, 20);
		costs.put(6695, 50);

		stageToItemCostsMap.put(4, costs);

		// sixth room = 300 minutes
		stageToTimeAmountMap.put(5, 300l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 18000);
		costs.put(6696, 70);
		costs.put(6705, 60);
		costs.put(6695, 75);

		stageToItemCostsMap.put(5, costs);

		// seventh room = 600 minutes
		stageToTimeAmountMap.put(6, 600l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 36000);
		costs.put(6696, 100);
		costs.put(6699, 20);
		costs.put(6695, 100);

		stageToItemCostsMap.put(6, costs);

		// eighth room = 1080 minutes
		stageToTimeAmountMap.put(7, 1080l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 64800);
		costs.put(6696, 150);
		costs.put(6705, 100);
		costs.put(6695, 100);

		stageToItemCostsMap.put(7, costs);

		// ninth room = 2160 minutes
		stageToTimeAmountMap.put(8, 2160l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 129600);
		costs.put(6696, 200);
		costs.put(6699, 40);
		costs.put(6703, 300);

		stageToItemCostsMap.put(8, costs);

		stageToTimeAmountMap.put(9, 2880l * 60000l); // tenth (final) room = 2880 minutes

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 172800);
		costs.put(6696, 250);
		costs.put(6699, 60);
		costs.put(6703, 400);

		stageToItemCostsMap.put(9, costs);

		// ----------------------------- EnLargens ------------------------- //

		// first room enlargen = 2 minutes
		enlargenIndexToTimeAmountMap.put(0, 2l * 60000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(6698, 60);
		costs.put(6705, 400);

		enlargenIndexToItemCostsMap.put(0, costs);

		// second room enlargen = 600 seconds
		enlargenIndexToTimeAmountMap.put(1, 600l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 600);
		costs.put(6698, 10);
		costs.put(6705, 10);

		enlargenIndexToItemCostsMap.put(1, costs);

		// third room enlargen = 1800 seconds
		enlargenIndexToTimeAmountMap.put(2, 1800l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 1800);
		costs.put(6698, 20);
		costs.put(6705, 20);

		enlargenIndexToItemCostsMap.put(2, costs);

		// fourth room enlargen = 3600 seconds
		enlargenIndexToTimeAmountMap.put(3, 3600l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 3600);
		costs.put(6704, 10);
		costs.put(6698, 30);
		costs.put(6705, 40);

		enlargenIndexToItemCostsMap.put(3, costs);

		// fifth room enlargen = 10800 seconds
		enlargenIndexToTimeAmountMap.put(4, 10800l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 10800);
		costs.put(6701, 5);
		costs.put(6698, 40);
		costs.put(6695, 40);

		enlargenIndexToItemCostsMap.put(4, costs);

		// sixth room enlargen = 21600 seconds
		enlargenIndexToTimeAmountMap.put(5, 10800l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 21600);
		costs.put(6704, 20);
		costs.put(6698, 60);
		costs.put(6705, 65);

		enlargenIndexToItemCostsMap.put(5, costs);

		// seventh room enlargen = 43200 seconds
		enlargenIndexToTimeAmountMap.put(6, 43200l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 43200);
		costs.put(6701, 15);
		costs.put(6698, 75);
		costs.put(6695, 55);

		enlargenIndexToItemCostsMap.put(6, costs);

		// eighth room enlargen = 86400 seconds
		enlargenIndexToTimeAmountMap.put(7, 86400l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 86400);
		costs.put(6704, 50);
		costs.put(6698, 100);
		costs.put(6705, 100);

		enlargenIndexToItemCostsMap.put(7, costs);

		// Ninth room enlargen = 172800 seconds
		enlargenIndexToTimeAmountMap.put(8, 172800l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 172800);
		costs.put(6702, 25);
		costs.put(6698, 150);
		costs.put(6699, 50);

		enlargenIndexToItemCostsMap.put(8, costs);

		// Tenth room enlargen = 259200 seconds
		enlargenIndexToTimeAmountMap.put(9, 259200l * 1000l);

		costs = new HashMap<Integer, Integer>();
		costs.put(8193, 259200);
		costs.put(6702, 50);
		costs.put(6698, 150);
		costs.put(6699, 75);

		enlargenIndexToItemCostsMap.put(9, costs);
	}

	public static long getTimeForStageUp(int stage) {
		if (!stageToTimeAmountMap.containsKey(stage))
			throw new ArrayIndexOutOfBoundsException(
					"The stage to time amount map contains no value for stage " + stage);

		return stageToTimeAmountMap.get(stage);
	}

	public static long getTimeForExpand(int expandArrayIndex) {
		if (!enlargenIndexToTimeAmountMap.containsKey(expandArrayIndex))
			throw new ArrayIndexOutOfBoundsException(
					"The expand to time amount map contains no value for expand index " + expandArrayIndex);

		return enlargenIndexToTimeAmountMap.get(expandArrayIndex);
	}

	public static Map<Integer, Integer> getCostForStageUp(int stage) {
		return stageToItemCostsMap.get(stage);
	}

	public static Map<Integer, Integer> getCostForEnlargen(int enlargenIndex) {
		return enlargenIndexToItemCostsMap.get(enlargenIndex);
	}

}
