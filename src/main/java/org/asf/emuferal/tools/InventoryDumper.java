package org.asf.emuferal.tools;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class InventoryDumper {

	private JFrame frmEmuferalInventoryDumpoer;
	private JTextField textField;
	private JTextField txtOutput;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					InventoryDumper window = new InventoryDumper();
					window.frmEmuferalInventoryDumpoer.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public InventoryDumper() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		try {
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e1) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
		}

		frmEmuferalInventoryDumpoer = new JFrame();
		frmEmuferalInventoryDumpoer.setResizable(false);
		frmEmuferalInventoryDumpoer.setTitle("EmuFeral Inventory Dumper");
		frmEmuferalInventoryDumpoer.setBounds(100, 100, 771, 356);
		frmEmuferalInventoryDumpoer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmEmuferalInventoryDumpoer.setLocationRelativeTo(null);
		frmEmuferalInventoryDumpoer.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(700, 300));
		frmEmuferalInventoryDumpoer.getContentPane().add(panel);
		panel.setLayout(null);

		JProgressBar progressBar = new JProgressBar();
		progressBar.setBounds(10, 237, 680, 22);
		panel.add(progressBar);

		JLabel lblNewLabel = new JLabel("Waiting for packet data to be entered...");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		lblNewLabel.setBounds(10, 212, 680, 22);
		panel.add(lblNewLabel);

		textField = new JTextField();
		textField.setPreferredSize(new Dimension(7, 17));
		textField.setText("<please select>");
		textField.setBounds(10, 68, 529, 30);
		panel.add(textField);
		textField.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("Fer.al Packet Recording");
		lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblNewLabel_1.setBounds(10, 45, 680, 13);
		panel.add(lblNewLabel_1);

		JButton btnNewButton = new JButton("Browse...");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser dialog = new JFileChooser(new File("."));
				dialog.setAcceptAllFileFilterUsed(false);
				dialog.addChoosableFileFilter(new FileFilter() {

					@Override
					public boolean accept(File arg0) {
						return arg0.getName().endsWith(".txt") || arg0.isDirectory();
					}

					@Override
					public String getDescription() {
						return "Fer.al Packet Recording (*.txt)";
					}

				});
				int result = dialog.showOpenDialog(frmEmuferalInventoryDumpoer);
				if (result == JFileChooser.APPROVE_OPTION) {
					textField.setText(dialog.getSelectedFile().getAbsolutePath());
					if (dialog.getSelectedFile().exists()) {
						lblNewLabel.setText("Ready");
					} else {
						lblNewLabel.setText("Waiting for packet data to be entered...");
					}
				}
			}
		});
		btnNewButton.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnNewButton.setBounds(545, 68, 145, 30);
		panel.add(btnNewButton);

		txtOutput = new JTextField();
		txtOutput.setText(new File("output").getAbsolutePath());
		txtOutput.setPreferredSize(new Dimension(7, 17));
		txtOutput.setColumns(10);
		txtOutput.setBounds(10, 148, 529, 30);
		panel.add(txtOutput);

		JButton btnNewButton_1 = new JButton("Browse...");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser f = new JFileChooser(textField.getText());
				f.setDialogTitle("Select installation directory...");
				f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				f.showSaveDialog(frmEmuferalInventoryDumpoer);
				if (f.getSelectedFile() != null)
					txtOutput.setText(f.getSelectedFile().getAbsolutePath());
			}
		});
		btnNewButton_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnNewButton_1.setBounds(545, 148, 145, 30);
		panel.add(btnNewButton_1);

		JLabel lblNewLabel_1_1 = new JLabel("Destination");
		lblNewLabel_1_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblNewLabel_1_1.setBounds(10, 125, 680, 13);
		panel.add(lblNewLabel_1_1);

		JButton btnNewButton_2 = new JButton("Begin");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File input = new File(textField.getText());
				File output = new File(txtOutput.getText());
				if (!input.exists()) {
					lblNewLabel.setText("Specified input file does not exist");
					return;
				}

				output.mkdirs();
				if (!output.exists()) {
					lblNewLabel.setText("Could not write to the destination directory");
					return;
				}
				btnNewButton_2.setEnabled(false);
				lblNewLabel.setText("Preparing...");

				Thread th = new Thread(() -> {
					dump(input, output);
				}, "Dumper Thread");
				th.setDaemon(true);
				th.start();
			}

			private void dump(File input, File output) {
				try {
					int i = 0;
					ArrayList<String> files = new ArrayList<String>(Files.readAllLines(input.toPath()));
					for (String str : new ArrayList<String>(files)) {
						if (str.isBlank())
							files.remove(str);
					}
					File outputFolder = new File(output, "packets");
					outputFolder.mkdirs();

					if (!new File(output, "packet.decode.completed").exists()) {
						final int fs = files.size();
						SwingUtilities.invokeLater(() -> {
							lblNewLabel.setText("Decoding packet data...");
							progressBar.setMaximum(fs);
							progressBar.setValue(0);
						});

						for (String line : files) {
							line = line.substring(1);
							String info = line.substring(line.indexOf("|") + 1);
							String content = info.substring(info.indexOf("|") + 1);
							info = info.substring(0, info.indexOf("|"));

							FileOutputStream os = new FileOutputStream(
									outputFolder + "/" + i++ + " (" + info.replace("->", " to ") + ").bin");
							for (int i2 = 0; i2 < content.length(); i2 += 2) {
								String hex = content.charAt(i2) + "" + content.charAt(i2 + 1);
								os.write(Integer.valueOf(hex, 16));
							}
							os.close();
							System.out.println("Decoded " + i + " packet(s)");

							SwingUtilities.invokeAndWait(() -> {
								progressBar.setMaximum(fs);
								progressBar.setValue(progressBar.getValue() + 1);
							});
						}

						new File(output, "packet.decode.completed").createNewFile();
					}

					File invOut = new File(output, "inventory objects");
					invOut.mkdirs();
					SwingUtilities.invokeLater(() -> {
						lblNewLabel.setText("Dumping inventory files...");
						progressBar.setMaximum(outputFolder.listFiles().length);
						progressBar.setValue(0);
					});

					String currentData = "";
					String currentFile = null;
					for (File packet : Stream.of(outputFolder.listFiles())
							.filter(t -> t.getName().endsWith(".bin") && t.getName().contains(" ("))
							.sorted((t1, t2) -> {
								String td1 = t1.getName().substring(0, t1.getName().indexOf(" ("));
								String td2 = t2.getName().substring(0, t2.getName().indexOf(" ("));

								return Integer.compare(Integer.valueOf(td1), Integer.valueOf(td2));
							}).toArray(t -> new File[t])) {

						SwingUtilities.invokeAndWait(() -> {
							progressBar.setValue(progressBar.getValue() + 1);
						});

						if (!packet.getName().endsWith(".bin"))
							continue;

						if (currentFile != null && packet.getName().endsWith("(S to C).bin")) {
							String payload = Files.readString(packet.toPath()).trim();
							if (currentData.isEmpty()
									&& (payload.startsWith("%xt%ilt%-1%") || payload.startsWith("%xt%il%-1%"))) {
								currentData += payload;

								if (!currentData.endsWith("%")) {
									boolean unlock = false;
									for (File packet2 : Stream.of(outputFolder.listFiles())
											.filter(t -> t.getName().endsWith(".bin") && t.getName().contains(" ("))
											.sorted((t1, t2) -> {
												String td1 = t1.getName().substring(0, t1.getName().indexOf(" ("));
												String td2 = t2.getName().substring(0, t2.getName().indexOf(" ("));

												return Integer.compare(Integer.valueOf(td1), Integer.valueOf(td2));
											}).toArray(t -> new File[t])) {
										if (!packet2.getName().endsWith(".bin"))
											continue;

										if (packet2.getName().equals(packet.getName())) {
											unlock = true;
										} else if (unlock && packet2.getName().endsWith("(S to C).bin")) {
											payload = Files.readString(packet2.toPath()).trim();
											currentData += payload;
											if (payload.endsWith("%")) {
												break;
											}
										}
									}
								}

								XtReader rd = new XtReader(currentData);
								rd.read();
								rd.read();
								byte[] compressed = rd.readBytes();

								GZIPInputStream strm = new GZIPInputStream(new ByteArrayInputStream(compressed));
								byte[] data = strm.readAllBytes();
								strm.close();

								String json = new String(data, "UTF-8");

								Files.writeString(Path.of(invOut + "/" + currentFile + ".json"), json);
								currentFile = null;
								currentData = "";
							}
						} else {
							if (packet.getName().endsWith("(C to S).bin")) {
								String payload = Files.readString(packet.toPath()).trim();
								if (payload.startsWith("%xt%o%ilt%-1%")) {
									currentFile = payload.substring("%xt%o%ilt%-1%".length());
									currentFile = currentFile.substring(0, currentFile.length() - 1);
								}
							}
						}
					}

					SwingUtilities.invokeLater(() -> {
						lblNewLabel.setText("Processing avatar file...");
						progressBar.setMaximum(100);
						progressBar.setValue(0);
					});

					JsonArray localAvatars = JsonParser
							.parseString(Files.readString(new File(invOut, "200.json").toPath())).getAsJsonArray();
					JsonArray avatars = InventoryItemDownloadPacket.buildDefaultLooksFile(null);
					SwingUtilities.invokeLater(() -> {
						lblNewLabel.setText("Processing avatar file...");
						progressBar.setMaximum(localAvatars.size());
						progressBar.setValue(0);
					});

					int i2 = 1;
					for (JsonElement ele : localAvatars) {
						SwingUtilities.invokeAndWait(() -> {
							progressBar.setValue(progressBar.getValue() + 1);
						});

						JsonObject avaD = ele.getAsJsonObject();
						if (avaD.get("components").getAsJsonObject().has("PrimaryLook")) {
							avaD.get("components").getAsJsonObject().remove("PrimaryLook");
						}
						avaD.get("components").getAsJsonObject().get("Name").getAsJsonObject().remove("name");
						avaD.get("components").getAsJsonObject().get("Name").getAsJsonObject().addProperty("name",
								"Look-" + i2++);

						avatars.add(ele);
					}

					Files.writeString(Path.of(invOut.getAbsolutePath() + "/avatars.json"), avatars.toString());
					new File(invOut, "200.json").delete();
					new File(output, "packet.decode.completed").delete();
					SwingUtilities.invokeLater(() -> {
						lblNewLabel.setText("Finished");
						progressBar.setMaximum(100);
						progressBar.setValue(100);
					});
					JOptionPane.showMessageDialog(null,
							"Inventory dump has been completed!\nThe 'inventory objects' folder contains your player data.\n\nCopy the contents of that folder to your EmuFeral inventory to use them.",
							"Dump Completed", JOptionPane.INFORMATION_MESSAGE);
					System.exit(0);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Inventory dump failed!", "Dump Failure",
							JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
			}
		});
		btnNewButton_2.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnNewButton_2.setBounds(10, 269, 680, 21);
		panel.add(btnNewButton_2);
	}
}
