package org.asf.centuria.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomSelectorUtil {

	private static Random rnd = new Random();

	/**
	 * Selects a item from weight
	 * 
	 * @param <T>       Item type
	 * @param selection Item weight map
	 * @return Object from the map
	 */
	public static <T> T selectWeighted(Map<T, Integer> selection) {
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

	/**
	 * Selects a item randomly
	 * 
	 * @param <T>       Item type
	 * @param selection Item list
	 * @return Object from the list
	 */
	public static <T> T selectRandom(List<T> selection) {
		if (selection.isEmpty())
			return null; // Invalid

		// Select item
		while (true) {
			int s = rnd.nextInt(selection.size() + 1);
			while (s < 0)
				s = rnd.nextInt(selection.size() + 1);
			if (s < selection.size())
				return selection.get(s);
		}
	}

}
