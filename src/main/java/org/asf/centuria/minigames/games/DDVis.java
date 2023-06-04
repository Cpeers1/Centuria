package org.asf.centuria.minigames.games;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.minigames.games.GameDizzywingDispatch.GridCell;
import org.asf.centuria.minigames.games.GameDizzywingDispatch.TileType;
import org.joml.Vector2i;

public class DDVis {

	public JFrame frame;
	public Map<Vector2i, JLabel> gameBoardDisplay = new HashMap<>();
	public Map<JLabel, Vector2i> gameBoardDisplayInverseMapping = new HashMap<>();
	private GameDizzywingDispatch ses;

	/**
	 * Create the application.
	 */
	public DDVis() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 1000, 650);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		MyMouseListener myMouseListener = new MyMouseListener();
		LevelIncrementListener levelIncrementListener = new LevelIncrementListener();

		for (int y = 0; y < 9; y++)
		{
			for (int x = 0; x < 9; x++)
			{
				JLabel l1 = new JLabel("New label");
				l1.setBounds((1000/9)*x, (500/9)*y, (1000/9), (500/9));
				l1.addMouseListener(myMouseListener);
				frame.getContentPane().add(l1);
				gameBoardDisplay.put(new Vector2i(x, y), l1);
				gameBoardDisplayInverseMapping.put(l1, new Vector2i(x, 8-y));
			}
		}

		JButton b = new JButton("next level");
		b.setBounds((1000/9)*1, (500/9)*10, (1000/9), (500/9));
		b.addMouseListener(levelIncrementListener);
		frame.getContentPane().add(b);


		new Thread(() -> {
			while (true) {
				if (Centuria.gameServer.getPlayers().length != 0) {
					if (Centuria.gameServer.getPlayers()[0].currentGame != null
							&& Centuria.gameServer.getPlayers()[0].currentGame instanceof GameDizzywingDispatch) {
						ses = (GameDizzywingDispatch) Centuria.gameServer.getPlayers()[0].currentGame;
						for (int y = 0; y < 9; y++)
						{
							for (int x = 0; x < 9; x++)
							{
								if(ses != null){

									gameBoardDisplay.get(new Vector2i(x, 8-y)).setText(
									"<html>" +
									ses.grid.getCell(new Vector2i(x, y)).getTileType().toString() + 
									"<br>" + 
									ses.grid.getCell(new Vector2i(x, y)).getBooster().toString() + 
									"<br>" + 
									ses.calcMoves.floodFillGetToVisit(new Vector2i(x, y)) +
									"</html>");
									gameBoardDisplay.get(new Vector2i(x, 8-y)).setForeground(TileColor(ses.grid.getCell(new Vector2i(x, y)).getTileType().toString()));
								}
							}
						}
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	private Color TileColor(String tileTypeString) {
		switch(tileTypeString) {
			case "RedBird":
				return Color.RED;
			case "BlueBird":
				return Color.BLUE;
			case "AquaBird":
				return Color.CYAN;
			case "GreenBird":
				return Color.GREEN;
			case "SnowyBird":
				return Color.LIGHT_GRAY;
			case "YellowBird":
				return Color.ORANGE;
			case "PurpleBird":
				return Color.MAGENTA;
			case "PinkBird":
				return Color.PINK;
			default:
				return Color.BLACK;
		}
	}

	class MyMouseListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) 
		{
			JLabel label = (JLabel) e.getComponent();
			Vector2i pos = gameBoardDisplayInverseMapping.get(label);
			if(ses != null){
				GridCell curr = ses.grid.getCell(pos);
	
				if(curr.getTileType() != TileType.PinkBird){
					curr.setColor(TileType.PinkBird);
				} else if (curr.getHealth() == 0) {
					curr.setHealth(1);
				} else if (curr.getTileType() != TileType.HatOrPurse){
					curr.setHealth(0);
					curr.setColor(TileType.HatOrPurse);
				}
	
				ses.grid.setCell(pos, curr);
				ses.syncClient(Centuria.gameServer.getPlayers()[0], new XtReader(""));
			}
		}
		
	}

	class LevelIncrementListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) 
		{
			if(ses != null){
				ses.level++;
				ses.objectives.newLevelNewObjectives();
				ses.syncClient(Centuria.gameServer.getPlayers()[0], new XtReader(""));
			}
		}
		
	}

}
