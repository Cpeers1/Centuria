package org.asf.centuria.minigames.games;

import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.asf.centuria.Centuria;

public class WTHVis {

	public JFrame frame;

	/**
	 * Create the application.
	 */
	public WTHVis() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 500, 485);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JLabel l1 = new JLabel("New label");
		l1.setBounds(24, 149, 46, 14);
		frame.getContentPane().add(l1);

		JLabel l2 = new JLabel("New label");
		l2.setBounds(24, 170, 46, 14);
		frame.getContentPane().add(l2);

		JLabel l3 = new JLabel("New label");
		l3.setBounds(24, 189, 46, 14);
		frame.getContentPane().add(l3);

		JLabel l4 = new JLabel("New label");
		l4.setBounds(24, 209, 46, 14);
		frame.getContentPane().add(l4);

		JLabel l5 = new JLabel("New label");
		l5.setBounds(72, 135, 46, 14);
		frame.getContentPane().add(l5);

		JLabel l6 = new JLabel("New label");
		l6.setBounds(72, 151, 46, 14);
		frame.getContentPane().add(l6);

		JLabel l7 = new JLabel("New label");
		l7.setBounds(72, 170, 46, 14);
		frame.getContentPane().add(l7);

		JLabel l8 = new JLabel("New label");
		l8.setBounds(72, 195, 46, 14);
		frame.getContentPane().add(l8);

		JLabel l9 = new JLabel("New label");
		l9.setBounds(72, 219, 46, 14);
		frame.getContentPane().add(l9);

		JLabel l10 = new JLabel("New label");
		l10.setBounds(119, 122, 46, 14);
		frame.getContentPane().add(l10);

		JLabel l11 = new JLabel("New label");
		l11.setBounds(119, 137, 46, 14);
		frame.getContentPane().add(l11);

		JLabel l12 = new JLabel("New label");
		l12.setBounds(119, 159, 46, 14);
		frame.getContentPane().add(l12);

		JLabel l13 = new JLabel("New label");
		l13.setBounds(119, 182, 46, 14);
		frame.getContentPane().add(l13);

		JLabel l14 = new JLabel("New label");
		l14.setBounds(119, 205, 46, 14);
		frame.getContentPane().add(l14);

		JLabel l15 = new JLabel("New label");
		l15.setBounds(119, 232, 46, 14);
		frame.getContentPane().add(l15);

		JLabel l16 = new JLabel("New label");
		l16.setBounds(167, 109, 46, 14);
		frame.getContentPane().add(l16);

		JLabel l17 = new JLabel("New label");
		l17.setBounds(170, 135, 46, 14);
		frame.getContentPane().add(l17);

		JLabel l18 = new JLabel("New label");
		l18.setBounds(169, 153, 46, 14);
		frame.getContentPane().add(l18);

		JLabel l19 = new JLabel("New label");
		l19.setBounds(170, 176, 46, 14);
		frame.getContentPane().add(l19);

		JLabel l20 = new JLabel("New label");
		l20.setBounds(171, 198, 46, 14);
		frame.getContentPane().add(l20);

		JLabel l21 = new JLabel("New label");
		l21.setBounds(171, 217, 46, 14);
		frame.getContentPane().add(l21);

		JLabel l22 = new JLabel("New label");
		l22.setBounds(167, 244, 46, 14);
		frame.getContentPane().add(l22);

		JLabel l23 = new JLabel("New label");
		l23.setBounds(223, 122, 46, 14);
		frame.getContentPane().add(l23);

		JLabel l24 = new JLabel("New label");
		l24.setBounds(225, 137, 46, 14);
		frame.getContentPane().add(l24);

		JLabel l25 = new JLabel("New label");
		l25.setBounds(226, 158, 46, 14);
		frame.getContentPane().add(l25);

		JLabel l26 = new JLabel("New label");
		l26.setBounds(228, 178, 46, 14);
		frame.getContentPane().add(l26);

		JLabel l27 = new JLabel("New label");
		l27.setBounds(221, 198, 46, 14);
		frame.getContentPane().add(l27);

		JLabel l28 = new JLabel("New label");
		l28.setBounds(220, 222, 46, 14);
		frame.getContentPane().add(l28);

		JLabel l29 = new JLabel("New label");
		l29.setBounds(278, 129, 46, 14);
		frame.getContentPane().add(l29);

		JLabel l30 = new JLabel("New label");
		l30.setBounds(278, 148, 46, 14);
		frame.getContentPane().add(l30);

		JLabel l31 = new JLabel("New label");
		l31.setBounds(275, 170, 46, 14);
		frame.getContentPane().add(l31);

		JLabel l32 = new JLabel("New label");
		l32.setBounds(273, 196, 46, 14);
		frame.getContentPane().add(l32);

		JLabel l33 = new JLabel("New label");
		l33.setBounds(271, 216, 46, 14);
		frame.getContentPane().add(l33);

		JLabel l34 = new JLabel("New label");
		l34.setBounds(329, 139, 46, 14);
		frame.getContentPane().add(l34);

		JLabel l35 = new JLabel("New label");
		l35.setBounds(327, 162, 46, 14);
		frame.getContentPane().add(l35);

		JLabel l36 = new JLabel("New label");
		l36.setBounds(327, 185, 46, 14);
		frame.getContentPane().add(l36);

		JLabel l37 = new JLabel("New label");
		l37.setBounds(327, 207, 46, 14);
		frame.getContentPane().add(l37);

		new Thread(() -> {
			while (true) {
				if (Centuria.gameServer.getPlayers().length != 0) {
					if (Centuria.gameServer.getPlayers()[0].currentGame != null
							&& Centuria.gameServer.getPlayers()[0].currentGame instanceof GameWhatTheHex) {
						GameWhatTheHex ses = (GameWhatTheHex) Centuria.gameServer.getPlayers()[0].currentGame;
						int i = 0;
						for (Component comp : frame.getContentPane().getComponents()) {
							if (comp instanceof JLabel) {
								tile((JLabel) comp, ses.board[i++]);
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

	private void tile(JLabel lbl, byte b) {
		SwingUtilities.invokeLater(() -> {
			if (b == 0)
				lbl.setText("X");
			else
				lbl.setText(b == 2 ? "FLAME" : b == 3 ? "FLORA" : "MIASMA");
		});
	}
}
