package org.asf.emuferal.entities.inventory.components.uservars;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.enums.inventory.uservars.UserVarType;

@Component
public class UserVarLowestComponent extends UserVarComponent {

	public static String componentName = UserVarType.Lowest.componentName;

	@Override
	public String getComponentName() {
		return componentName;
	}

}
