package org.asf.centuria.minigames.games;

import java.util.Map;
import java.util.Objects;
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
import java.util.Vector;

import org.apache.logging.log4j.core.tools.picocli.CommandLine.Help.TextTable.Cell;
import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.joml.Vector2i;

import javassist.bytecode.ByteArray;

public class GameDizzywingDispatch extends AbstractMinigame {

    public String currentGameUUID;
    public GameState gameState;
    public int level = 1;
    public int score = 500;
    public int moveCount = 0;
    public int powerup = 0;

    static {
    }

    public class GameState {

        public class GridCell {
            public int tileHealth;
            public TileType TileType;
            public BoosterType Booster;

            public GridCell(int health, TileType tileType, BoosterType booster){
                tileHealth = health;
                TileType = tileType;
                Booster = booster;
            }
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
            DelayedDoubleTwinTrouble,
        }

        public enum TileType {
            AquaBird,
            BlueBird,
            GreenBird,
            PinkBird,
            PurpleBird,
            RedBird,
            SnowyBird,
            YellowBird,
            HatOrPurse,
            None
        }

        public Map<Vector2i, GridCell> grid;
        public Map<Vector2i, Boolean> visited;
        public Vector2i gridSize;
        public Integer moveCount;
        public List<TileType> spawnTiles;

        public GameState(){
            gridSize = new Vector2i(9, 9);
            spawnTiles = new ArrayList<TileType>(Arrays.asList(
            TileType.AquaBird,
            TileType.BlueBird,
            TileType.GreenBird,
            TileType.PinkBird,
            TileType.PurpleBird,
            TileType.RedBird,
            TileType.SnowyBird,
            TileType.YellowBird,
            TileType.HatOrPurse));
            InitializeGameBoard();
        }

        public GridCell GetCell(Vector2i pos)
        {
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return grid.get(pos);
            }
            return null;
        }
        
