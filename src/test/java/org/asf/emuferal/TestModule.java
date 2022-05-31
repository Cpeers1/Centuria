package org.asf.emuferal;

import org.asf.emuferal.modules.IEmuFeralModule;

public class TestModule implements IEmuFeralModule {

	@Override
	public String id() {
		return "test";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
		getClass();
	}

}
