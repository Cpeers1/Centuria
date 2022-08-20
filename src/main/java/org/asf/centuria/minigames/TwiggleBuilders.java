package org.asf.centuria.minigames;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.packets.xt.gameserver.minigames.MinigameCurrency;
import org.asf.centuria.packets.xt.gameserver.minigames.MinigamePrize;;

public class TwiggleBuilders {
    
    public static void OnJoin(Player plr){
       MinigameCurrency currency = new MinigameCurrency();
       currency.Currency = 9514;
       plr.client.sendPacket(currency);
    }
    
    public static boolean HandleMessage(Player plr, String command, String data){
        XtReader rd = new XtReader(data);
		XtWriter pk = new XtWriter();
        pk.writeString("mm");
        pk.writeInt(-1);
        pk.writeString(command);

        switch (command){
            case "startLevel": {
                int level = rd.readInt();
                
                pk.writeInt(level);
                break;
            }
            case "endLevel": {
                GivePrize(plr);

                pk.writeInt(30);
                break;
            }
            default: {
                return true;
            }
        }

        pk.writeString(""); // Data suffix
        String msg = pk.encode();
        plr.client.sendPacket(msg);

        return true;
    }

    public static void GivePrize(Player plr){
        MinigamePrize prize = new MinigamePrize();
			prize.ItemDefId = "2327";
			prize.ItemCount = 15;
			prize.Given = true;
			prize.PrizeIndex1 = 6566;
			prize.PrizeIndex2 = 0;
			plr.client.sendPacket(prize);

			MinigamePrize prize1 = new MinigamePrize();
			prize1.ItemDefId = "2327";
			prize1.ItemCount = 15;
			prize1.Given = true;
			prize1.PrizeIndex1 = 6567;
			prize1.PrizeIndex2 = 1;
			plr.client.sendPacket(prize1);

			MinigamePrize prize2 = new MinigamePrize();
			prize2.ItemDefId = "2327";
			prize2.ItemCount = 15;
			prize2.Given = true;
			prize2.PrizeIndex1 = 6572;
			prize2.PrizeIndex2 = 2;
			plr.client.sendPacket(prize2);
    }

}

