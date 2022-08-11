package org.asf.centuria.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class WeightedSelectorUtil {

	private static Random rnd = new Random();

	/**
	 * Selects a item from weight
	 * 
	 * @param <T>       Item type
	 * @param selection Item weight map
	 * @return Object from the map
	 */
	public static <T> T select(Map<T, Integer> selection) {
		if (selection.isEmpty())
			return null; // Invalid

		// Build list
		ArrayList<T> items = new ArrayList<T>();
		selection.forEach((itm, weight) -> {
			for (int i = 0; i < weight; i++)
				items.add(itm);
		});
		if (items.isEmpty())
			return null; // Invalid

		// Select item
		while (true) {
			int s = rnd.nextInt(items.size() + 1);
			while (s < 0)
				s = rnd.nextInt(items.size() + 1);
			if (s < items.size())
				return items.get(s);
		}
	}

}
