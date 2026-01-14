package org.asf.centuria.textfilter.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.result.FilterResult;

/**
 * 
 * Text filter context memory set object - a filter-based message history used
 * by the textfilter to expand context sensitivity across multiple messages
 * 
 * @author Sky Swimmer
 * 
 */
public class TextFilterContextMemory {

	private FilterSeverity triggerSeverity;

	private ArrayList<FilterResult> context = new ArrayList<FilterResult>();
	private ArrayList<TriggerMemoryEntry> triggerMemoryList = new ArrayList<TriggerMemoryEntry>();
	private HashMap<FilterResult, TriggerMemoryEntry> triggerMemory = new HashMap<FilterResult, TriggerMemoryEntry>();

	private class TriggerMemoryEntry {
		public FilterResult entry;
		public int indexInContext;
	}

	private int maxLength = 0;
	private int maxMemoryLengthTriggers = 0;
	private int maxMessagePositionAgeTriggers = 0;
	private int rangeSurroundingTrigger = 0;
	private long memoryTriggerAgeLimit = 0;

	/**
	 * Initializes the context memory object with default settings based on a max
	 * amount of non-flagged messages
	 * 
	 * @param maxLength Max amount of messages to store
	 */
	public TextFilterContextMemory(FilterSeverity triggerSeverity, int maxLength) {
		this(triggerSeverity, maxLength, 5);
	}

	/**5
	 * Initializes the context memory object with default settings based on a max
	 * amount of non-flagged messages and a set amount of trigger flags to remember,
	 * using 20 minutes as maximum message age for trigger memory
	 * 
	 * @param maxLength               Max amount of messages to store
	 * @param maxMemoryLengthTriggers Max amount of triggers to store
	 */
	public TextFilterContextMemory(FilterSeverity triggerSeverity, int maxLength, int maxMemoryLengthTriggers) {
		this(triggerSeverity, maxLength, maxMemoryLengthTriggers, 20 * 60 * 1000);
	}

	/**
	 * Initializes the context memory object with default settings based on a max
	 * amount of non-flagged messages and a set amount of trigger flags to remember
	 * as well as a pre-configured memory trigger age limit
	 * 
	 * @param maxLength               Max amount of messages to store
	 * @param maxMemoryLengthTriggers Max amount of triggers to store
	 * @param memoryTriggerAgeLimit   The maximum age of a trigger object for
	 *                                context consideration in milliseconds
	 */
	public TextFilterContextMemory(FilterSeverity triggerSeverity, int maxLength, int maxMemoryLengthTriggers,
			long memoryTriggerAgeLimit) {
		this(triggerSeverity, maxLength, maxMemoryLengthTriggers, memoryTriggerAgeLimit,
				maxLength == -1 ? 10 : maxLength);
	}

	/**
	 * Initializes the context memory object with default settings based on a max
	 * amount of non-flagged messages and a set amount of trigger flags to remember
	 * as well as a pre-configured memory trigger age limit
	 * 
	 * @param maxLength               Max amount of messages to store
	 * @param maxMemoryLengthTriggers Max amount of triggers to store
	 * @param memoryTriggerAgeLimit   The maximum age of a trigger object for
	 *                                context consideration in milliseconds
	 * @param rangeSurroundingTrigger The amount of messages surrounding a trigger
	 *                                to retain
	 */
	public TextFilterContextMemory(FilterSeverity triggerSeverity, int maxLength, int maxMemoryLengthTriggers,
			long memoryTriggerAgeLimit, int rangeSurroundingTrigger) {
		this(triggerSeverity, maxLength, maxMemoryLengthTriggers, memoryTriggerAgeLimit, 15, rangeSurroundingTrigger);
	}

