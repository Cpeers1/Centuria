package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class WorldReadyPacket implements IXtPacket<WorldReadyPacket> {

	public String teleportUUID = "";

	@Override
	public WorldReadyPacket instantiate() {
		return new WorldReadyPacket();
	}

	@Override
	public String id() {
		return "wr";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		teleportUUID = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find the world coordinates

		// Load player
		Player plr = (Player) client.container;
		
		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		plr.room = teleportUUID;

		// Send to tutorial if new
		if (plr.account.isPlayerNew()) {
			// Tutorial spawn
			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 107.67;
			res.y = 8.85;
			res.z = -44.85;
			res.rw = 0;
			res.rx = 0.9171;
			res.ry = -0;
			res.rz = 0.3987;
			client.sendPacket(res);
			return true;
		}

		try {
			Thread.sleep(5000); // Temporary wait
		} catch (InterruptedException e) {
		}

		// Find spawn
		handleSpawn(plr.room, plr, client);

		// Set location
		plr.lastLocation = plr.respawn;

		// Send all other players to the current player
		GameServer server = (GameServer) client.getServer();
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.syncTo(plr);
			}
		}

		// Sync spawn
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.syncTo(player);
			}
		}

		return true;
	}

	private void handleSpawn(String room, Player plr, SmartfoxClient client) throws IOException {
		// Find teleport
		switch (room) {
		case "3b8493d7-5077-4e90-880c-ed2974513a2f": {
			// City Fera Join Spawn
			plr.room = "cityfera";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 54.57;
			res.y = 3.82;
			res.z = -7.76;
			res.rw = 0;
			res.rx = 0.9979;
			res.ry = -0;
			res.rz = -0.0654;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: City Fera: Login Spawn");
			return;
		}
		case "79d7fdcb-38bc-41ea-b422-e9517f3c946b": {
			// City Shopping Plaza // TODO
			plr.room = "cityfera";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 99.99889;
			res.y = 9.128172;
			res.z = -50.78214;
			res.rw = 0;
			res.rx = 0.6088401;
			res.ry = 0;
			res.rz = -0.7932929;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: City Fera: Shopping Plaza");
			return;
		}
		case "5975db44-3bd3-481b-90a1-a12b561c5eff": {
			// City Fera: Centuria Door // TODO
			plr.room = "cityfera";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 144.0079;
			res.y = 14.16582;
			res.z = -43.13978;
			res.rw = 0;
			res.rx = -0.7482728;
			res.ry = 0;
			res.rz = 0.6633912;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: City Fera: Centuria (Exit Door)");
			return;
		}
		case "8f1dc98c-2d5d-47b0-9ef7-87d7e606d37b": {
			// Centuria // TODO
			plr.room = "centuria";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 0.03377199;
			res.y = -4.768372E-07;
			res.z = 19.85855;
			res.rw = 0;
			res.rx = 0.9999619;
			res.ry = 0;
			res.rz = -0.008726531;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println(
					"Player teleport: " + plr.account.getDisplayName() + ", teleport destination: City Fera: Centuria");
			return;
		}
		case "3814e0d6-e731-4cf8-ac7c-6a663fe24c6b": {
			// Centuria: Door // TODO
			plr.room = "centuria";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 0.03377199;
			res.y = -4.768372E-07;
			res.z = 19.85855;
			res.rw = 0;
			res.rx = 0.9999619;
			res.ry = 0;
			res.rz = -0.008726531;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: City Fera: Centuria (from City Fera)");
			return;
		}
		case "6cf58e20-1975-453f-ba90-a0c93ad50391": {
			// Blood Tundra West Spawn // TODO
			plr.room = "bloodtundra";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 397.9397;
			res.y = 51.88305;
			res.z = 197.1988;
			res.rw = 3.961745E-12;
			res.rx = 0.9999619;
			res.ry = -1.400224E-11;
			res.rz = -0.2722489;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Blood Tundra: The Tree");
			return;
		}
		case "f82542e2-5c28-4ef1-a475-a486c337d5c6": {
			// Lakeroot West Spawn // TODO
			plr.room = "lakeroot";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = -333.6679;
			res.y = 43.56293;
			res.z = 224.623;
			res.rw = 0;
			res.rx = 0.5688978;
			res.ry = 0;
			res.rz = 0.8224082;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Lakeroot: Lakeroot Beach");
			break;
		}
		case "1473b384-8887-48a8-b6a1-04db3a874a0c": {
			// Shattered Bay: Back Entryway // TODO
			plr.room = "shatteredbay";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 0.2999811;
			res.y = 26.56618;
			res.z = -193.95;
			res.rw = 0;
			res.rx = 0;
			res.ry = 0;
			res.rz = 1;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Shattered Bay: Back Entryway");
			break;
		}
		case "8bdf6b4c-f968-435c-b71d-266ab47869d0": {
			// Mugmyre Marsh: Kobold Camp // TODO
			plr.room = "murgmyre";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = -318.6938;
			res.y = 72.18782;
			res.z = 10.87311;
			res.rw = -1.31041E-11;
			res.rx = 0.9773986;
			res.ry = -6.32777E-12;
			res.rz = -0.2114049;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Mugmyre Marsh: Kobold Camp");
			break;
		}
		case "33c96321-50ba-48b1-8448-2dfc0ee063b0": {
			// Sunken Thicket: Thicket Base // TODO
			plr.room = "sunkenthicket";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = -39.86998;
			res.y = 30.04944;
			res.z = 9.340012;
			res.rw = 0;
			res.rx = 0;
			res.ry = 0;
			res.rz = 1;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Sunken Thicket: Thicket Base");
			break;
		}
		case "88111293-ddaf-4fe5-b1d3-ee54b6970153": {
			// Latchkey's Lab (Door) // TODO
			plr.room = "latchkey";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 7.515773E-07;
			res.y = -0.4878199;
			res.z = 8.597492;
			res.rw = 0;
			res.rx = 1;
			res.ry = 0;
			res.rz = -4.371139E-08;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Latchkey's Lab (Door)");
			break;
		}
		case "c2e9f139-1829-4f23-a357-e717d55ba26b": {
			// Latchkey's Lab (Map) // TODO
			plr.room = "latchkey";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 7.515773E-07;
			res.y = -0.4878199;
			res.z = 8.597492;
			res.rw = 0;
			res.rx = 1;
			res.ry = 0;
			res.rz = -4.371139E-08;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Latchkey's Lab (Map)");
			break;
		}
		case "bda8e376-3915-482a-a634-e0f1fcc386c8": {
			// Sunken Thicket: Latchkey's Lab (Exit Door) // TODO
			plr.room = "sunkenthicket";

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = -89.39825;
			res.y = 33.78165;
			res.z = 32.70113;
			res.rw = 0;
			res.rx = 0.7639955;
			res.ry = 0;
			res.rz = 0.6452216;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Sunken Thicket: Latchkey's Lab (Exit Door)");
			break;
		}
		default: {
			System.err.println("Player teleport: " + plr.account.getDisplayName() + " to unrecognized spawn!");
			room = room;

			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 0;
			res.y = 80;
			res.z = 0;
			res.rw = 0;
			res.rx = 0;
			res.ry = 0;
			res.rz = 0;
			client.sendPacket(res);

			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
			System.out.println("Player teleport: " + plr.account.getDisplayName()
					+ ", teleport destination: Sunken Thicket: Latchkey's Lab (Exit Door)");
		}
		}
	}

}
