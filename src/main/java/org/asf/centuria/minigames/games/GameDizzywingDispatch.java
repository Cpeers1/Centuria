package org.asf.centuria.minigames.games;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.joml.Vector2i;

public class GameDizzywingDispatch extends AbstractMinigame {

    public String currentGameUUID;
    public Match3GameBoard gameState;
    public int level = 1;
    public int value = 500;

    static {
    }

    public class Match3GameBoard {

        public class Match3Cell {
            public int tileHealth;
            public Vector2i cellPos;
            public Match3TileType TileType;
            public BoosterType Booster;
        }

        public enum BoosterType
        {
            None,
            FlyerHorizontal,
            FlyerVertical,
            TwinTrouble,
            ColorBomb,
            Combo_DoubleFlyer,
            Combo_FlyerTwinTrouble,
            Combo_FlyerColorBomb,
            Combo_DoubleTwinTrouble,
            Combo_TwinTroubleColorBomb,
            Combo_DoubleColorBomb,
            DelayedTwinTrouble,
            DelayedDoubleTwinTrouble
        }

        public enum Match3TileType {
            RedBird,
            YellowBird,
            GreenBird,
            BlueBird,
            PurpleBird,
            WhiteBird,
            Hat,
            Purse
        }

        public Match3Cell[][] grid;
        public Vector2i gridSize;
        public Integer moveCount;
        private List<Match3TileType> spawnTiles;

        public Match3GameBoard(){
            gridSize = new Vector2i(9, 9);
            spawnTiles = new ArrayList<Match3TileType>(Arrays.asList(
                Match3TileType.RedBird,
                Match3TileType.YellowBird,
                Match3TileType.GreenBird,
                Match3TileType.PurpleBird,
                Match3TileType.WhiteBird));
            InitializeCells();
            InitializeGameBoard();
        }

        public Match3Cell GetCell(Vector2i inCellPos)
        {
            return GetCell(inCellPos.x, inCellPos.y);
        }

        public Match3Cell GetCell(int x, int y)
        {
            if (x >= 0 && x < grid.length && y >= 0 && y < grid[0].length)
            {
                return grid[x][y];
            }
            return null;
        }

        private void InitializeCells()
        {
            grid = new Match3Cell[gridSize.x][gridSize.y];
            for (int y = 0; y < gridSize.y; y++)
            {
                for (int x = 0; x < gridSize.x; x++)
                {
                    Match3Cell cell = new Match3Cell();
                    cell.tileHealth = 0;
                    cell.Booster = BoosterType.None;
                    cell.cellPos = new Vector2i(x, y);
                    grid[x][y] = cell;
                }
            }
        }
        
        public int CalculateBoardChecksum()
        {
            byte[] inArray = new byte[gridSize.x * gridSize.y];
            
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    inArray[x + gridSize.x * y] = GridCellByteValueForChecksum(x, y);
                }
            }

            //Board checksum algorithm!
            String text = Base64.getEncoder().encodeToString(inArray);
            String text2 = currentGameUUID.toString();
            String text3 = moveCount.toString();
            return (text + text2 + text3).hashCode();
        }

        public byte GridCellByteValueForChecksum(int col, int row)
        {
            int byteValue = 0;
            Match3Cell cell = GetCell(col, row);

            int typeValueOnServer = cell.TileType.ordinal();

            boolean isBoosted = cell.Booster != BoosterType.None;
            if (isBoosted)
            {
                byteValue += typeValueOnServer;
            } else {
                byteValue += typeValueOnServer * 2 + cell.tileHealth;
            }

            return (byte)byteValue;
        }

        private void InitializeGameBoard()
        {

            InitializeCells();
            Random randomizer = new Random(currentGameUUID.hashCode());

            for (int y = 0; y < gridSize.y; y++)
            {
                for (int x = 0; x < gridSize.x; x++)
                {
                    Collections.shuffle(spawnTiles);
                    for (Match3TileType spawnTile : spawnTiles)
                    {
                        if ((x < 2 || !(GetCell(x - 1, y).TileType == spawnTile) || !(GetCell(x - 2, y).TileType == spawnTile)) && (y < 2 || !(GetCell(x, y - 1).TileType == spawnTile) || !(GetCell(x, y - 2).TileType == spawnTile)))
                        {
                            grid[x][y].TileType = spawnTile;
                            break;
                        }
                    }
                }
            }
            moveCount = 0;
        }

        public void CalculateMove(Vector2i cell1, Vector2i cell2){

        }
    
    }

    @Override
	public AbstractMinigame instantiate() {
		return new GameDizzywingDispatch();
	}

    @Override
	public boolean canHandle(int levelID) {
		return levelID == 8192;
	}

    @Override
	public void onJoin(Player plr) {
	}

    @Override
	public void onExit(Player plr) {
	}

    @MinigameMessage("startGame")
	public void startGame(Player player, XtReader rd) {
        
        
        // send timestamp to start a game
        currentGameUUID = UUID.randomUUID().toString();
        level = 1;
        gameState = new Match3GameBoard();

        XtWriter mmData = new XtWriter();
        mmData.writeString(currentGameUUID);
        mmData.writeInt(gameState.CalculateBoardChecksum()); // this is a checksum, not a timestamp
        mmData.writeInt(level);
        mmData.writeInt(0);
        mmData.writeInt(value);
        mmData.writeInt(0);

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "startGame";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);


        if (Centuria.debugMode) {
            DDVis vis = new DDVis();
            new Thread(() -> vis.frame.setVisible(true)).start();
        }

    }

    @MinigameMessage("move")
	public void move(Player player, XtReader rd) {

        int clientChecksum = rd.readInt();

        // process move sent by client
        gameState.CalculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()));

        InventoryItemPacket pk = new InventoryItemPacket();
        pk.item = player.account.getSaveSpecificInventory().getItem("303");
        player.client.sendPacket(pk);

        if (Centuria.debugMode) {
            level++;
            goToLevel(player, level, value);
        }
        //System.out.println(rd.readRemaining());

    }

	private void goToLevel(Player player, int level, int value) {
        // change to next level
        XtWriter mmData = new XtWriter();
        mmData.writeInt(level);
        mmData.writeInt(0);
        mmData.writeInt(value);
        mmData.writeInt(0);

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "goToLevel";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);

    }

    @MinigameMessage("dizzyBird")
    public void dizzyBird(Player player, XtReader rd) {
        // send timestamp to start a game
        currentGameUUID = UUID.randomUUID().toString();

        XtWriter mmData = new XtWriter();
        mmData.writeLong(System.currentTimeMillis() / 1000); // this is a checksum, not a timestamp

        MinigameMessagePacket mm = new MinigameMessagePacket();
        mm.command = "dizzyBird";
        mm.data = mmData.encode().substring(4);
        player.client.sendPacket(mm);
    }

    @MinigameMessage("continueGame")
	public void continueGame(Player player, XtReader rd) {

    }

    @MinigameMessage("saveGame")
	public void saveGame(Player player, XtReader rd) {

    }

    @MinigameMessage("redeemPiece")
	public void redeemPiece(Player player, XtReader rd) {

    }

    @MinigameMessage("syncClient")
	public void syncClient(Player player, XtReader rd) {

    }
    
}
