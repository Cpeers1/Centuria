package org.asf.centuria.util;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;

public class TaskThread extends Thread {

	private ArrayList<Runnable> tasks = new ArrayList<Runnable>();
	private boolean stop = false;

	public TaskThread() {
		super();
	}

	public TaskThread(String name) {
		super(name);
	}

	@Override
	public void run() {
		while (!stop) {
			while (tasks.isEmpty()) {
				try {
					if (stop)
						break;
					Thread.sleep(1);
				} catch (InterruptedException e) {
				}
			}

			ArrayList<Runnable> tasks;
			while (true) {
				try {
					tasks = new ArrayList<Runnable>(this.tasks);
					break;
				} catch (ConcurrentModificationException e) {
				}
			}

			for (Runnable task : tasks) {
				try {
					if (task != null)
						task.run();
				} catch (Exception e) {
					Centuria.logger.error(MarkerManager.getMarker("TaskThreads"), "Exception in TaskThread " + getName(), e);
					e.printStackTrace();
				}
				try {
					this.tasks.remove(task);
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Schedules a task
	 * 
	 * @param task Task to schedule
	 */
	public void schedule(Runnable task) {
		if (!stop)
			tasks.add(task);
	}

	/**
	 * Stops the thread, waits for the current task to finish before exiting
	 */
	public void stopCleanly() {
		stop = true;
		try {
			join();
		} catch (InterruptedException e) {
		}
		tasks.clear();
	}

	/**
	 * Waits for all tasks to finish
	 */
	public void flush() {
		flush(-1);
	}

	/**
	 * Waits for all tasks to finish
	 * 
	 * @param timeout Wait timeout length in seconds
	 */
	public void flush(int timeout) {
		int i = 0;
		while (!tasks.isEmpty()) {
			try {
				if (i < timeout * 10)
					i++;
				else
					break;
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

}
