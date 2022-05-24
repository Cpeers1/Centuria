package org.asf.connective.https;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;

@CYAN_COMPONENT
public class ConnectiveHTTPS extends CyanComponent {
	
	protected static void initComponent() {
		ConnectiveHTTPSServer implementation = new ConnectiveHTTPSServer();
		implementation.assignAsMainImplementation();
	}
	
}
