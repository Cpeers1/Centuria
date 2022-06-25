package org.asf.emuferal.entities.components.uservars;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.enums.uservars.UserVarType;

@Component
public class UserVarBitOnOnlyComponent extends UserVarComponent {

	public static final String COMPONENT_NAME = UserVarType.BitOnOnly.componentName;

	public String getComponentName() {
		return COMPONENT_NAME;
	}
}
