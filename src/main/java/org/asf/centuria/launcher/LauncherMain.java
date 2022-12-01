package org.asf.centuria.launcher;

import java.awt.EventQueue;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JProgressBar;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.asf.centuria.Centuria;

import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class LauncherMain {

	private JFrame frmCenturiaLauncher;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LauncherMain window = new LauncherMain();
					window.frmCenturiaLauncher.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LauncherMain() {
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

		frmCenturiaLauncher = new JFrame();
		frmCenturiaLauncher.setResizable(false);
		frmCenturiaLauncher.setTitle("EmuFeral Launcher");
		frmCenturiaLauncher.setBounds(100, 100, 651, 342);
		frmCenturiaLauncher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCenturiaLauncher.setLocationRelativeTo(null);
		try {
			InputStream strmi = getClass().getClassLoader().getResourceAsStream("emulogo_purple.png");
			frmCenturiaLauncher.setIconImage(ImageIO.read(strmi));
			strmi.close();
		} catch (IOException e1) {
		}

		JPanel panel = new JPanel();
		frmCenturiaLauncher.getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JProgressBar progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(146, 20));
		panel.add(progressBar, BorderLayout.NORTH);

		JPanel panel_1 = new JPanel();
		frmCenturiaLauncher.getContentPane().add(panel_1, BorderLayout.CENTER);

		JPanel panel_2 = new JPanel();
		panel_2.setPreferredSize(new Dimension(600, 260));
		panel_1.add(panel_2);
		panel_2.setLayout(null);

		JLabel lblNewLabel = new JLabel("Starting EmuFeral Singleplayer...");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 24));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		lblNewLabel.setBounds(10, 91, 580, 49);
		panel_2.add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Checking for updates...");
		lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_1.setBounds(10, 139, 580, 33);
		panel_2.add(lblNewLabel_1);

		Thread th = new Thread(() -> {
			try {
				SwingUtilities.invokeLater(() -> {
					lblNewLabel_1.setText("Checking for server updates...");
				});
				String cver = "";
				if (new File("version.info").exists()) {
					cver = Files.readString(Path.of("version.info"));
				}

				// Update configuration
				String updateChannel = "alpha";
				if (new File("updater.conf").exists()) {
					// Parse properties
					HashMap<String, String> properties = new HashMap<String, String>();
					for (String line : Files.readAllLines(Path.of("updater.conf"))) {
						String key = line;
						String value = "";
						if (key.contains("=")) {
							value = key.substring(key.indexOf("=") + 1);
							key = key.substring(0, key.indexOf("="));
						}
						properties.put(key, value);
					}

					// Load channel
					updateChannel = properties.getOrDefault("channel", updateChannel);
				}

				// Check for updates
				System.out.println("Checking for updates...");
				try {
					InputStream updateLog = new URL(Centuria.DOWNLOAD_BASE_URL + "/" + updateChannel + "/update.info")
							.openStream();
					String update = new String(updateLog.readAllBytes(), "UTF-8").trim();
					updateLog.close();

					if (!cver.equals(update)) {
						// Download the update list
						System.out.println("Update available, new version: " + update);
						System.out.println("Preparing to update Centuria...");
						SwingUtilities.invokeLater(() -> {
							lblNewLabel_1.setText("Updating to Centuria " + update + "...");
						});

						InputStream strm = new URL(
								Centuria.DOWNLOAD_BASE_URL + "/" + updateChannel + "/" + update + "/update.list")
								.openStream();
						String fileList = new String(strm.readAllBytes(), "UTF-8").trim();
						strm.close();

						// Parse the file list (newline-separated)
						HashMap<String, String> urls = new HashMap<String, String>();
						for (String file : fileList.split("\n")) {
							if (!file.isEmpty()) {
								urls.put(file,
										Centuria.DOWNLOAD_BASE_URL + "/" + updateChannel + "/" + update + "/" + file);
							}
						}
						if (urls.containsKey("Centuria.jar")) {
							urls.put("CenturiaServer.jar", urls.get("Centuria.jar"));
						}

						// Set progress bar status
						SwingUtilities.invokeLater(() -> {
							lblNewLabel_1.setText("Updating to Centuria " + update + "...");
							progressBar.setMaximum(urls.size());
						});

						// Begin download
						for (String file : urls.keySet()) {
							File output = new File(file);
							if (output.getParentFile() != null && !output.getParentFile().exists())
								output.getParentFile().mkdirs();
							URL u = new URL(urls.get(file));
							strm = u.openStream();
							FileOutputStream o = new FileOutputStream(output);
							strm.transferTo(o);
							o.close();

							try {
								SwingUtilities.invokeAndWait(() -> {
									progressBar.setValue(progressBar.getValue() + 1);
								});
							} catch (InvocationTargetException | InterruptedException e) {
							}
						}

						// Save version
						Files.writeString(Path.of("version.info"), update);
					}
				} catch (IOException e) {
				}

				File server = new File("CenturiaServer.jar");
				if (!server.exists()) {
					JOptionPane.showMessageDialog(null,
							"Failed to download the Centuria server!\nThis is most likely due to either network being offline or a server issue.\n\nPlease try again in a few minutes, if the error persists, please contact Zera#4232.",
							"Download Failure", JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}

				// Set progress bar status
				try {
					SwingUtilities.invokeAndWait(() -> {
						lblNewLabel_1.setText("Checking for client updates...");
						progressBar.setMaximum(100);
						progressBar.setValue(0);
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}

				String currentClient = "";
				if (new File("clientversion.info").exists()) {
					currentClient = Files.readString(Path.of("clientversion.info"));
				}

				// Check for client updates
				try {
					// Download client ini
					InputStream strm = new URL("https://download.fer.al/win64/launcher.ini").openStream();
					String propFile = new String(strm.readAllBytes(), "UTF-8").trim().replace("\r", "");
					strm.close();

					// Parse ini (ish)
					HashMap<String, String> properties = new HashMap<String, String>();
					for (String line : propFile.split("\n")) {
						String key = line;
						String value = "";
						if (key.contains("=")) {
							value = key.substring(key.indexOf("=") + 1);
							key = key.substring(0, key.indexOf("="));
						}
						properties.put(key, value);
					}

					// Load version and download URL
					String cVer = properties.get("ApplicationVersion");
					String url = properties.get("ApplicationDownloadUrl");
					if (!currentClient.equals(cVer)) {
						// Download new client
						try {
							SwingUtilities.invokeAndWait(() -> {
								lblNewLabel_1.setText("Updating Fer.al...");
								progressBar.setMaximum(100);
								progressBar.setValue(0);
							});
						} catch (InvocationTargetException | InterruptedException e) {
						}

						URLConnection urlConnection = new URL(url).openConnection();
						try {
							SwingUtilities.invokeAndWait(() -> {
								lblNewLabel_1.setText("Updating client...");
								progressBar.setMaximum(urlConnection.getContentLength() / 1000);
								progressBar.setValue(0);
							});
						} catch (InvocationTargetException | InterruptedException e) {
						}

						File tmpOut = new File("client.7z");
						InputStream data = urlConnection.getInputStream();
						FileOutputStream out = new FileOutputStream(tmpOut);
						while (true) {
							byte[] b = data.readNBytes(1000);
							if (b.length == 0)
								break;
							else {
								out.write(b);
								SwingUtilities.invokeLater(() -> {
									progressBar.setValue(progressBar.getValue() + 1);
								});
							}
						}
						out.close();
						data.close();

						SwingUtilities.invokeLater(() -> {
							progressBar.setValue(progressBar.getMaximum());
						});
						try {
							SwingUtilities.invokeAndWait(() -> {
								lblNewLabel_1.setText("Installing client update...");
								progressBar.setMaximum(100);
								progressBar.setValue(0);
							});
						} catch (InvocationTargetException | InterruptedException e) {
						}

						unzip7z(tmpOut, new File("client"), progressBar);

						// Apply modifications
						try {
							SwingUtilities.invokeAndWait(() -> {
								lblNewLabel_1.setText("Modifying the client...");
								progressBar.setMaximum(100);
								progressBar.setValue(0);
							});
						} catch (InvocationTargetException | InterruptedException e) {
						}
						File modOut = new File("client/build/Fer.al_Data/sharedassets1.assets");
						FileOutputStream os = new FileOutputStream(modOut);
						InputStream is = new URL("https://aerialworks.ddns.net/extra/emuferal/sharedassets1.assets")
								.openStream();
						is.transferTo(os);
						os.close();
						is.close();

						// Save version
						Files.writeString(Path.of("clientversion.info"), cVer);
					}
				} catch (IOException e) {
				}

				File client = new File("client/build/Fer.al.exe");
				if (!client.exists()) {
					JOptionPane.showMessageDialog(null, "Failed to download the fer.al client!", "Download Failure",
							JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}

				// Start server
				try {
					SwingUtilities.invokeAndWait(() -> {
						lblNewLabel_1.setText("Starting server...");
						progressBar.setMaximum(100);
						progressBar.setValue(0);
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
				String jvm = ProcessHandle.current().info().command().get();
				// Scan libs
				String libs = server.getName();
				for (File lib : new File("libs").listFiles()) {
					libs += File.pathSeparator + "libs/" + lib.getName();
				}
				ProcessBuilder builder = new ProcessBuilder(jvm, "-cp", libs, "org.asf.centuria.Centuria");
				Process serverProc = builder.start();

				// Start client
				try {
					SwingUtilities.invokeAndWait(() -> {
						lblNewLabel_1.setText("Starting client...");
						progressBar.setMaximum(100);
						progressBar.setValue(0);
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
				SwingUtilities.invokeLater(() -> {
					frmCenturiaLauncher.dispose();
				});
				builder = new ProcessBuilder(client.getAbsolutePath());
				try {
					builder.start().waitFor();
				} catch (InterruptedException e) {
				}
				serverProc.destroy();
				System.exit(0);
			} catch (IOException e) {
			}
		}, "Launcher Thread");
		th.start();
	}

	private void unzip7z(File input, File output, JProgressBar bar) throws IOException {
		output.mkdirs();

		// count entries
		SevenZFile archive = new SevenZFile(input);
		int count = 0;
		while (archive.getNextEntry() != null) {
			count++;
		}
		archive.close();

		// prepare and log
		archive = new SevenZFile(input);
		try {
			int fcount = count;
			SwingUtilities.invokeAndWait(() -> {
				bar.setMaximum(fcount);
				bar.setValue(0);
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// extract
		while (true) {
			SevenZArchiveEntry ent = archive.getNextEntry();
			if (ent == null)
				break;

			if (ent.isDirectory()) {
				new File(output, ent.getName()).mkdirs();
			} else {
				File out = new File(output, ent.getName());
				if (out.getParentFile() != null && !out.getParentFile().exists())
					out.getParentFile().mkdirs();
				FileOutputStream os = new FileOutputStream(out);
				InputStream is = archive.getInputStream(ent);
				is.transferTo(os);
				is.close();
				os.close();
			}

			SwingUtilities.invokeLater(() -> {
				bar.setValue(bar.getValue() + 1);
			});
		}

		// finish progress
		SwingUtilities.invokeLater(() -> {
			bar.setValue(bar.getValue() + 1);
		});
		archive.close();
	}
}
