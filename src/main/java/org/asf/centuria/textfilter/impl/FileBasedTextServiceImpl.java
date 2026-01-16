package org.asf.centuria.textfilter.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.asf.centuria.textfilter.result.FilterResult;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.IResultStringBuilder;
import org.asf.centuria.textfilter.PhraseFilterSet;
import org.asf.centuria.textfilter.context.TextFilterContextMemory;

public class FileBasedTextServiceImpl extends BaseTextFilterServiceImpl {

	private boolean useTimeBasedRefresher = false;
	private boolean reloadRequired = false;

	private long lastReloadCheckTime = System.currentTimeMillis();
	private HashMap<String, Long> fileDates = new HashMap<String, Long>();

	@Override
	public void initService() {
		super.initService();

		// Check disk
		try {
			File filter = new File("textfilter");
			filter.mkdirs();
			if (!new File(filter, "alwaysfilter.etfd").exists()) {
				// Write file
				InputStream strm = getClass().getClassLoader().getResourceAsStream("defaultfilters/alwaysfilter.etfd");
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
				InputStream strm = getClass().getClassLoader().getResourceAsStream("defaultfilters/userfilter.etfd");
				FileOutputStream strmO = new FileOutputStream(new File(filter, "userfilter.etfd"));
				strm.transferTo(strmO);
				strmO.close();
				strm.close();
			}
			if (!new File(filter, "blockednames.etfd").exists()) {
				// Write file
				InputStream strm = getClass().getClassLoader().getResourceAsStream("defaultfilters/blockednames.etfd");
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

			// Create monitor
			try {
				// Create
				WatchService service = FileSystems.getDefault().newWatchService();

				// Register
				registerRecurse(service, filter);

				// Start thread
				AsyncTaskManager.runAsync(() -> {
					while (true) {
						try {
							WatchKey k = service.take();
							for (WatchEvent<?> event : k.pollEvents()) {
								WatchEvent.Kind<?> kind = event.kind();
								if (kind == StandardWatchEventKinds.OVERFLOW)
									continue;
								Path parent = (Path) k.watchable();
								Path entry = (Path) event.context();
								File path = new File(parent.toFile(), entry.toString());
								if (kind == StandardWatchEventKinds.ENTRY_CREATE && path.isDirectory()) {
									// Subdirectory made

									// Register
									try {
										registerRecurse(service, path);
									} catch (IOException e) {
									}
								} else {
									// Handle
									if (path.isFile() || kind == StandardWatchEventKinds.ENTRY_DELETE) {
										// Require update
										reloadRequired = true;
									}
								}
							}
							if (!k.reset())
								break;
						} catch (InterruptedException e) {
							break;
						}
					}
				});
			} catch (Exception e) {
				// Use time based
				useTimeBasedRefresher = true;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void registerRecurse(WatchService service, File filter) throws IOException {
		filter.toPath().register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		for (File sub : filter.listFiles(t -> t.isDirectory()))
			registerRecurse(service, sub);
	}

	private void checkFilterUpdate() {
		// Check mode
		if (useTimeBasedRefresher) {
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
					if (fileDates.get(path).longValue() != currentFileDates.get(path).longValue()) {
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
			reloadRequired = updated;
		}

		if (reloadRequired) {
			reloadRequired = false;
			LogManager.getLogger().info("Updating chat filters...");
			loadFilters();
		}
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
				LogManager.getLogger().debug("Loading filter: " + f.getPath());
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
	public boolean isFiltered(TextFilterContextMemory memory, FilterSeverity minimalSeverity, String text,
			boolean strictMode, String... tags) {
		checkFilterUpdate();
		return super.isFiltered(memory, minimalSeverity, text, strictMode, tags);
	}

	@Override
	public boolean shouldFilterMute(TextFilterContextMemory memory, FilterSeverity minimalSeverity, String text,
			String... tags) {
		checkFilterUpdate();
		return super.shouldFilterMute(memory, minimalSeverity, text, tags);
	}

	@Override
	public FilterResult filter(TextFilterContextMemory memory, FilterSeverity minimalSeverity, String text,
			boolean strictMode, IResultStringBuilder stringBuilder, String... tags) {
		checkFilterUpdate();
		return super.filter(memory, minimalSeverity, text, strictMode, stringBuilder, tags);
	}

}
