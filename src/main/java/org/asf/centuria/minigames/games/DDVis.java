package org.asf.centuria.minigames.games;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.asf.centuria.Centuria;

public class DDVis {

	public JFrame frame;

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
		frame.setBounds(100, 100, 1000, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		for (int y = 0; y < 9; y++)
		{
			for (int x = 0; x < 9; x++)
			{
				JLabel l1 = new JLabel("New label");
				l1.setBounds((1000/9)*x, (500/9)*y, (1000/9), (500/9));
				frame.getContentPane().add(l1);
			}
		}

		new Thread(() -> {
			while (true) {
				if (Centuria.gameServer.getPlayers().length != 0) {
					if (Centuria.gameServer.getPlayers()[0].currentGame != null
							&& Centuria.gameServer.getPlayers()[0].currentGame instanceof GameDizzywingDispatch) {
						GameDizzywingDispatch ses = (GameDizzywingDispatch) Centuria.gameServer.getPlayers()[0].currentGame;
						int i = 0;
						for (Component comp : frame.getContentPane().getComponents()) {
							if (comp instanceof JLabel) {
								((JLabel) comp).setText(ses.gameState.grid[i/9][i%9].TileType.toString() + 
														'\n' + 
														ses.gameState.grid[i/9][i%9].Booster.toString());
								((JLabel) comp).setForeground(TileColor(ses.gameState.grid[i/9][i%9].TileType.toString()));
								i++;
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
			case "GreenBird":
				return Color.GREEN;
			case "WhiteBird":
				return Color.WHITE;
			case "YellowBird":
				return Color.YELLOW;
			case "PurpleBird":
				return Color.MAGENTA;
			default:
				return Color.BLACK;
		}
	}
}
