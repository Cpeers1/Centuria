package org.asf.emuferal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.asf.rats.ConnectedClient;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.ConnectiveServerFactory;

public class EmuFeral {

	public static class DirectorServer extends ConnectiveHTTPServer {

		public boolean waitWithAccept = false;

		public DirectorServer() {
			super();
			serverProcessor = new Thread(() -> {
				while (connected) {
					try {
						while (waitWithAccept) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
							}
						}
						Socket client = socket.accept();
						if (waitWithAccept) {
							continue;
						}

						acceptConnection(client);
						InputStream in = getClientInput(client);
						OutputStream out = getClientOutput(client);

						ConnectedClient cl = new ConnectedClient(client, in, out, this);
						clients.add(cl);
						cl.beginReceive();
					} catch (IOException ex) {
						if (!connected)
							break;

						error("Failed to process client", ex);
					}
				}
			}, "Connective server thread");
		}

		public Socket acceptClient() throws IOException {
			waitWithAccept = true;
			try {
				Socket s = socket.accept();
				waitWithAccept = false;
				return s;
			} finally {
				waitWithAccept = false;
			}
		}
	}

	public static void main(String[] args) throws InvocationTargetException, IOException {
		System.out.println("--------------------------------------------------------------------");
		System.out.println("                                                                    ");
		System.out.println("                              EmuFeral                              ");
		System.out.println("                       Fer.al Server Emulator                       ");
		System.out.println("                                                                    ");
		System.out.println("                          Version 1.0.0.A2                          ");
		System.out.println("                                                                    ");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("");
		System.out.println("Starting Emulated Feral API server...");
		ConnectiveHTTPServer apiServer;
		try {
			apiServer = new ConnectiveServerFactory().setPort(6).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		} catch (Exception e) {
			System.err.println("Unable to start on port 6! Switching to debug mode!");
			System.err.println("If you are not attempting to debug the server, please run as administrator.");
			apiServer = new ConnectiveServerFactory().setPort(6970).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		}
		apiServer.registerProcessor(new APIProcessor());
		System.out.println("Starting Emulated Feral Director server...");
		ConnectiveHTTPServer directorServer = new ConnectiveServerFactory().setImplementation(DirectorServer.class)
				.setPort(6969).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		directorServer.registerProcessor(new DirectorProcessor());
		System.out.println("Starting Emulated Feral Fake Payment server...");
		ConnectiveHTTPServer paymentServer = new ConnectiveServerFactory().setPort(6971)
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		paymentServer.registerProcessor(new PaymentsProcessor());
		System.out.println("Starting Emulated Feral Game server...");
		ServerSocket gameServer = new ServerSocket(6968, 0, InetAddress.getByName("0.0.0.0"));
		new GameServer().run(gameServer);
		System.out.println("Successfully started emulated servers.");
		paymentServer.waitExit();
		directorServer.waitExit();
		apiServer.waitExit();
		gameServer.close();
	}

}
