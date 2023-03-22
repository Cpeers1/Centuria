package org.asf.centuria.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

public class DecodeInventoryPacketString {
	public static void main(String[] args) throws IOException {

		// HACKYYY
		for (int i = 0; i > -1; i++) {
			try {
				Scanner sc = new Scanner(System.in);

				System.out.print("base64: ");
				String input = sc.nextLine();

				System.out.println("Loading...");

				byte[] decoded = Base64.getDecoder().decode(input);
				InputStream stream = new ByteArrayInputStream(decoded);
				GZIPInputStream gis = new GZIPInputStream(stream);
				ByteArrayOutputStream output = new ByteArrayOutputStream();

				// copy GZIPInputStream to ByteArrayOutputStream
				byte[] buffer = new byte[1024];
				int len;
				while ((len = gis.read(buffer)) > 0) {
					output.write(buffer, 0, len);
				}

				String decodedToString = new String(output.toByteArray(), "UTF-8");

				System.out.println(decodedToString);
				sc.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
