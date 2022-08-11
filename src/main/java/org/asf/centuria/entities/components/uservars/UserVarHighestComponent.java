package org.asf.centuria.entities.components.uservars;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.enums.uservars.UserVarType;

@Component
public class UserVarHighestComponent extends UserVarComponent {

	public static final String COMPONENT_NAME = UserVarType.Highest.componentName;

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

}
