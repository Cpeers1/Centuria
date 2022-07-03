package org.asf.emuferal.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.enums.players.OnlineStatus;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.social.SocialManager;

public class PlayerOnlineStatus implements IXtPacket<PlayerOnlineStatus> {

	private String playerID = "";
	private OnlineStatus playerOnlineStatus;

	@Override
	public PlayerOnlineStatus instantiate() {
		return new PlayerOnlineStatus();
	}

	@Override
	public String id() {
		return "rfo";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		playerID = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // data prefix
		
		writer.writeString(playerID); // player ID
		writer.writeInt(playerOnlineStatus.value); // player online status
		
		writer.writeString(""); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		//TODO: Implement handling for the player loading in
		
		// Find online player
		playerOnlineStatus = OnlineStatus.Offline;

		// Check block
		SocialManager socialManager = SocialManager.getInstance();
		if (!socialManager.socialListExists(playerID)
				|| !socialManager.getPlayerIsBlocked(playerID, ((Player) client.container).account.getAccountID())) {
			for (Player plr : ((GameServer) client.getServer()).getPlayers()) {
				if (plr.account.getAccountID().equals(playerID)) {
					playerOnlineStatus = OnlineStatus.LoggedInToRoom;
					break;
				}
			}
		}

		// Send response
		client.sendPacket(this);

		return true;
	}

}
