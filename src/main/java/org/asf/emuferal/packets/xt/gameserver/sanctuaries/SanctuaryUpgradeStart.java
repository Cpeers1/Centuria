package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asf.emuferal.accounts.highlevel.TwiggleAccessor;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;
import org.asf.emuferal.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.emuferal.entities.twiggles.TwiggleWorkParameters;
import org.asf.emuferal.enums.twiggles.TwiggleState;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.util.SanctuaryWorkTimeCalculator;

public class SanctuaryUpgradeStart implements IXtPacket<SanctuaryUpgradeStart> {

	private static final String PACKET_ID = "sus";
	
	public int stage;
	public List<Integer> enlargedAreaIndexes;
	
	public boolean success;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryUpgradeStart instantiate() {
		return new SanctuaryUpgradeStart();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		stage = reader.readInt();
		
		enlargedAreaIndexes = new ArrayList<Integer>(reader.readInt());
		
		for(int i = 0; i < enlargedAreaIndexes.size(); i++)
		{
			enlargedAreaIndexes.set(i, reader.readInt());
		}
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(success);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		try {
			//Assign work to twiggle
			//Work is of type 'WorkingSanctuary'
			
			var player = (Player) client.container;
			var twiggleAccessor = player.account.getPlayerInventory().getTwiggleAccesor();
			
			TwiggleItem updatedTwiggle = null;
			
			// If the stage isn't 0, its a stage upgrade
			if(stage != 0)
			{
				TwiggleWorkParameters workParams = new TwiggleWorkParameters();
				workParams.classItemInvId = player.account.getPlayerInventory().getSanctuaryAccessor().getSanctuaryClassObject(player.account.getActiveSanctuaryLook()).get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
				workParams.stage = stage;
				updatedTwiggle = twiggleAccessor.setTwiggleWork(TwiggleState.WorkingSanctuary, System.currentTimeMillis() + SanctuaryWorkTimeCalculator.getTimeForStageUp(stage), workParams);			
			}		
			//if the enlarged array has any elements that arn't 0
			else if(enlargedAreaIndexes.contains(1))
			{
				//its a room expand upgrade
				var expansionIndex = enlargedAreaIndexes.indexOf(1);
				
				TwiggleWorkParameters workParams = new TwiggleWorkParameters();
				workParams.classItemInvId = player.account.getPlayerInventory().getSanctuaryAccessor().getSanctuaryClassObject(player.account.getActiveSanctuaryLook()).get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
				workParams.enlargedAreaIndex = expansionIndex;
				updatedTwiggle = twiggleAccessor.setTwiggleWork(TwiggleState.WorkingSanctuary, System.currentTimeMillis() + SanctuaryWorkTimeCalculator.getTimeForExpand(expansionIndex), workParams);		
			}
			
			if(updatedTwiggle == null)
			{
				//failed to expand
				this.success = false;		
				client.sendPacket(this);
			}
			else
			{
				
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			//failed to expand
			this.success = false;		
			client.sendPacket(this);
		}	

		return true;
	}
	
}
