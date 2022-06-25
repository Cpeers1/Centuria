package org.asf.emuferal.entities.components.uservars;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.enums.uservars.UserVarType;

@Component
public class UserVarHighestComponent extends UserVarComponent {

	public static final String COMPONENT_NAME = UserVarType.Highest.componentName;

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

}