	/**
	 * Initializes the context memory object with default settings based on a max
	 * amount of non-flagged messages and a set amount of trigger flags to remember
	 * as well as a pre-configured memory trigger age limit
	 * 
	 * @param maxLength                     Max amount of messages to store
	 * @param maxMemoryLengthTriggers       Max amount of triggers to store
	 * @param memoryTriggerAgeLimit         The maximum age of a trigger object for
	 *                                      context consideration in milliseconds
	 * @param maxMessagePositionAgeTriggers The maximum distance of a trigger
	 *                                      message before its removed to conserve
	 *                                      memory and keep context recent enough
	 * @param rangeSurroundingTrigger       The amount of messages surrounding a
	 *                                      trigger to retain
	 */
	public TextFilterContextMemory(FilterSeverity triggerSeverity, int maxLength, int maxMemoryLengthTriggers,
			long memoryTriggerAgeLimit, int maxMessagePositionAgeTriggers, int rangeSurroundingTrigger) {
		this.maxLength = maxLength;
		this.maxMessagePositionAgeTriggers = maxMessagePositionAgeTriggers;
		this.maxMemoryLengthTriggers = maxMemoryLengthTriggers;
		this.memoryTriggerAgeLimit = memoryTriggerAgeLimit;
		this.rangeSurroundingTrigger = rangeSurroundingTrigger;
		this.triggerSeverity = triggerSeverity;
	}

	private void refactorTriggerMemoryList() {
		for (TriggerMemoryEntry entry : triggerMemory.values()) {
			entry.indexInContext = context.indexOf(entry.entry);
		}
	}

	/**
	 * Adds a filter result to the context
	 * 
	 * @param result Result object to add
	 */
	public void pushToContext(FilterResult result) {
		synchronized (context) {
			if (!context.contains(result)) {
				int index = context.size();
				context.add(result);

				// Check trim
				if (maxLength != -1) {
					// Limit of messages set, trim if needed, and store filter trigger if flagged to
					// expand context further

					// Check state
					if (result.isMatch() && result.getSeverity().ordinal() >= triggerSeverity.ordinal()) {
						// Mark
						TriggerMemoryEntry entry = new TriggerMemoryEntry();
						entry.entry = result;
						entry.indexInContext = index;
						triggerMemory.put(result, entry);
						triggerMemoryList.add(entry);

						// Check max amount of triggers to store
						if (triggerMemory.size() > maxMemoryLengthTriggers) {
							// Trim trigger list
							boolean trimmed = false;
							int off = 0;
							while (triggerMemory.size() > maxMemoryLengthTriggers && context.size() > maxLength) {
								TriggerMemoryEntry ent = triggerMemoryList.get(0);

								// Remove surrounding
								int amountPreceeding = (ent.indexInContext - off);
								for (int i = 0; i < amountPreceeding && context.size() > maxLength; i++)
									context.remove(0);

								// Remove if needed
								if (context.size() > maxLength) {
									triggerMemoryList.remove(ent);
									triggerMemory.remove(ent.entry);
									context.remove(ent.entry);
								}

								// Mark trimmed
								off += amountPreceeding + 1;
								trimmed = true;
							}
							if (trimmed)
								refactorTriggerMemoryList();
						}
					}

					// Trim old triggers if needed
					if (triggerMemory.size() != 0) {
						// Remove aging by position
						if (maxMessagePositionAgeTriggers != -1) {
							boolean trimmed = false;
							int off = 0;
							while (triggerMemory.size() != 0 && context.size() > maxLength) {
								TriggerMemoryEntry ent = triggerMemoryList.get(0);
								int distanceToFirst = index - (ent.indexInContext - off);
								if (distanceToFirst > maxMessagePositionAgeTriggers) {
									// Remove surrounding
									int amountPreceeding = (ent.indexInContext - off);
									for (int i = 0; i < amountPreceeding && context.size() > maxLength; i++)
										context.remove(0);

									// Remove if needed
									if (context.size() > maxLength) {
										triggerMemoryList.remove(ent);
										triggerMemory.remove(ent.entry);
										context.remove(ent.entry);
									}

									// Mark trimmed
									off += amountPreceeding + 1;
									trimmed = true;
								} else
									break;
							}
							if (trimmed)
								refactorTriggerMemoryList();
						}

						// Remove aging by time
						if (memoryTriggerAgeLimit != -1) {
							boolean trimmed = false;
							int off = 0;
							while (triggerMemory.size() != 0 && context.size() > maxLength) {
								TriggerMemoryEntry ent = triggerMemoryList.get(0);
								if ((System.currentTimeMillis() - ent.entry.getResultTime()) > memoryTriggerAgeLimit) {
									// Remove surrounding
									int amountPreceeding = (ent.indexInContext - off);
									for (int i = 0; i < amountPreceeding && context.size() > maxLength; i++)
										context.remove(0);

									// Remove if needed
									if (context.size() > maxLength) {
										triggerMemoryList.remove(ent);
										triggerMemory.remove(ent.entry);
										context.remove(ent.entry);
									}

									// Mark trimmed
									off += amountPreceeding + 1;
									trimmed = true;
								} else
									break;
							}
							if (trimmed)
								refactorTriggerMemoryList();
						}
					}

					// Trim if needed based on message history size
					if (context.size() > maxLength) {
						boolean trimmed = false;

						// Check trigger presence
						if (triggerMemory.size() != 0) {
							// Check first trigger entry
							TriggerMemoryEntry ent = triggerMemoryList.get(0);
							int amountPreceeding = ent.indexInContext;

							// Check amount of entries preceeding the trigger, if above the range, trim the
							// message history
							if (amountPreceeding > rangeSurroundingTrigger) {
								// Trim
								trimmed = true;

								// Remove preceeding messages
								for (int i = 0; i < amountPreceeding && i < rangeSurroundingTrigger; i++)
									context.remove(0);
							}
						} else {
							// No stored trigger entry, trim normally

							// Trim
							while (context.size() > maxLength) {
								context.remove(0);
							}
							trimmed = true;
						}

						// Refactor
						if (trimmed)
							refactorTriggerMemoryList();
					}
				}
			}
		}
	}

