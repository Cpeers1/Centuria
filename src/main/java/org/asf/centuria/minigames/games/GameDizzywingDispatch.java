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

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.joml.Vector2i;

public class GameDizzywingDispatch extends AbstractMinigame{

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
        public boolean visited[][];
        public Vector2i gridSize;
        public Integer moveCount;
        public List<TileType> spawnTiles;
        public Random randomizer;

        public GameState(){
            gridSize = new Vector2i(9, 9);
            spawnTiles = new ArrayList<TileType>(Arrays.asList(
            TileType.GreenBird,
            TileType.PurpleBird,
            TileType.RedBird,
            TileType.SnowyBird,
            TileType.YellowBird));
            randomizer = new Random(currentGameUUID.hashCode());
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

        public void SetCell(Vector2i pos, GridCell cell){
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y && cell != null)
            {
                grid.put(pos, cell);
            }
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
                            SetCell(new Vector2i(x, y), new GridCell(0, spawnTile, BoosterType.None));
                            break;
                        }
                    }
                }
            }
            moveCount = 0;
        }

        public void CalculateMove(Vector2i pos1, Vector2i pos2){

            GridCell cell1 = GetCell(pos1);
            SetCell(pos1, GetCell(pos2));
            SetCell(pos2, cell1);

            floodFillClearVisited();

            if(GetCell(pos2).Booster.ordinal() > GetCell(pos1).Booster.ordinal()){
                Vector2i temp = pos1;
                pos1 = pos2;
                pos2 = temp;
            }

            if(GetCell(pos1).Booster != BoosterType.None && GetCell(pos2).Booster != BoosterType.None){
            }
            else if(GetCell(pos1).Booster != BoosterType.None && GetCell(pos2).Booster == BoosterType.None){
                
                if(GetCell(pos1).Booster == BoosterType.FlyerHorizontal){
                    for(int x = 0; x < gridSize.x; x++){
                        SetCell(new Vector2i(x, pos1.y), new GridCell(0, TileType.None, BoosterType.None));
                    }
                    fillGaps();
                } else if(GetCell(pos1).Booster == BoosterType.FlyerVertical){
                    for(int y = 0; y < gridSize.y; y++){
                        SetCell(new Vector2i(pos1.x, y), new GridCell(0, TileType.None, BoosterType.None));
                    }
                    fillGaps();
                } else if(GetCell(pos1).Booster == BoosterType.ColorBomb){
                }
            }

            for(Integer evaluationDepth = 0; evaluationDepth < 5; evaluationDepth++){
                Boolean isMatches = false;

                for(int x = 0; x < gridSize.x; x++){
                    for(int y = 0; y < gridSize.y; y++){
                        if(!floodFillGetVisited(new Vector2i(x, y))){
                            isMatches ^= floodFill(new Vector2i(x, y), pos1, pos2);
                        }
                    }
                }
                fillGaps();

                if(!isMatches){
                    break;
                }
            }
                
        }

        public void fillGaps(){
            for(int x = 0; x < gridSize.x; x++){
                Integer noGapsInColumn = 0;
                for(int y = 0; y < gridSize.y; y++){
                    GridCell currCell = GetCell(new Vector2i(x, y));
                    if(currCell.TileType == TileType.None && currCell.Booster == BoosterType.None){
                        noGapsInColumn++;
                    } else {
                        SetCell(new Vector2i(x, y - noGapsInColumn), currCell);
                    }
                }
                for(int y = gridSize.y; y >= gridSize.y-noGapsInColumn; y--){
                    SetCell(new Vector2i(x, y), new GridCell(0, spawnTiles.get(randomizer.nextInt(spawnTiles.size())), BoosterType.None));
                }
            }
        }

        public Boolean isNeighbourCell(Vector2i pos1, Vector2i pos2){
            GridCell cell1 = GetCell(pos1);
            GridCell cell2 = GetCell(pos2);

            if(cell1 == null || cell2 == null) return false;
            if(cell1.TileType == cell2.TileType && cell1.Booster == cell2.Booster) return true;

            return false;
        }

        public void floodFillSetVisited(Vector2i pos){
            visited[pos.x][pos.y] = true;
        }

        public boolean floodFillGetVisited(Vector2i pos){
            return visited[pos.x][pos.y];
        }

        public void floodFillClearVisited(){
            visited = new boolean[gridSize.x][gridSize.y];
        }
        
        // patterns are hardcoded
        public Boolean floodFill(Vector2i pos, Vector2i swappedTile1, Vector2i swappedTile2){
            
            Queue<Vector2i> floodFillQueue = new LinkedList<>();
            floodFillQueue.add(pos);

            List<Vector2i> connectedNodes = new LinkedList<>();
            String nodePattern = "";

            while(!floodFillQueue.isEmpty()){
                Vector2i curr = floodFillQueue.poll();

                if(GetCell(curr) == null) continue;
                if(floodFillGetVisited(curr)) continue;
                if(GetCell(pos).TileType != GetCell(curr).TileType) continue;
                if(GetCell(pos).Booster != GetCell(curr).Booster) continue;

                floodFillSetVisited(curr);
                connectedNodes.add(curr);

                Integer horizontalConnections = 0;
                Integer verticalConnections = 0;

                if(isNeighbourCell(curr, new Vector2i(curr.x-1, curr.y))) horizontalConnections++; floodFillQueue.add(new Vector2i(curr.x-1, curr.y));
                if(isNeighbourCell(curr, new Vector2i(curr.x+1, curr.y))) horizontalConnections++; floodFillQueue.add(new Vector2i(curr.x+1, curr.y));
                if(isNeighbourCell(curr, new Vector2i(curr.x, curr.y-1))) verticalConnections++; floodFillQueue.add(new Vector2i(curr.x, curr.y-1));
                if(isNeighbourCell(curr, new Vector2i(curr.x, curr.y+1))) verticalConnections++; floodFillQueue.add(new Vector2i(curr.x, curr.y+1));

                if(horizontalConnections == 1 && verticalConnections == 0) nodePattern += "C";
                else if(horizontalConnections == 0 && verticalConnections == 1) nodePattern += "U";
                else if(horizontalConnections == 2 && verticalConnections == 0) nodePattern += "-";
                else if(horizontalConnections == 0 && verticalConnections == 2) nodePattern += "|";
                else if(horizontalConnections == 1 && verticalConnections == 1) nodePattern += "L";
                else if((horizontalConnections == 2 && verticalConnections == 1) || (horizontalConnections == 1 && verticalConnections == 2)) nodePattern += "T";
            }

            if (nodePattern.length() > 2) Centuria.logger.info(nodePattern + " " + GetCell(pos).TileType + " " + GetCell(pos).Booster + " " + pos.toString());

            return true;
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
            } else if(cell.TileType != TileType.None){
                byteValue += ((cell.Booster == BoosterType.None ? 2 : 1) * cell.TileType.ordinal() + (cell.tileHealth == 0 ? 0 : 1));
            }
            
            byteValue += cell.Booster.ordinal() * 20;
            
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

        syncClient(player, rd);

    }

    @MinigameMessage("move")
	public void move(Player player, XtReader rd) {

        int clientChecksum = rd.readInt();

        // process move sent by client
        gameState.CalculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()));
        syncClient(player, rd);

        InventoryItemPacket pk = new InventoryItemPacket();
        pk.item = player.account.getSaveSpecificInventory().getItem("303");
        player.client.sendPacket(pk);

        if (Centuria.debugMode) {
            level++;
            goToLevel(player);
        }

    }

	private void goToLevel(Player player) {
        // change to next level
        XtWriter mmData = new XtWriter();
        mmData.writeInt(level);
        mmData.writeInt(0);
        mmData.writeInt(score);
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
	public void syncClient(Player player, XtReader rd) {
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
