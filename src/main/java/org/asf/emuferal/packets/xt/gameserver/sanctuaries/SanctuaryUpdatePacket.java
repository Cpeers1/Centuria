package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.objects.SancObjectInfo;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;

public class SanctuaryUpdatePacket implements IXtPacket<SanctuaryUpdatePacket> {
	
	public class UpdateSancObjectItem
	{
		public String objectId;
		public SancObjectInfo objectInfo;
		public JsonObject furnitureObject;
	}
	
	public int numOfUpdates;
	public boolean success;
	public List<UpdateSancObjectItem> updates;
	public List<UpdateSancObjectItem> additions = new ArrayList<UpdateSancObjectItem>();
	public List<UpdateSancObjectItem> removals = new ArrayList<UpdateSancObjectItem>();
		

	@Override
	public String id() {
		return "ssu";
	}

	@Override
	public SanctuaryUpdatePacket instantiate() {
		return new SanctuaryUpdatePacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		
		updates = new ArrayList<UpdateSancObjectItem>();
		
		//first number is how many ssu's there really are
		numOfUpdates = reader.readInt();
		
		for(int i = 0; i < numOfUpdates; i++)
		{
			UpdateSancObjectItem item = new UpdateSancObjectItem();
			
			//next line is placementId
			item.objectId = reader.read();
			
			//then its x y z 
			SancObjectInfo info = new SancObjectInfo();
			info.x = reader.readDouble();
			info.y = reader.readDouble();
			info.z = reader.readDouble();
			
			//then rot x y z w
			info.rotX = reader.readDouble();
			info.rotY = reader.readDouble();
			info.rotZ = reader.readDouble();
			info.rotW = reader.readDouble();
			
			//then it's uhm.. probably grid id then state id
			info.gridId = reader.readInt();
			info.state = reader.readInt();
			
			//then its some garbage, prep for next read
			reader.read(); //empty
			reader.read(); //-1
			
			item.objectInfo = info;
			updates.add(item);
		}
		
		//finished reading, the rest afterwards from what I know is garbage
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		
		//test this..
		
		writer.writeInt(-1); // Data prefix
		
		writer.writeBoolean(true);
		writer.writeInt(additions.size()); //number of item additions

		if(additions.size() != 0)
		{
			for(var item : additions)
			{
				writer.writeString(item.objectId);
			}
		}
		
		writer.writeInt(removals.size());
		
		if(removals.size() != 0)
		{
			for(var item : removals)
			{
				writer.writeString(item.objectId);
			}						
		}

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Switch sanctuary look
		Player plr = (Player) client.container;
		
		//for now, all I know is about additions
		additions.addAll(updates);
		
		for(var item : additions)
		{
			plr.account.getPlayerInventory().getSanctuaryAccessor().addSanctuaryObject(item.objectId, item.objectInfo, plr.activeSanctuaryLook);		
		}

		//uhh yeah ok
		//send il and sanctuaryUpdatePacket response for the main player
		
		var il = plr.account.getPlayerInventory().getItem("201");
		var ilPacket = new InventoryItemPacket();
		ilPacket.item = il;
		
		XtWriter writer = new XtWriter();
		ilPacket.build(writer);
		
		//send IL
		plr.client.sendPacket(writer.encode());
		
		//then do this packet
		
		writer = new XtWriter();
		this.build(writer);
		
		plr.client.sendPacket(writer.encode());
		
		SendFunitureUpdatePackets(client);

		return true;
	}
	
	public void SendFunitureUpdatePackets(SmartfoxClient client) {
		
		for(var update : updates)
		{
			var owner = (Player)client.container;
			var furnItem = owner.account.getPlayerInventory().getFurnitureAccessor().getFurnitureData(update.objectId);
			
			//now do an OI packet
			for (Player player : ((GameServer) client.getServer()).getPlayers()) {				
				if (player.room.equals("sanctuary_" + owner.account.getAccountID())) {			
					// Send packet
					XtWriter wr = new XtWriter();
					wr.writeString("oi");
					wr.writeInt(-1); // data prefix

					// Object creation parameters
					wr.writeString(update.objectId); // World object ID
					wr.writeInt(1751);
					wr.writeString(player.room.substring("sanctuary_".length())); // Owner ID

					// Object info
					wr.writeInt(0);
					wr.writeLong(System.currentTimeMillis() / 1000);
					wr.writeDouble(update.objectInfo.x);
					wr.writeDouble(update.objectInfo.y);
					wr.writeDouble(update.objectInfo.z);
					wr.writeDouble(update.objectInfo.rotX);
					wr.writeDouble(update.objectInfo.rotY);
					wr.writeDouble(update.objectInfo.rotZ);
					wr.writeDouble(update.objectInfo.rotW);
					wr.writeString("0%0%0%0.0%0%2"); // idk tbh
					
					// Only send json if its not the owner

					
					if (!player.account.getAccountID().equals(owner.account.getAccountID()))
						wr.writeString(furnItem.toString());
					wr.writeString(String.valueOf(update.objectInfo.gridId)); // grid
					wr.writeString(furnItem.get("parentItemId").getAsString()); // parent item ??
					wr.writeString(update.objectInfo.toString()); // state
					wr.writeString(""); // data suffix
					String pk = wr.encode();
					player.client.sendPacket(pk);		
				}
			}	
			
			// Log
			if (System.getProperty("debugMode") != null) {
				System.out.println("[SANCTUARY] [LOAD]  Server to client: load object (id: " + update
						+ ", type: furniture, defId: " + furnItem.get("defId").getAsString() + ")");
			}
		}
	}

}
