package org.asf.emuferal.discord;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.UUID;

public class TimedActions {

	// Actions
	private static HashMap<String, TimedActionData> actions = new HashMap<String, TimedActionData>();

	// Action data container
	private static class TimedActionData {
		public long timeRemaining;
		public String code;
		public Runnable action;
	}

	// Start expiry thread
	static {
		Thread th = new Thread(() -> {
			while (true) {
				HashMap<String, TimedActionData> sActions;
				while (true) {
					try {
						sActions = new HashMap<String, TimedActionData>(actions);
						break;
					} catch (ConcurrentModificationException e) {
					}
				}

				for (String c : sActions.keySet()) {
					if (actions.get(c).timeRemaining - 1 <= 0) {
						actions.remove(c);
					} else {
						actions.get(c).timeRemaining--;
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}, "Action cleanup");
		th.setDaemon(true);
		th.start();
	}

	/**
	 * Runs a action by its identifier code and removes it
	 * 
	 * @param code Action code
	 * @return True if successful, false otherwise
	 */
	public static boolean runAction(String code) {
		HashMap<String, TimedActionData> sActions;
		while (true) {
			try {
				sActions = new HashMap<String, TimedActionData>(actions);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Find token by user ID
		for (String namespace : sActions.keySet()) {
			if (sActions.get(namespace).code.equals(code)) {
				TimedActionData d = sActions.get(namespace);

				// Remove code
				actions.remove(namespace);

				// Run action
				d.action.run();

				// Return success
				return true;
			}
		}

		// Return failure
		return false;
	}

	/**
	 * Schedules a timed action
	 * 
	 * @param namespace Action namespace (actions already in this namespace would be
	 *                  overwritten)
	 * @param action    Action runnable
	 * @param time      Action expiry time
	 * @return Action code
	 */
	public static String addAction(String namespace, Runnable action, long time) {
		// Generate action code
		String code = UUID.randomUUID().toString();
		while (true) {
			// Attempt to locate existing code
			boolean found = false;

			HashMap<String, TimedActionData> sActions;
			while (true) {
				try {
					sActions = new HashMap<String, TimedActionData>(actions);
					break;
				} catch (ConcurrentModificationException e) {
				}
			}

			for (TimedActionData d : sActions.values())
				if (d.code.equals(code)) {
					found = true;
					break;
				}

			// Break loop
			if (!found)
				break;

			// Generate new code
			code = UUID.randomUUID().toString();
		}

		// Build object info and save
		TimedActionData data = new TimedActionData();
		data.action = action;
		data.code = code;
		data.timeRemaining = time;
		actions.put(namespace, data);

		// Return code
		return code;
	}

	/**
	 * Expires codes by namespace
	 * 
	 * @param namespace Action namespace
	 */
	public static void expire(String namespace) {
		if (actions.containsKey(namespace))
			actions.remove(namespace);
	}

}
