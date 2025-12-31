package org.asf.centuria.textfilter.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.asf.centuria.textfilter.result.FilterResult;
import org.asf.centuria.textfilter.PhraseFilterSet;

public class FileBasedTextServiceImpl extends BaseTextFilterServiceImpl {

	private long lastReloadCheckTime = System.currentTimeMillis();
	private HashMap<String, Long> fileDates = new HashMap<String, Long>();

	private void checkFilterUpdate() {
		// Check if the filters should reload time-wise, reload checks should only
		// happen every 30 seconds, if a check is done in the last 30 seconds, skip
		if (System.currentTimeMillis() - lastReloadCheckTime < 30000)
			return;

		// Update reload check time
		lastReloadCheckTime = System.currentTimeMillis();

		// Check reload
		boolean updated = false;
		HashMap<String, Long> currentFileDates = new HashMap<String, Long>();
		loadFilterDates(new File("textfilter"), currentFileDates);

		// Check dates of known files
		synchronized (fileDates) {
			for (String path : currentFileDates.keySet()) {
				if (!fileDates.containsKey(path)) {
					// New file
					updated = true;
					break;
				}

				// Check
				if (fileDates.get(path) != currentFileDates.get(path)) {
					// Updated
					updated = true;
					break;
				}
			}

			// Check removed
			for (String path : fileDates.keySet()) {
				if (!currentFileDates.containsKey(path)) {
					// Removed
					updated = true;
					break;
				}
			}
		}

		if (updated)
			loadFilters();
	}

	@Override
	protected void loadFilters() {
		// Prepare
		HashMap<String, PhraseFilterSet> filters = new HashMap<String, PhraseFilterSet>();
		synchronized (fileDates) {
			lastReloadCheckTime = System.currentTimeMillis();
			fileDates.clear();

			// Load from disk
			// Check disk
			try {
				File filter = new File("textfilter");
				filter.mkdirs();
				if (!new File(filter, "alwaysfilter.etfd").exists()) {
					// Write file
					InputStream strm = getClass().getClassLoader()
							.getResourceAsStream("defaultfilters/alwaysfilter.etfd");
					FileOutputStream strmO = new FileOutputStream(new File(filter, "alwaysfilter.etfd"));
					strm.transferTo(strmO);
					strmO.close();
					strm.close();
				}
				if (!new File(filter, "instamute.etfd").exists()) {
					// Write file
					InputStream strm = getClass().getClassLoader().getResourceAsStream("defaultfilters/instamute.etfd");
					FileOutputStream strmO = new FileOutputStream(new File(filter, "instamute.etfd"));
					strm.transferTo(strmO);
					strmO.close();
					strm.close();
				}
				if (!new File(filter, "userfilter.etfd").exists()) {
					// Write file
					InputStream strm = getClass().getClassLoader()
							.getResourceAsStream("defaultfilters/userfilter.etfd");
					FileOutputStream strmO = new FileOutputStream(new File(filter, "userfilter.etfd"));
					strm.transferTo(strmO);
					strmO.close();
					strm.close();
				}
				if (!new File(filter, "blockednames.etfd").exists()) {
					// Write file
					InputStream strm = getClass().getClassLoader()
							.getResourceAsStream("defaultfilters/blockednames.etfd");
					FileOutputStream strmO = new FileOutputStream(new File(filter, "blockednames.etfd"));
					strm.transferTo(strmO);
					strmO.close();
					strm.close();
				}
				if (!new File(filter, "flagwords.etfd").exists()) {
					// Write file
					InputStream strm = getClass().getClassLoader().getResourceAsStream("defaultfilters/flagwords.etfd");
					FileOutputStream strmO = new FileOutputStream(new File(filter, "flagwords.etfd"));
					strm.transferTo(strmO);
					strmO.close();
					strm.close();
				}

				// Load filter
				loadFilterFilesFromDir(filter, filters);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		// Apply
		applyFilters(filters);
	}

	private void loadFilterDates(File filter, HashMap<String, Long> fileDates) {
		// Go through files
		for (File d : filter.listFiles(t -> t.isDirectory()))
			loadFilterDates(d, fileDates);
		for (File f : filter.listFiles(t -> t.isFile() && t.getName().endsWith(".etfd"))) {
			// Load filter
			fileDates.put(f.getAbsolutePath(), f.lastModified());
		}
	}

	private void loadFilterFilesFromDir(File filter, HashMap<String, PhraseFilterSet> filters) {
		// Go through files
		for (File d : filter.listFiles(t -> t.isDirectory()))
			loadFilterFilesFromDir(d, filters);
		for (File f : filter.listFiles(t -> t.isFile() && t.getName().endsWith(".etfd"))) {
			// Load filter
			FileInputStream fSrc = null;
			try {
				// Save date
				fileDates.put(f.getAbsolutePath(), f.lastModified());

				// Load set from file
				fSrc = new FileInputStream(f);
				PhraseFilterSet set = loadFilter(fSrc, f.getPath());
				fSrc.close();
				fSrc = null;

				// Add
				filters.put(set.getSetName().toLowerCase(), set);
			} catch (IOException e) {
				// Close source file
				if (fSrc != null) {
					try {
						fSrc.close();
					} catch (IOException e2) {
					}
				}
				LogManager.getLogger().error("Failed to load filter: " + f.getPath(), e);
			}
		}
	}

	@Override
	public boolean isFiltered(String text, boolean strictMode, String... tags) {
		checkFilterUpdate();
		return super.isFiltered(text, strictMode, tags);
	}

	@Override
	public boolean shouldFilterMute(String text, String... tags) {
		checkFilterUpdate();
		return super.shouldFilterMute(text, tags);
	}

	@Override
	public FilterResult filter(String text, boolean strictMode, String... tags) {
		checkFilterUpdate();
		return super.filter(text, strictMode, tags);
	}

}
