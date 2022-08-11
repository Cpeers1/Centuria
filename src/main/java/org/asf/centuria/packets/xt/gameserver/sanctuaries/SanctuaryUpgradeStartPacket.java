package org.asf.centuria.packets.xt.gameserver.sanctuaries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asf.centuria.accounts.highlevel.TwiggleAccessor;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.centuria.entities.twiggles.TwiggleWorkParameters;
import org.asf.centuria.enums.twiggles.TwiggleState;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.world.JoinRoom;
import org.asf.centuria.players.Player;
import org.asf.centuria.util.SanctuaryWorkCalculator;

import com.google.gson.JsonArray;

public class SanctuaryUpgradeStartPacket implements IXtPacket<SanctuaryUpgradeStartPacket> {

	private static final String PACKET_ID = "sus";
	
	public int stage;
	public List<Integer> enlargedAreaIndexes;
	private int expansionIndex;
	
	public boolean success;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryUpgradeStartPacket instantiate() {
		return new SanctuaryUpgradeStartPacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		stage = reader.readInt();
		
		int arraySize = reader.readInt();
		enlargedAreaIndexes = new ArrayList<Integer>();
		
		for(int i = 0; i < arraySize; i++)
		{
			enlargedAreaIndexes.add(reader.readInt());
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
			boolean isStageUpgrade = false;
			
			TwiggleItem updatedTwiggle = null;
			var classItemInvId = player.account.getPlayerInventory().getSanctuaryAccessor().getSanctuaryLook(player.account.getActiveSanctuaryLook())
					.get("components").getAsJsonObject()
					.get("SanctuaryLook").getAsJsonObject()
					.get("info").getAsJsonObject()
					.get("classInvId").getAsString();
			
			// If the stage has gone up, its a stage upgrade
			if(stage > player.account.getPlayerInventory().getSanctuaryAccessor().getCurrentSanctuaryStage(classItemInvId))
			{
				TwiggleWorkParameters workParams = new TwiggleWorkParameters();
				workParams.classItemInvId = classItemInvId;
				workParams.stage = stage;
				updatedTwiggle = twiggleAccessor.setTwiggleWork(TwiggleState.WorkingSanctuary, System.currentTimeMillis() + SanctuaryWorkCalculator.getTimeForStageUp(stage), workParams);		
				
				isStageUpgrade = true;
			}		
			//if the enlarged array has any elements that don't match the og elements
			else
			{
				boolean nonMatchingElement = false;
				int nonMatchingElementIndex = 0;
				JsonArray expansionArray = player.account.getPlayerInventory().getSanctuaryAccessor().getExpandedRooms(classItemInvId);
				
				for(int i = 0; i < enlargedAreaIndexes.size() && i < expansionArray.size(); i++)
				{
					if(enlargedAreaIndexes.get(i) != expansionArray.get(i).getAsInt())
					{
						nonMatchingElement = true;
						nonMatchingElementIndex = i;
						break;
					}
				}
				
				if(nonMatchingElement)
				{
					//its a room expand upgrade
					expansionIndex = nonMatchingElementIndex;
					
					TwiggleWorkParameters workParams = new TwiggleWorkParameters();
					workParams.classItemInvId = classItemInvId;
					workParams.enlargedAreaIndex = expansionIndex;
					updatedTwiggle = twiggleAccessor.setTwiggleWork(TwiggleState.WorkingSanctuary, System.currentTimeMillis() + SanctuaryWorkCalculator.getTimeForExpand(expansionIndex), workParams);		
				}
			}
			
			if(updatedTwiggle == null)
			{
				//failed to expand
				this.success = false;		
				client.sendPacket(this);
			}
			else
			{
				//remove resources from player
				
				if(isStageUpgrade)
				{
					var map = SanctuaryWorkCalculator.getCostForStageUp(stage);
					
					for(var item : map.entrySet())
					{
						player.account.getPlayerInventory().getItemAccessor(player).remove(item.getKey(), item.getValue());	
					}
				}
				else {
					var map = SanctuaryWorkCalculator.getCostForEnlargen(expansionIndex);
					
					for(var item : map.entrySet())
					{
						player.account.getPlayerInventory().getItemAccessor(player).remove(item.getKey(), item.getValue());	
					}
				}				
				
				InventoryItemPacket packet = new InventoryItemPacket();
				var array = new JsonArray();
				array.add(updatedTwiggle.toJsonObject());
				packet.item = array;
				client.sendPacket(packet);
				
				this.success = true;
				client.sendPacket(this);
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
