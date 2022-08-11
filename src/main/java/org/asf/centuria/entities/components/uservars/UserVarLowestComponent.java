package org.asf.centuria.entities.components.uservars;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.enums.uservars.UserVarType;

@Component
public class UserVarLowestComponent extends UserVarComponent {

	public static String COMPONENT_NAME = UserVarType.Lowest.componentName;

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

}
