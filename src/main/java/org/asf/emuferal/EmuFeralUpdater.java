package org.asf.emuferal;

import java.io.IOException;

import org.asf.centuria.CenturiaUpdater;

public class EmuFeralUpdater {

	public static void main(String[] args) throws IOException {
		System.out.println("WARNING!");
		System.out.println("The EmuFeral project has been rebranded!");
		System.out.println("You are currently using a old update script, please re-install the server as this will cease to function in the future!");
		System.out.println("Visit https://github.com/Cpeers1/Centuria for the new versions, the EmuFeral repository will no longer be updated and the updater will likely break!");
		System.out.println("");
		System.out.println("");
		CenturiaUpdater.main(args);
	}

}
