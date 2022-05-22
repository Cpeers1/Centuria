package org.asf.emuferal.util;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

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
			while (tasks.size() == 0) {
				try {
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
					task.run();
				} catch (Exception e) {
					System.err.println("Exception in TaskThread " + getName() + ": " + e.getClass().getTypeName()
							+ (e.getMessage() != null ? ": " + e.getMessage() : ""));
					e.printStackTrace();
				}
				this.tasks.remove(task);
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

}
