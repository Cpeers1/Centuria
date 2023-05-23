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
    public int score = 0;
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
            ColorBomb,
            TwinTrouble
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
        public int visited[][];
        public Vector2i gridSize;
        public Integer moveCount;
        public List<TileType> spawnTiles;
        public Random randomizer;
        public int matchComboScore;

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
            floodFillClearVisited();
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

        public void ClearCell(Vector2i pos, Boolean isScore){
            SetCell(pos, new GridCell(0, TileType.None, BoosterType.None));
            if(isScore) score += 30;
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

        public void CalculateMove(Vector2i pos1, Vector2i pos2, Player player, XtReader rd){

            GridCell cell1 = GetCell(pos1);
            SetCell(pos1, GetCell(pos2));
            SetCell(pos2, cell1);

            floodFillClearVisited();

            if(GetCell(pos2).Booster.ordinal() > GetCell(pos1).Booster.ordinal()){
                Vector2i temp = pos1;
                pos1 = pos2;
                pos2 = temp;
            }

            if(GetCell(pos1).Booster != BoosterType.None && GetCell(pos2).Booster != BoosterType.None){ // booster combo behaviours
                
            } else if(GetCell(pos1).Booster != BoosterType.None && GetCell(pos2).Booster == BoosterType.None){    // single booster behaviours
                
                if(GetCell(pos1).Booster == BoosterType.FlyerHorizontal){
                    HorizontalBuzzyBirdBehaviour(pos1);
                } else if(GetCell(pos1).Booster == BoosterType.FlyerVertical){
                    VerticalBuzzyBirdBehaviour(pos1);
                } else if(GetCell(pos1).Booster == BoosterType.ColorBomb){
                    ColorBombBehaviour(pos1);
                } else if (GetCell(pos1).Booster == BoosterType.TwinTrouble){
                    PrismPeacockBehaviour(pos2);
                }
            }

            matchComboScore = 0;

            while(true){ // for as long as there are still matches on the game board

                Boolean isMatches = findAndClearMatches();
                fillGaps(); // replace tiles marked as having being cleared

                if(!isMatches){
                    break;
                }

            }
                
        }

        private void HorizontalBuzzyBirdBehaviour(Vector2i pos1) {
            for(int x = 0; x < gridSize.x; x++){
                ClearCell(new Vector2i(x, pos1.y), true);
            }
            fillGaps();
        }

        private void VerticalBuzzyBirdBehaviour(Vector2i pos1) {
            for(int y = 0; y < gridSize.y; y++){
                ClearCell(new Vector2i(pos1.x, y), true);
            }
            fillGaps();
        }

        private void ColorBombBehaviour(Vector2i pos1) {
            for(int x = pos1.x-1; x <= pos1.x+1; x++){
                for(int y = pos1.y-1; y <= pos1.y+1; y++){
                    ClearCell(new Vector2i(x, y), true);
                }
            }
            SetCell(new Vector2i(pos1.x, Math.max(0, pos1.y-1)), new GridCell(0, TileType.RedBird, BoosterType.ColorBomb));
            fillGaps();
            for(int x = pos1.x-1; x <= pos1.x+1; x++){
                for(int y = pos1.y-2; y <= pos1.y; y++){
                    ClearCell(new Vector2i(x, y), true);
                }
            }
            fillGaps();
        }

        private void PrismPeacockBehaviour(Vector2i pos2) {
            TileType refType = GetCell(pos2).TileType;
            for(int y = 0; y < gridSize.y; y++){
                for(int x = 0; x < gridSize.x; x++){
                    if(GetCell(new Vector2i(x, y)).TileType == refType) ClearCell(new Vector2i(x, y), true);
                }
            }
            fillGaps();
        }

        public void fillGaps(){
            for(int x = 0; x < gridSize.x; x++){
                Integer noGapsInColumn = 0;
                for(int y = 0; y < gridSize.y; y++){ // for each column
                    GridCell currCell = GetCell(new Vector2i(x, y));
                    if(currCell.TileType == TileType.None && currCell.Booster == BoosterType.None){
                        noGapsInColumn++;
                    } else {
                        SetCell(new Vector2i(x, y - noGapsInColumn), currCell); // move all the tiles down to eliminate gaps
                    }
                }
                for(int y = gridSize.y; y >= gridSize.y-noGapsInColumn; y--){
                    SetCell(new Vector2i(x, y), new GridCell(0, spawnTiles.get(randomizer.nextInt(spawnTiles.size())), BoosterType.None)); // now fill the gaps
                }
            }
        }

        public Boolean floodFillIsNeighbourCell(Vector2i pos1, Vector2i pos2){
            GridCell cell1 = GetCell(pos1);
            GridCell cell2 = GetCell(pos2);

            if(cell1 == null || cell2 == null) return false;
            if(cell1.TileType == cell2.TileType && cell1.Booster == cell2.Booster) return true;

            return false;
        }

        // 0 = unvisited
        // 1 = visited
        // 3 = match 3 (unvisited)
        // 4 = match 4 (unvisited)
        // 5 = match 5 (unvisited)
        // 6 = intersection of match 3s (unvisited)
        public void floodFillSetMatch(Vector2i pos, int matchType){
            visited[pos.x][pos.y] = matchType;
        }

        public int floodFillGetMatch(Vector2i pos){
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return visited[pos.x][pos.y];
            }
            return 0;
        }

        public void floodFillSetVisited(Vector2i pos){
            visited[pos.x][pos.y] = 1;
        }

        public boolean floodFillGetVisited(Vector2i pos){
            return visited[pos.x][pos.y] == 1;
        }

        public void floodFillClearVisited(){
            visited = new int[gridSize.x][gridSize.y];
        }

        public void floodFill(Vector2i pos){
            
            TileType refType = GetCell(pos).TileType;
            BoosterType refBooster = BoosterType.None;

            if(floodFillGetMatch(pos) == 3) {
                matchComboScore += 30; score += matchComboScore;
            }
            else if(floodFillGetMatch(pos) == 4) {
                refBooster = randomizer.nextInt(2) == 0 ? BoosterType.FlyerHorizontal : BoosterType.FlyerVertical;
                matchComboScore += 150; score += matchComboScore;
            }
            else if(floodFillGetMatch(pos) == 5) {
                refBooster = BoosterType.TwinTrouble;
                refType = TileType.None;
                matchComboScore += 150; score += matchComboScore;
            }
            else if(floodFillGetMatch(pos) == 6) {
                refBooster = BoosterType.ColorBomb;
                matchComboScore += 150; score += matchComboScore;
            }
            
            Queue<Vector2i> floodFillQueue = new LinkedList<>();
            floodFillQueue.add(pos);

            List<Vector2i> connectedNodes = new LinkedList<>();

            while(!floodFillQueue.isEmpty()){
                Vector2i curr = floodFillQueue.poll();

                if(GetCell(curr) == null) continue;
                if(floodFillGetMatch(curr) <= 1) continue;
                if(GetCell(pos).TileType != GetCell(curr).TileType) continue;
                if(GetCell(pos).Booster != GetCell(curr).Booster) continue;

                floodFillSetVisited(curr);
                connectedNodes.add(curr);

                floodFillQueue.add(new Vector2i(curr.x-1, curr.y));
                floodFillQueue.add(new Vector2i(curr.x+1, curr.y));
                floodFillQueue.add(new Vector2i(curr.x, curr.y-1));
                floodFillQueue.add(new Vector2i(curr.x, curr.y+1));
            }

           
            for(Vector2i node : connectedNodes){
                ClearCell(node, false);
            }
            if(refBooster != BoosterType.None){
                SetCell(pos, new GridCell(0, refType, refBooster));
            }

        }
        
        // patterns are hardcoded
        public Boolean findAndClearMatches(){

            Boolean isMatch = false;

            // find all vertical matches
            for(int x = 0; x < gridSize.x; x++){
                Integer sameTilesColumn = 1;
                for(int y = 0; y < gridSize.y; y++){ // for each column
                    if(floodFillIsNeighbourCell(new Vector2i(x, y), new Vector2i(x, y+1))){ // keep track of number of tiles with the same tile type next to each other
                        sameTilesColumn++;
                        //System.out.print(sameTilesColumn + " " + x + " " + y + "\n");
                    } else if (sameTilesColumn > 2) {
                        //System.out.print(sameTilesColumn + " " + x + " " + y + "\n");
                        isMatch = true;
                        
                        for(int backtrack = y-sameTilesColumn+1; backtrack <= y; backtrack++){
                            floodFillSetMatch(new Vector2i(x, backtrack), sameTilesColumn); // record this number
                        }
                        sameTilesColumn = 1;
                    } else {
                        sameTilesColumn = 1;
                    }
                }
            }
            
            // find all horizontal matches
            for(int y = 0; y < gridSize.y; y++){
                Integer sameTilesRow = 1;
                for(int x = 0; x < gridSize.x; x++){ // for each column
                    if(floodFillIsNeighbourCell(new Vector2i(x+1, y), new Vector2i(x, y))){ // keep track of number of tiles with the same tile type next to each other
                        sameTilesRow++;
                        //System.out.print(sameTilesRow + " " + x + " " + y + "\n");
                    } else if (sameTilesRow > 2) {
                        //System.out.print(sameTilesRow + " " + x + " " + y + "\n");
                        isMatch = true;

                        for(int backtrack = x-sameTilesRow+1; backtrack <= x; backtrack++){
                            Integer horizontalConnections = 0;
                            Integer verticalConnections = 0;
                            
                            if(floodFillGetMatch(new Vector2i(backtrack-1, y)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack-1, y))) horizontalConnections++;
                            if(floodFillGetMatch(new Vector2i(backtrack+1, y)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack+1, y))) horizontalConnections++;
                            if(floodFillGetMatch(new Vector2i(backtrack, y-1)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack, y-1))) verticalConnections++;
                            if(floodFillGetMatch(new Vector2i(backtrack, y+1)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack, y+1))) verticalConnections++;

                            if(horizontalConnections != 0 && verticalConnections != 0){
                                floodFillSetMatch(new Vector2i(backtrack, y), 6); // same as before, but check if this is the intersection of 2 lines
                            } else {
                                floodFillSetMatch(new Vector2i(backtrack, y), sameTilesRow);
                            }
                        }
                        sameTilesRow = 1;
                    } else {
                        sameTilesRow = 1;
                    }
                }
            }

            // clear all T shaped and L shaped matches
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    if(floodFillGetMatch(new Vector2i(x, y)) == 6) floodFill(new Vector2i(x, y));
                }
            }

            // clear all line-shaped matches
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    if(floodFillGetMatch(new Vector2i(x, y)) > 2) floodFill(new Vector2i(x, y));
                }
            }

            return isMatch;
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
                byteValue = 18;
            } else if (cell.Booster == BoosterType.TwinTrouble){
                byteValue = 88;
            } else {
                byteValue += ((cell.Booster == BoosterType.None ? 2 : 1) * cell.TileType.ordinal() + (cell.tileHealth == 0 ? 0 : 1));
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

        syncClient(player, rd);

    }

    @MinigameMessage("move")
	public void move(Player player, XtReader rd) {

        int clientChecksum = rd.readInt();

        // process move sent by client
        gameState.CalculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()), player, rd);
        syncClient(player, rd);

        InventoryItemPacket pk = new InventoryItemPacket();
        pk.item = player.account.getSaveSpecificInventory().getItem("303");
        player.client.sendPacket(pk);

        if ((int)Math.log(score) > level) {
            level = (int)Math.log(score);
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
        //mmData.writeString("8/T19vf4+fr7/P3+/w==");
        mmData.writeInt(0); // no level objective

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "syncClient";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);
    }
    
}