	/**
	 * Removes a filter result object from the context
	 * 
	 * @param result Result object to remove
	 */
	public void removeFromContext(FilterResult result) {
		synchronized (context) {
			if (context.contains(result))
				context.remove(result);
			if (triggerMemory.containsKey(result)) {
				TriggerMemoryEntry e = triggerMemory.remove(result);
				triggerMemoryList.remove(e);
			}
			refactorTriggerMemoryList();
		}
	}

	/**
	 * Removes a list of filter result objects from the context
	 * 
	 * @param results List of filter results to remove
	 */
	public void removeAll(FilterResult[] results) {
		synchronized (context) {
			for (FilterResult res : results) {
				if (context.contains(res))
					context.remove(res);
				if (triggerMemory.containsKey(res)) {
					TriggerMemoryEntry e = triggerMemory.remove(res);
					triggerMemoryList.remove(e);
				}
			}
			refactorTriggerMemoryList();
		}
	}

	/**
	 * Removes a list of filter result objects from the context
	 * 
	 * @param results List of filter results to remove
	 */
	public void removeAll(Collection<FilterResult> results) {
		synchronized (context) {
			for (FilterResult res : results) {
				if (context.contains(res))
					context.remove(res);
				if (triggerMemory.containsKey(res)) {
					TriggerMemoryEntry e = triggerMemory.remove(res);
					triggerMemoryList.remove(e);
				}
			}
			refactorTriggerMemoryList();
		}
	}

	/**
	 * Retrieves the amount of messages stored
	 * 
	 * @return Amount of messages stored in this context object
	 */
	public int count() {
		return context.size();
	}

	/**
	 * Retrieves all filter results recorded in this context memory set
	 * 
	 * @return Array of FilterResult instances
	 */
	public FilterResult[] getMessagesInContext() {
		synchronized (context) {
			return context.toArray(t -> new FilterResult[t]);
		}
	}

}
