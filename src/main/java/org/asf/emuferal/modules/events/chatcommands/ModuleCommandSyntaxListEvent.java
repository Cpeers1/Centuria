package org.asf.emuferal.modules.events.chatcommands;

import org.asf.emuferal.modules.eventbus.EventObject;

/**
 * 
 * Event called to register command syntaxes (for the help command)
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ModuleCommandSyntaxListEvent extends EventObject {

	@Override
	public String eventPath() {
		return "chatcommands.helpsyntax";
	}

}
