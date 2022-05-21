package org.asf.emuferal.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SnifferPacketDataDecoder {

	public static void main(String[] args) throws IOException {
		String fileIn = args[0];
		String folderOut = args[1];
		new File(folderOut).mkdirs();

		int i = 0;
		for (String line : Files.readAllLines(Path.of(fileIn))) {
			line = line.substring(1);
			String info = line.substring(line.indexOf("|") + 1);
			String content = info.substring(info.indexOf("|") + 1);
			info = info.substring(0, info.indexOf("|"));

			FileOutputStream os = new FileOutputStream(
					folderOut + "/" + i++ + " (" + info.replace("->", " to ") + ").bin");
			for (int i2 = 0; i2 < content.length(); i2 += 2) {
				String hex = content.charAt(i2) + "" + content.charAt(i2 + 1);
				os.write(Integer.valueOf(hex, 16));
			}
			os.close();
			System.out.println("Decoded " + i + " packet(s)");
		}
	}

}
