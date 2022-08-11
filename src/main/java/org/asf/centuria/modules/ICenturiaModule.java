package org.asf.centuria.modules;

import org.asf.centuria.modules.eventbus.IEventReceiver;

public interface ICenturiaModule extends IEventReceiver {

	/**
	 * Defines the module ID
	 */
	public String id();

	/**
	 * Defines the module version
	 */
	public String version();

	/**
	 * Main initialization method
	 */
	public void init();

	/**
	 * Early loading method
	 */
	public default void preInit() {
	}

	/**
	 * Post-loading method
	 */
	public default void postInit() {
	}

}
