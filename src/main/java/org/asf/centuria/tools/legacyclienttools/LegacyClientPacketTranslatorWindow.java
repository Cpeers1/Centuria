package org.asf.centuria.tools.legacyclienttools;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.logging.log4j.LogManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.tools.legacyclienttools.http.DirectorProcessor;
import org.asf.centuria.tools.legacyclienttools.http.ProxyHttpProcessor;
import org.asf.centuria.tools.legacyclienttools.servers.BasicProxyServer;
import org.asf.centuria.tools.legacyclienttools.servers.TranslatorGameServer;
import org.asf.connective.ConnectiveHttpServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.JCheckBox;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.awt.event.ActionEvent;

public class LegacyClientPacketTranslatorWindow {

	public BasicProxyServer chat;
	public ConnectiveHttpServer director;
	public ConnectiveHttpServer api;
	public TranslatorGameServer game;

	private JFrame frmLegacyClientPacket;
	private JTextField txtHttpaerialworksddnsnet;
	private JLabel lblNewLabel_1;
	private JTextField txtHttpsaerialworksddnsnet;
	private JTextField txtAerialworksddnsnet;
	private JLabel lblNewLabel_2;
	private JCheckBox chckbxNewCheckBox;
	private JLabel lblNewLabel_3;
	private JTextField textField;
	private JTextField textField_1;
	private JLabel lblNewLabel_4;
	private JLabel lblNewLabel_5;
	private JTextField textField_2;
	private JLabel lblNewLabel_6;
	private JTextField textField_3;
	private JLabel lblNewLabel_7;
	private JTextField textField_4;
	private JTextField textField_5;
	private JLabel lblNewLabel_8;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LegacyClientPacketTranslatorWindow window = new LegacyClientPacketTranslatorWindow();
					window.frmLegacyClientPacket.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LegacyClientPacketTranslatorWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmLegacyClientPacket = new JFrame();
		frmLegacyClientPacket.setTitle("Legacy Client Packet Translator");
		frmLegacyClientPacket.setResizable(false);
		frmLegacyClientPacket.setBounds(100, 100, 648, 466);
		frmLegacyClientPacket.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmLegacyClientPacket.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		Centuria.logger = LogManager.getLogger("Proxy");

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(600, 400));
		frmLegacyClientPacket.getContentPane().add(panel);
		panel.setLayout(null);

		JLabel lblNewLabel = new JLabel("Upstream Director");
		lblNewLabel.setBounds(10, 11, 580, 14);
		panel.add(lblNewLabel);

		txtHttpaerialworksddnsnet = new JTextField();
		txtHttpaerialworksddnsnet.setText("http://emuferal.ddns.net:6969");
		txtHttpaerialworksddnsnet.setBounds(10, 28, 580, 20);
		panel.add(txtHttpaerialworksddnsnet);
		txtHttpaerialworksddnsnet.setColumns(10);

		lblNewLabel_1 = new JLabel("Upstream API");
		lblNewLabel_1.setBounds(10, 59, 580, 14);
		panel.add(lblNewLabel_1);

		txtHttpsaerialworksddnsnet = new JTextField();
		txtHttpsaerialworksddnsnet.setText("https://emuferal.ddns.net:6970");
		txtHttpsaerialworksddnsnet.setColumns(10);
		txtHttpsaerialworksddnsnet.setBounds(10, 75, 580, 20);
		panel.add(txtHttpsaerialworksddnsnet);

		txtAerialworksddnsnet = new JTextField();
		txtAerialworksddnsnet.setText("emuferal.ddns.net");
		txtAerialworksddnsnet.setColumns(10);
		txtAerialworksddnsnet.setBounds(10, 122, 580, 20);
		panel.add(txtAerialworksddnsnet);

		lblNewLabel_2 = new JLabel("Upstream Chat Server");
		lblNewLabel_2.setBounds(10, 106, 580, 14);
		panel.add(lblNewLabel_2);

		chckbxNewCheckBox = new JCheckBox("Encrypted");
		chckbxNewCheckBox.setSelected(true);
		chckbxNewCheckBox.setBounds(10, 149, 580, 23);
		panel.add(chckbxNewCheckBox);

		lblNewLabel_3 = new JLabel("Upstream game port");
		lblNewLabel_3.setBounds(10, 179, 256, 14);
		panel.add(lblNewLabel_3);

		textField = new JTextField();
		textField.setText("6968");
		textField.setColumns(10);
		textField.setBounds(10, 196, 281, 20);
		panel.add(textField);

		textField_1 = new JTextField();
		textField_1.setText("6972");
		textField_1.setColumns(10);
		textField_1.setBounds(301, 196, 289, 20);
		panel.add(textField_1);

		lblNewLabel_4 = new JLabel("Upstream chat port");
		lblNewLabel_4.setBounds(301, 179, 256, 14);
		panel.add(lblNewLabel_4);

		lblNewLabel_5 = new JLabel("Local game port");
		lblNewLabel_5.setBounds(10, 227, 256, 14);
		panel.add(lblNewLabel_5);

		textField_2 = new JTextField();
		textField_2.setText("6968");
		textField_2.setColumns(10);
		textField_2.setBounds(10, 244, 281, 20);
		panel.add(textField_2);

		lblNewLabel_6 = new JLabel("Local chat port");
		lblNewLabel_6.setBounds(301, 227, 256, 14);
		panel.add(lblNewLabel_6);

		textField_3 = new JTextField();
		textField_3.setText("6972");
		textField_3.setColumns(10);
		textField_3.setBounds(301, 244, 289, 20);
		panel.add(textField_3);

		lblNewLabel_7 = new JLabel("Local director port");
		lblNewLabel_7.setBounds(10, 276, 256, 14);
		panel.add(lblNewLabel_7);

		textField_4 = new JTextField();
		textField_4.setText("6969");
		textField_4.setColumns(10);
		textField_4.setBounds(10, 293, 281, 20);
		panel.add(textField_4);

		textField_5 = new JTextField();
		textField_5.setText("6970");
		textField_5.setColumns(10);
		textField_5.setBounds(301, 293, 281, 20);
		panel.add(textField_5);

		lblNewLabel_8 = new JLabel("Local API port");
		lblNewLabel_8.setBounds(301, 276, 256, 14);
		panel.add(lblNewLabel_8);

		JButton btnNewButton = new JButton("Start");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btnNewButton.getText().equals("Start")) {
					// Start servers
					chat = new BasicProxyServer();

					// Director
					try {
						director = ConnectiveHttpServer.createNetworked("HTTP/1.1",
								Map.of("address", "127.0.0.1", "port", textField_4.getText()));
						director.registerProcessor(new DirectorProcessor());
						director.start();
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(frmLegacyClientPacket,
								"Failed to start the director server, please check the configuration.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					// API
					try {
						api = ConnectiveHttpServer.createNetworked("HTTP/1.1",
								Map.of("address", "127.0.0.1", "port", textField_5.getText()));
						api.registerProcessor(new ProxyHttpProcessor(txtHttpsaerialworksddnsnet.getText()));
						api.start();
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(frmLegacyClientPacket,
								"Failed to start the API server, please check the configuration.", "Error",
								JOptionPane.ERROR_MESSAGE);
						try {
							director.stop();
						} catch (IOException e1) {
						}
						return;
					}

					// Chat
					try {
						chat.remoteAddr = txtAerialworksddnsnet.getText();
						chat.localPort = Integer.parseInt(textField_3.getText());
						chat.remotePort = Integer.parseInt(textField_1.getText());
						chat.encryptUpstream = chckbxNewCheckBox.isSelected();
						chat.start();
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(frmLegacyClientPacket,
								"Failed to start the chat server, please check the configuration.", "Error",
								JOptionPane.ERROR_MESSAGE);
						try {
							director.stop();
						} catch (IOException e1) {
						}
						try {
							api.stop();
						} catch (IOException e1) {
						}
						return;
					}

					// Game
					try {
						try {
							InputStream strm = new URL(txtHttpaerialworksddnsnet.getText() + "/v1/bestGameServer")
									.openStream();
							String resp = new String(strm.readAllBytes());
							strm.close();

							JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
							if (!obj.has("smartfoxServer"))
								throw new IOException();
							game = new TranslatorGameServer(Integer.parseInt(textField_2.getText()));
							game.directorAddr = txtHttpaerialworksddnsnet.getText();
							game.apiAddr = txtHttpsaerialworksddnsnet.getText();
							game.remotePort = Integer.parseInt(textField.getText());
						} catch (Exception e2) {
							JOptionPane.showMessageDialog(frmLegacyClientPacket,
									"Failed to contact the remote director server, please check the configuration.",
									"Error", JOptionPane.ERROR_MESSAGE);
							director.stop();
							api.stop();
							chat.stop();
							return;
						}
						game.start();
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(frmLegacyClientPacket,
								"Failed to start the game server, please check the configuration.", "Error",
								JOptionPane.ERROR_MESSAGE);
						try {
							director.stop();
						} catch (IOException e1) {
						}
						try {
							api.stop();
						} catch (IOException e1) {
						}
						chat.stop();
						return;
					}

					btnNewButton.setText("Stop");
				} else {
					// Stop
					try {
						director.stop();
					} catch (IOException e1) {
					}
					try {
						api.stop();
					} catch (IOException e1) {
					}
					chat.stop();
					game.stop();
					btnNewButton.setText("Start");
				}
			}
		});
		btnNewButton.setBounds(501, 366, 89, 23);
		panel.add(btnNewButton);
	}
}
