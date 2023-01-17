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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OnlineLauncherMain {

	private JFrame frmCenturiaLauncher;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					OnlineLauncherMain window = new OnlineLauncherMain();
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
	public OnlineLauncherMain() {
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
		frmCenturiaLauncher.setTitle("EmuFeral Online Launcher");
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

		JLabel lblNewLabel = new JLabel("EmuFeral Online");
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
					String launcherUrl = "https://download.fer.al/win64/launcher.ini";

					// Check OS
					if (System.getProperty("os.name").toLowerCase().contains("darwin")
							|| System.getProperty("os.name").toLowerCase().contains("mac")) {
						launcherUrl = "https://download.fer.al/osx/launcher.ini";
					}

					InputStream strm = new URL(launcherUrl).openStream();
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

						// Check OS
						if (System.getProperty("os.name").toLowerCase().contains("darwin")
								|| System.getProperty("os.name").toLowerCase().contains("mac")) {
							unZip(tmpOut, new File("client"), progressBar); // OSX
						} else {
							unzip7z(tmpOut, new File("client"), progressBar); // Windows or linux
						}

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

						// Check OS
						if (System.getProperty("os.name").toLowerCase().contains("darwin")
								|| System.getProperty("os.name").toLowerCase().contains("mac")) {
							modOut = new File("client/build/Fer.al.app/Contents/Resources/Data/sharedassets1.assets"); // MacOS
						}

						FileOutputStream os = new FileOutputStream(modOut);
						InputStream is = new URL(
								"https://aerialworks.ddns.net/extra/emuferal/sharedassets1-online.assets").openStream();
						is.transferTo(os);
						os.close();
						is.close();

						// Save version
						Files.writeString(Path.of("clientversion.info"), cVer);
					}
				} catch (IOException e) {
				}

				File client;

				// Check OS
				if (System.getProperty("os.name").toLowerCase().contains("darwin")
						|| System.getProperty("os.name").toLowerCase().contains("mac")) {
					client = new File("client/build/Fer.al.app/Contents/MacOS/Feral"); // MacOS
				} else {
					client = new File("client/build/Fer.al.exe"); // Linux or Windows
				}
				if (!client.exists()) {
					JOptionPane.showMessageDialog(null, "Failed to download the fer.al client!", "Download Failure",
							JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}

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
				ProcessBuilder builder;

				// Check OS
				if (System.getProperty("os.name").toLowerCase().contains("win")
						&& !System.getProperty("os.name").toLowerCase().contains("darwin"))
					builder = new ProcessBuilder(client.getAbsolutePath()); // Windows
				else if (System.getProperty("os.name").toLowerCase().contains("darwin")
						|| System.getProperty("os.name").toLowerCase().contains("mac"))
					builder = new ProcessBuilder("open", "-n", client.getAbsolutePath()); // MacOS
				else
					builder = new ProcessBuilder("wine", client.getAbsolutePath()); // Linux, need wine
				try {
					builder.start().waitFor();
				} catch (InterruptedException e) {
				}
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

	private void unZip(File input, File output, JProgressBar bar) throws IOException {
		output.mkdirs();

		// count entries
		ZipFile archive = new ZipFile(input);
		int count = 0;
		Enumeration<? extends ZipEntry> en = archive.entries();
		while (en.hasMoreElements()) {
			en.nextElement();
			count++;
		}
		archive.close();

		// prepare and log
		archive = new ZipFile(input);
		en = archive.entries();
		try {
			int fcount = count;
			SwingUtilities.invokeAndWait(() -> {
				bar.setMaximum(fcount);
				bar.setValue(0);
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// extract
		while (en.hasMoreElements()) {
			ZipEntry ent = en.nextElement();
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