        public int CalculateBoardChecksum()
        {
            byte[] inArray = new byte[gridSize.x * gridSize.y];
            
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    inArray[x + gridSize.x * y] = GridCellByteValueForChecksum(new Vector2i(x, y));
                }
            }

            //Board checksum algorithm!
            String text = Base64.getEncoder().encodeToString(inArray);
            String text2 = currentGameUUID.toString();
            String text3 = moveCount.toString();
            return (text + text2 + text3).hashCode();
        }

        public byte GridCellByteValueForChecksum(Vector2i pos)
        {
            int byteValue = 0;
            GridCell cell = GetCell(pos);

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

            grid = new HashMap<>();
            Random randomizer = new Random(currentGameUUID.hashCode());

            for (int y = 0; y < gridSize.y; y++)
            {
                for (int x = 0; x < gridSize.x; x++)
                {
                    List<TileType> shuffledSpawnTiles = new ArrayList<TileType>(spawnTiles);
                    Collections.shuffle(shuffledSpawnTiles, randomizer);

                    for (TileType spawnTile : shuffledSpawnTiles)
                    {
                        if ((x < 2 || !(GetCell(new Vector2i(x - 1, y)).TileType == spawnTile) || !(GetCell(new Vector2i(x - 2, y)).TileType == spawnTile)) && (y < 2 || !(GetCell(new Vector2i(x, y - 1)).TileType == spawnTile) || !(GetCell(new Vector2i(x, y - 2)).TileType == spawnTile)))
                        {
                            grid.put(new Vector2i(x, y), new GridCell(0, spawnTile, BoosterType.None));
                            break;
                        }
                    }
                }
            }
            moveCount = 0;
        }

        public void CalculateMove(Vector2i pos1, Vector2i pos2){
            GridCell cell1 = GetCell(pos1);
            grid.put(pos1, GetCell(pos2));
            grid.put(pos2, cell1);

            visited = new HashMap<>();

            Random randomizer = new Random(currentGameUUID.hashCode());
            
            if(GetCell(pos1).Booster == BoosterType.None && GetCell(pos2).Booster == BoosterType.None){
                
                for(int x = 0; x < gridSize.x; x++){
                    for(int y = 0; y < gridSize.y; y++){
                        if(visited.get(new Vector2i(x, y)) == null){
                            floodFill(new Vector2i(x, y));
                        }
                    }
                }

                for(int x = 0; x < gridSize.x; x++){
                    Integer noGapsInColumn = 0;
                    for(int y = 0; y < gridSize.y; y++){
                        GridCell currCell = GetCell(new Vector2i(x, y));
                        if(currCell.TileType == TileType.None && currCell.Booster == BoosterType.None){
                            noGapsInColumn++;
                        } else {
                            grid.put(new Vector2i(x, y - noGapsInColumn), currCell);
                        }
                    }
                    for(int y = gridSize.y; y >= gridSize.y-noGapsInColumn; y--){
                        //grid.put(new Vector2i(x, y), new GridCell(0, spawnTiles.get(randomizer.nextInt(spawnTiles.size())), BoosterType.None));
                        grid.put(new Vector2i(x, y), new GridCell(0, TileType.HatOrPurse, BoosterType.None));
                    }
                }
                
            } else if (GetCell(pos1).Booster != BoosterType.None && GetCell(pos2).Booster != BoosterType.None){
                
            } else {
                
            }
        }
        
        // patterns are hardcoded
        public void floodFill(Vector2i pos){
            
            TileType refTileType = GetCell(pos).TileType;
            BoosterType newBooster = BoosterType.None;
            Boolean patternMatched = false;
            
            Integer noSameTypeNorth = 0;
            Integer noSameTypeSouth = 0;
            Integer noSameTypeEast = 0;
            Integer noSameTypeWest = 0;

            for(int x = pos.x+1; x < Math.min(pos.x+4, gridSize.x); x++){
                if(GetCell(new Vector2i(x, pos.y)).TileType == refTileType){
                    noSameTypeEast++;
                } else {
                    break;
                }
            }

            for(int x = pos.x-1; x > Math.max(pos.x-4, 0); x--){
                if(GetCell(new Vector2i(x, pos.y)).TileType == refTileType){
                    noSameTypeWest++;
                } else {
                    break;
                }
            }

            for(int y = pos.y+1; y < Math.min(pos.y+4, gridSize.y); y++){
                if(GetCell(new Vector2i(pos.x, y)).TileType == refTileType){
                    noSameTypeNorth++;
                } else {
                    break;
                }
            }

            for(int y = pos.y-1; y > Math.max(pos.y-4, 0); y--){
                if(GetCell(new Vector2i(pos.x, y)).TileType == refTileType){
                    noSameTypeSouth++;
                } else {
                    break;
                }
            }

            // L shape, boom bird
            if((noSameTypeNorth == 2 && noSameTypeEast == 2) ||
                (noSameTypeNorth == 2 && noSameTypeWest == 2) ||
                (noSameTypeSouth == 2 && noSameTypeEast == 2) ||
                (noSameTypeSouth == 2 && noSameTypeWest == 2)){
                    newBooster = BoosterType.ColorBomb;
                    patternMatched = true;
                }
            
            // T shape, boom bird
            if((noSameTypeNorth == 2 && noSameTypeEast == 1 && noSameTypeWest == 1) ||
                (noSameTypeSouth == 2 && noSameTypeEast == 1 && noSameTypeWest == 1) ||
                (noSameTypeEast == 2 && noSameTypeNorth == 1 && noSameTypeSouth == 1) ||
                (noSameTypeWest == 2 && noSameTypeNorth == 1 && noSameTypeSouth == 1)){
                    newBooster = BoosterType.ColorBomb;
                    patternMatched = true;
                }

            // match 5, prism peacock
            if(noSameTypeNorth == 4 || noSameTypeEast == 4){
                newBooster = BoosterType.TwinTrouble;
                patternMatched = true;
            }
            
            // vertical line, buzzy bird
            if(noSameTypeNorth == 3){
                newBooster = BoosterType.FlyerVertical;
                patternMatched = true;
            }

            // vertical line, buzzy bird
            if(noSameTypeEast == 3){
                newBooster = BoosterType.FlyerHorizontal;
                patternMatched = true;
            }
            
            // match 3
            if(noSameTypeNorth == 2 || noSameTypeEast == 2){
                patternMatched = true;
            }

            if(patternMatched){
                for(int x = pos.x-noSameTypeWest; x <= pos.x+noSameTypeEast; x++){
                    grid.put(new Vector2i(x, pos.y), new GridCell(0, TileType.None, BoosterType.None));
                    visited.put(new Vector2i(x, pos.y), true);
                }
    
                for(int y = pos.y-noSameTypeSouth; y <= pos.y+noSameTypeNorth; y++){
                    grid.put(new Vector2i(pos.x, y), new GridCell(0, TileType.None, BoosterType.None));
                    visited.put(new Vector2i(pos.x, y), true);
                }

                if(newBooster != BoosterType.None){
                    grid.put(pos, new GridCell(0, refTileType, newBooster));
                }
            }

            

            return;

        }

        public String toBase64String(){
            byte[] inArray = new byte[gridSize.x * gridSize.y];
            
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    inArray[x + gridSize.x * y] = GridCellByteValueForBase64String(new Vector2i(x, y));
                }
            }

            return Base64.getEncoder().encodeToString(inArray);
        }

        public byte GridCellByteValueForBase64String(Vector2i pos)
        {
            int byteValue = 0;
            GridCell cell = GetCell(pos);

            if(cell.TileType == TileType.HatOrPurse){
                byteValue += 18;
            } else if(cell.TileType == TileType.None){
                byteValue += 255;
            } else {
                byteValue += (2 * cell.TileType.ordinal() + (cell.tileHealth == 0 ? 0 : 1));
                byteValue += cell.Booster.ordinal() * 20;
            }

            
            return (byte)byteValue;
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
        gameState = new GameState();

        XtWriter mmData = new XtWriter();
        mmData.writeString(currentGameUUID);
        mmData.writeInt(gameState.CalculateBoardChecksum()); // this is a checksum, not a timestamp
        mmData.writeInt(level);
        mmData.writeInt(0);
        mmData.writeInt(score);
        mmData.writeInt(0);

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "startGame";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);
        
        
        if (Centuria.debugMode) {
            DDVis vis = new DDVis();
            new Thread(() -> vis.frame.setVisible(true)).start();
        }

        syncClient(player, rd, moveCount, level, score, powerup, gameState);

    }

    @MinigameMessage("move")
	public void move(Player player, XtReader rd) {

        int clientChecksum = rd.readInt();

        // process move sent by client
        gameState.CalculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()));
        syncClient(player, rd, moveCount, level, score, powerup, gameState);

        InventoryItemPacket pk = new InventoryItemPacket();
        pk.item = player.account.getSaveSpecificInventory().getItem("303");
        player.client.sendPacket(pk);

        if (Centuria.debugMode) {
            level++;
            goToLevel(player, level, score);
        }

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
        // send start game client with previous values
    }

    @MinigameMessage("saveGame")
	public void saveGame(Player player, XtReader rd) {

    }

    @MinigameMessage("redeemPiece")
	public void redeemPiece(Player player, XtReader rd) {

    }

    @MinigameMessage("syncClient")
	public void syncClient(Player player, XtReader rd, int moveCount, int level, int score, int powerup, GameState gameState) {
        XtWriter mmData = new XtWriter();
        mmData.writeInt(moveCount);
        mmData.writeInt(level);
        mmData.writeInt(score);
        mmData.writeInt(powerup);
        mmData.writeString(gameState.toBase64String());
        //mmData.writeString("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9Q");
        mmData.writeInt(0); // no level objective

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "syncClient";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);
    }
    
}
