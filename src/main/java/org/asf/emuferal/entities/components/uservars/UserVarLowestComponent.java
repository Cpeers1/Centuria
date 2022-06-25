package org.asf.emuferal.entities.components.uservars;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.enums.uservars.UserVarType;

@Component
public class UserVarLowestComponent extends UserVarComponent {

	public static String COMPONENT_NAME = UserVarType.Lowest.componentName;

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

}
