package org.asf.centuria.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class DumpPackets {

	public static void main(String[] args) throws IOException {	
		while(true)
		{
			try (Scanner sc = new Scanner(System.in)) {
				System.out.print("Packet data folder: ");
				String folderIn = sc.nextLine();

				File dir = new File(folderIn);
				File[] directoryListing = dir.listFiles();
				if (directoryListing != null) {
				  for (File child : directoryListing) {
					  try {
					    // Do something with child
						System.out.println("Loading " + child.getName() + "...");
						ArrayList<String> packets = new ArrayList<String>();
						int i = 1;
						for (String line : Files.readAllLines(Path.of(child.getAbsolutePath()))) {
							line = line.substring(1);
							String info = line.substring(line.indexOf("|") + 1);
							String content = info.substring(info.indexOf("|") + 1);
							info = info.substring(0, info.indexOf("|"));
				
							ByteArrayOutputStream os = new ByteArrayOutputStream();
							for (int i2 = 0; i2 < content.length(); i2 += 2) {
								String hex = content.charAt(i2) + "" + content.charAt(i2 + 1);
								os.write(Integer.valueOf(hex, 16));
							}
							os.close();
							packets.add(info + ": " + new String(os.toByteArray(), "UTF-8"));
							System.out.println(i++ + " packets prepared...");
						}
				
						ArrayList<String> packetData = new ArrayList<String>();
						for (i = 0; i < packets.size(); i++) {
							String pk = packets.get(i);
							while (!pk.endsWith("%") && !pk.endsWith("\0") && i + 1 < packets.size()) {
								String pk2 = packets.get(i + 1);
								if (!pk2.startsWith("%")) {
									i++;
									pk += pk2;
								} else
									break;
							}
							packetData.add(pk);
							System.out.println(i + 1 + " packets processed...");
						}
				
						System.out.println("Dumping...");
						String fileOutPath = child.getAbsolutePath() + ".dump";
						
						FileWriter fileOutWriter = new FileWriter(fileOutPath);
						
						for (String packet : packetData) {
							fileOutWriter.write(packet + "\n");
						}
						
						fileOutWriter.close();
						continue;  
					  }
					  catch(Exception e)
					  {
							System.out.println("cannot process, skipping...");
					  }
				
				  	}
				}
			}			
		}
	}

}
