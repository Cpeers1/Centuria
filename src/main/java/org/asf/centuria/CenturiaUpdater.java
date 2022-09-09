package org.asf.centuria;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;

public class CenturiaUpdater {

	public static void main(String[] args) throws IOException {
		// Updater
		if (args.length == 1 && args[0].equals("--update")) {
			Centuria.logger.info("Updating Centuria...");
			File updateData = new File("update.list");

			// Check validity
			if (!updateData.exists()) {
				Centuria.logger.fatal("Update list missing!");
				System.exit(1);
			}

			// Download all files in update.list
			// Should be filename=url
			for (String fileD : Files.readAllLines(updateData.toPath())) {
				String name = fileD;
				String url = name.substring(name.indexOf("=") + 1);
				name = name.substring(0, name.indexOf("="));
				Centuria.logger.info("Downloading " + name + "...");
				File output = new File(name);
				if (output.getParentFile() != null && !output.getParentFile().exists())
					output.getParentFile().mkdirs();
				URL u = new URL(url);
				InputStream strm = u.openStream();
				FileOutputStream o = new FileOutputStream(output);
				strm.transferTo(o);
				o.close();
			}

			System.exit(0);
		}
	}

}
