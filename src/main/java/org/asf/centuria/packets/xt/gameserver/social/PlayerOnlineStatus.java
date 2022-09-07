package org.asf.centuria.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.players.OnlineStatus;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.social.SocialManager;

public class PlayerOnlineStatus implements IXtPacket<PlayerOnlineStatus> {

	private static final String PACKET_ID = "rfo";

	private String playerID = "";
	private OnlineStatus playerOnlineStatus;

	@Override
	public PlayerOnlineStatus instantiate() {
		return new PlayerOnlineStatus();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		playerID = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // data prefix
		
		writer.writeString(playerID); // player ID
		writer.writeInt(playerOnlineStatus.value); // player online status
		
		writer.writeString(DATA_SUFFIX); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		// Find online player
		playerOnlineStatus = OnlineStatus.Offline;

		// Check block
		SocialManager socialManager = SocialManager.getInstance();
		if (!socialManager.socialListExists(playerID)
				|| !socialManager.getPlayerIsBlocked(playerID, ((Player) client.container).account.getAccountID())) {
			for (Player plr : ((GameServer) client.getServer()).getPlayers()) {
				if (plr.account.getAccountID().equals(playerID)) {

					//TODO: Is this correct implementation for when the player is loading in?
					if(plr.roomReady)
					{
						playerOnlineStatus = OnlineStatus.LoggedInToRoom;
					}
					else
					{
						playerOnlineStatus = OnlineStatus.LoggingIn;
					}

					break;
				}
			}
		}

		// Send response
		client.sendPacket(this);

		return true;
	}

}
