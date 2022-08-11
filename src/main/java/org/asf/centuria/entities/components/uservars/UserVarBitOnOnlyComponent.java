package org.asf.centuria.entities.components.uservars;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.enums.uservars.UserVarType;

@Component
public class UserVarBitOnOnlyComponent extends UserVarComponent {

	public static final String COMPONENT_NAME = UserVarType.BitOnOnly.componentName;

	public String getComponentName() {
		return COMPONENT_NAME;
	}
}
