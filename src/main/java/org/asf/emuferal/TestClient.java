package org.asf.emuferal;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.asf.emuferal.data.XtReader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestClient {

	public static void main(String[] args) throws MalformedURLException, IOException {
		String api = "http://api.fera.rawaho.dev:8080";
		String director = "http://director.fera.rawaho.dev:8081";
		int gameServerPort = 2410;

		// Authenticate
		JsonObject auth = JsonParser.parseString(postJson("{\"username\":\"skyswimmer\",\"password\":\"Stefan2004\"}",
				api + "/a/authenticate", new HashMap<String, String>())).getAsJsonObject();
		String refreshToken = auth.get("refresh_token").getAsString();
		String accessToken = auth.get("auth_token").getAsString();
		String uuid = auth.get("uuid").getAsString();

		// Connect with the game server
		JsonObject server = JsonParser
				.parseString(getJson(director + "/v1/bestGameServer", new HashMap<String, String>())).getAsJsonObject();
		String gameServer = server.get("smartfoxServer").getAsString();
		Socket cl = new Socket();
		cl.connect(new InetSocketAddress(gameServer, gameServerPort), 1000);

		// Version checker
		sendPacket(cl, "<msg t='sys'><body action='verChk' r='0'><ver v='165' /></body></msg>");
		readPacket(cl);

		// rndK something
		sendPacket(cl, "<msg t='sys'><body action='rndK' r='-1'></body></msg>");
		String key = readPacket(cl);

		// Login
		sendPacket(cl, "<msg t='sys'><body action='login' r='0'><login z='sbiLogin'><nick><![CDATA[" + uuid
				+ "9%0%0.19.1%9%0%ASUS TUF Gaming A15 FA506IV_TUF506IV (ASUSTeK COMPUTER INC.)%0]]></nick><pword><![CDATA["
				+ accessToken + "]]></pword></login></body></msg>");
		String data = readPacket(cl);

		String objects = "1\n" + "200\n" + "2\n" + "104\n" + "100\n" + "3\n" + "111\n" + "7\n" + "8\n" + "9\n" + "4\n"
				+ "302\n" + "105\n" + "102\n" + "6\n" + "5\n" + "201\n" + "10\n" + "103\n" + "300\n" + "303\n" + "304\n"
				+ "110\n" + "400\n" + "311";

		for (String obj : objects.split("\n")) {
			sendPacket(cl, "%xt%o%ilt%-1%" + obj + "%");
			String resp = readPacket(cl);
			while (!resp.startsWith("%xt%il%-1%")) {
				resp = readPacket(cl);
			}

			XtReader rd = new XtReader(resp);
			rd.read();
			rd.read();
			String dt = rd.read();

			try {
				GZIPInputStream decompressor = new GZIPInputStream(
						new ByteArrayInputStream(Base64.getDecoder().decode(dt)));
				FileOutputStream o = new FileOutputStream("inventory-" + obj + ".json");
				decompressor.transferTo(o);
				decompressor.close();
				o.close();
			} catch (Exception e) {
			}
		}

		sendPacket(cl, "%xt%o%wr%-1%3b8493d7-5077-4e90-880c-ed2974513a2f%%");
		String rjR = readPacket(cl);
		sendPacket(cl, "%xt%o%$l%-1%30550%");
		sendPacket(cl, "%xt%o%ka%-1%");
		sendPacket(cl, "%xt%o%wr%-1%3b8493d7-5077-4e90-880c-ed2974513a2f%%");
		sendPacket(cl, "%xt%o%rjt%-1%");
		String d3 = readPacket(cl);
		String d4 = readPacket(cl);
		sendPacket(cl, "%xt%o%ou%-1%0%107.67%8.85%-44.85%0%0%0%0%0.9170844%0%0.3986933%0%");
		sendPacket(cl, "%xt%o%als%-1%6d7c4813-e20c-4d94-b64e-1f6edf3fdf26%");
		String d5 = readPacket(cl);
		sendPacket(cl,
				"%xt%o%utc%-1%Look Name%2%{\"actorClassDefID\":\"1929\",\"bodyParts\":[{\"bodyPartDefID\":\"2086\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2083\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2089\",\"attached\":true,\"_decalEntries\":[{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.879974\",\"y\":\"0.209771\"},\"mirroredPosition\":{\"x\":\"0.110955\",\"y\":\"0.424341\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7778,4000,6500\"},\"rotation\":\"200.637500\",\"rotationCompensation\":\"-107.195800\",\"mirrorRotationCompensation\":\"-252.856600\",\"scale\":\"0.077212\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.828178\",\"y\":\"0.186970\"},\"mirroredPosition\":{\"x\":\"0.162830\",\"y\":\"0.401385\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8060,7974,6000\"},\"rotation\":\"175.959900\",\"rotationCompensation\":\"-95.516980\",\"mirrorRotationCompensation\":\"-264.639000\",\"scale\":\"0.123228\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4186\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.785125\",\"y\":\"0.250773\"},\"mirroredPosition\":{\"x\":\"0.205879\",\"y\":\"0.465148\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8060,7974,6000\"},\"rotation\":\"15.562280\",\"rotationCompensation\":\"-93.172960\",\"mirrorRotationCompensation\":\"-266.941300\",\"scale\":\"0.108928\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4156\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.745191\",\"y\":\"0.232091\"},\"mirroredPosition\":{\"x\":\"0.245809\",\"y\":\"0.446535\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7778,4000,6000\"},\"rotation\":\"318.885200\",\"rotationCompensation\":\"-76.860230\",\"mirrorRotationCompensation\":\"76.580920\",\"scale\":\"0.088952\",\"flipX\":false,\"flipY\":false,\"mirror\":true},{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.799291\",\"y\":\"0.161676\"},\"mirroredPosition\":{\"x\":\"0.191684\",\"y\":\"0.376162\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8060,7974,6000\"},\"rotation\":\"170.835500\",\"rotationCompensation\":\"-97.904430\",\"mirrorRotationCompensation\":\"-262.113500\",\"scale\":\"0.077499\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4185\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.735050\",\"y\":\"0.181732\"},\"mirroredPosition\":{\"x\":\"0.255933\",\"y\":\"0.396183\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8060,7974,6000\"},\"rotation\":\"187.265700\",\"rotationCompensation\":\"-92.505930\",\"mirrorRotationCompensation\":\"-267.449100\",\"scale\":\"0.100386\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4181\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.755521\",\"y\":\"0.138621\"},\"mirroredPosition\":{\"x\":\"0.235464\",\"y\":\"0.352987\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8060,7974,6000\"},\"rotation\":\"108.100300\",\"rotationCompensation\":\"-95.819780\",\"mirrorRotationCompensation\":\"-264.323200\",\"scale\":\"0.108951\",\"flipX\":false,\"flipY\":false,\"mirror\":true},{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.901615\",\"y\":\"0.622830\"},\"mirroredPosition\":{\"x\":\"0.078280\",\"y\":\"0.801144\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7778,4000,6500\"},\"rotation\":\"184.187000\",\"rotationCompensation\":\"-92.756460\",\"mirrorRotationCompensation\":\"-267.328400\",\"scale\":\"0.077212\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4185\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.822633\",\"y\":\"0.618296\"},\"mirroredPosition\":{\"x\":\"0.157221\",\"y\":\"0.796644\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8060,7974,6000\"},\"rotation\":\"201.663600\",\"rotationCompensation\":\"-98.327230\",\"mirrorRotationCompensation\":\"-261.844200\",\"scale\":\"0.086100\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.884461\",\"y\":\"0.597936\"},\"mirroredPosition\":{\"x\":\"0.095490\",\"y\":\"0.776229\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8060,7974,6000\"},\"rotation\":\"173.901700\",\"rotationCompensation\":\"-94.359760\",\"mirrorRotationCompensation\":\"-265.355800\",\"scale\":\"0.083234\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.829438\",\"y\":\"0.660645\"},\"mirroredPosition\":{\"x\":\"0.150540\",\"y\":\"0.839342\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7778,4000,6500\"},\"rotation\":\"212.976300\",\"rotationCompensation\":\"-105.022400\",\"mirrorRotationCompensation\":\"-256.643300\",\"scale\":\"0.051506\",\"flipX\":true,\"flipY\":false,\"mirror\":true},{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.847176\",\"y\":\"0.571400\"},\"mirroredPosition\":{\"x\":\"0.132793\",\"y\":\"0.749808\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7778,4000,6500\"},\"rotation\":\"112.217200\",\"rotationCompensation\":\"-93.459500\",\"mirrorRotationCompensation\":\"-268.300100\",\"scale\":\"0.080061\",\"flipX\":false,\"flipY\":false,\"mirror\":true},{\"defID\":\"4184\",\"isDefault\":true,\"disabled\":false,\"position\":{\"x\":\"0.779476\",\"y\":\"0.596852\"},\"mirroredPosition\":{\"x\":\"0.200484\",\"y\":\"0.774721\"},\"color1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7772,6013,6000\"},\"rotation\":\"185.209700\",\"rotationCompensation\":\"-115.654400\",\"mirrorRotationCompensation\":\"-244.558700\",\"scale\":\"0.054690\",\"flipX\":true,\"flipY\":false,\"mirror\":true}]},{\"bodyPartDefID\":\"2087\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2097\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2093\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"3792\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2085\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2094\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2090\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2088\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"3793\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"7553\",\"attached\":true,\"_decalEntries\":[]}],\"_bodyColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"6778,1100,10000\"},\"_bodyColor2HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8285,10000,5412\"},\"_bodyColor3HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"6894,4314,10000\"},\"_bodyColor4HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"4887,6425,8118\"},\"eyeShapeDefID\":\"2262\",\"eyePupilDefID\":\"2274\",\"_eyeShapeColorHSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"1431,8500,5500\"},\"_eyePupilColorHSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"3837,10000,10000\"},\"_sparkColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8603,6549,10000\"},\"_hornColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8556,10000,9400\"},\"_hornColor2HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7472,900,10000\"},\"_hornColor3HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"4889,6400,8100\"},\"_hornColor4HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"5379,10000,7412\"},\"_wingColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8354,5159,6157\"},\"_wingColor2HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8495,3316,7333\"},\"_wingColor3HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8374,3432,9255\"},\"_wingColor4HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8561,1725,10000\"},\"eyeShapeScale\":0.5,\"eyePupilScale\":0.5,\"clothingItems\":[],\"defaultDecalsEnabled\":true,\"scaleGroups\":[{\"scaleGroupDefID\":\"698\",\"scale\":0.5},{\"scaleGroupDefID\":\"699\",\"scale\":0.5},{\"scaleGroupDefID\":\"700\",\"scale\":0.5},{\"scaleGroupDefID\":\"701\",\"scale\":0.5},{\"scaleGroupDefID\":\"702\",\"scale\":0.5},{\"scaleGroupDefID\":\"704\",\"scale\":0.5},{\"scaleGroupDefID\":\"709\",\"scale\":0.49649477005004885},{\"scaleGroupDefID\":\"710\",\"scale\":0.5000001788139343},{\"scaleGroupDefID\":\"711\",\"scale\":0.5},{\"scaleGroupDefID\":\"3130\",\"scale\":0.5}]}%");
		String d6 = readPacket(cl);
		String d7 = readPacket(cl);
		sendPacket(cl,
				"%xt%o%alz%-1%6d7c4813-e20c-4d94-b64e-1f6edf3fdf26%%0%{\"actorClassDefID\":\"1929\",\"bodyParts\":[{\"bodyPartDefID\":\"2086\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2083\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2089\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2087\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2097\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2093\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"3792\",\"attached\":true,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2085\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2094\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2090\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"2088\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"3793\",\"attached\":false,\"_decalEntries\":[]},{\"bodyPartDefID\":\"7553\",\"attached\":true,\"_decalEntries\":[]}],\"_bodyColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"5641,10000,10000\"},\"_bodyColor2HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"10000,0,10000\"},\"_bodyColor3HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"6894,4314,10000\"},\"_bodyColor4HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"4887,6425,8118\"},\"eyeShapeDefID\":\"2262\",\"eyePupilDefID\":\"2274\",\"_eyeShapeColorHSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"5649,8883,7373\"},\"_eyePupilColorHSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"3837,10000,10000\"},\"_sparkColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8534,7490,10000\"},\"_hornColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7910,10000,9412\"},\"_hornColor2HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"7472,900,10000\"},\"_hornColor3HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"4889,6400,8100\"},\"_hornColor4HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"5379,10000,7412\"},\"_wingColor1HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8354,5159,6157\"},\"_wingColor2HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8495,3316,7333\"},\"_wingColor3HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8374,3432,9255\"},\"_wingColor4HSV\":{\"_h\":\"\",\"_s\":\"\",\"_v\":\"\",\"_hsv\":\"8561,1725,10000\"},\"eyeShapeScale\":0.5,\"eyePupilScale\":0.5,\"clothingItems\":[],\"defaultDecalsEnabled\":true,\"scaleGroups\":[{\"scaleGroupDefID\":\"698\",\"scale\":0.5},{\"scaleGroupDefID\":\"699\",\"scale\":0.5},{\"scaleGroupDefID\":\"700\",\"scale\":0.5},{\"scaleGroupDefID\":\"701\",\"scale\":0.5},{\"scaleGroupDefID\":\"702\",\"scale\":0.5},{\"scaleGroupDefID\":\"704\",\"scale\":0.5},{\"scaleGroupDefID\":\"709\",\"scale\":1.0},{\"scaleGroupDefID\":\"710\",\"scale\":0.5000001788139343},{\"scaleGroupDefID\":\"711\",\"scale\":0.5},{\"scaleGroupDefID\":\"3130\",\"scale\":0.5}]}%");
		sendPacket(cl, "%xt%o%oas%-1%920d5920-39c3-4756-9beb-30425b59b1a9%");
		sendPacket(cl, "%xt%o%oac%-1%920d5920-39c3-4756-9beb-30425b59b1a9%");
		String d9 = readPacket(cl);

		//
		cl = cl;

	}

	private static String postJson(String data, String url, HashMap<String, String> headers)
			throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		headers.forEach((k, v) -> conn.setRequestProperty(k, v));
		conn.getOutputStream().write(data.getBytes("UTF-8"));
		return new String(conn.getInputStream().readAllBytes(), "UTF-8");
	}

	private static String getJson(String url, HashMap<String, String> headers)
			throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("Content-Type", "application/json");
		headers.forEach((k, v) -> conn.setRequestProperty(k, v));
		return new String(conn.getInputStream().readAllBytes(), "UTF-8");
	}

	private static void sendPacket(Socket client, String packet) throws IOException {
		byte[] payload = packet.getBytes("UTF-8");
		byte[] newPayload = new byte[payload.length + 1];
		for (int i = 0; i < payload.length; i++)
			newPayload[i] = payload[i];
		newPayload[payload.length] = 0;
		client.getOutputStream().write(newPayload);
		client.getOutputStream().flush();
	}

	private static String readPacket(Socket client) throws IOException {
		ArrayList<Byte> bytes = new ArrayList<Byte>();
		while (true) {
			int b = client.getInputStream().read();
			if (b == -1) {
				throw new IOException("Stream closed");
			} else if (b == 0) {
				byte[] packet = new byte[bytes.size()];
				int i = 0;
				for (byte b2 : bytes) {
					packet[i++] = b2;
				}
				String payload = new String(packet, "UTF-8");
				bytes.clear();
				return payload;
			} else
				bytes.add((byte) b);
		}
	}

}
