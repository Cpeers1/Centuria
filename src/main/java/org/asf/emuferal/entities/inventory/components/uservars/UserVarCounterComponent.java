package org.asf.emuferal.entities.inventory.components.uservars;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.enums.inventory.uservars.UserVarType;

@Component
public class UserVarCounterComponent extends UserVarComponent {

	private static String componentName = UserVarType.Counter.componentName;
	
	@Override
	public String getComponentName() {
		return componentName;
	}

}
