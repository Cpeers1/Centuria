package org.asf.centuria.minigames.games;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.asf.centuria.Centuria;
import org.joml.Vector2i;

public class DDVis {

	public JFrame frame;
	public Map<Vector2i, JLabel> gameBoardDisplay = new HashMap<>();
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

		for (int y = 0; y < 9; y++)
		{
			for (int x = 0; x < 9; x++)
			{
				JLabel l1 = new JLabel("New label");
				l1.setBounds((1000/9)*x, (500/9)*y, (1000/9), (500/9));
				frame.getContentPane().add(l1);
				gameBoardDisplay.put(new Vector2i(x, y), l1);
			}
		}


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
								gameBoardDisplay.get(new Vector2i(x, 8-y)).setText(
								"<html>" +
								ses.gameState.GetCell(new Vector2i(x, y)).TileType.toString() + 
								"<br>" + 
								ses.gameState.GetCell(new Vector2i(x, y)).Booster.toString() + 
								"<br>" + 
								ses.gameState.floodFillGetMatch(new Vector2i(x, y)) +
								"</html>");
								gameBoardDisplay.get(new Vector2i(x, 8-y)).setForeground(TileColor(ses.gameState.GetCell(new Vector2i(x, y)).TileType.toString()));
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
}
