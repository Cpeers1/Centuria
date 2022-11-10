package org.asf.centuria.tools.legacyclienttools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import javax.net.ssl.SSLSocketFactory;

public class BasicProxyServer {

	public int localPort;

	public boolean encryptUpstream = true;
	public String remoteAddr;
	public int remotePort;

	private ServerSocket server;
	private ArrayList<ProxyClient> clients = new ArrayList<ProxyClient>();

	public class ProxyClient {
		public Socket local;
		public Socket remote;
	}

	public ProxyClient[] getClients() {
		while (true) {
			try {
				return clients.toArray(t -> new ProxyClient[t]);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	public void start() throws IOException {
		server = new ServerSocket(localPort, 0, InetAddress.getLoopbackAddress());
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		Thread th = new Thread(() -> {
			while (true) {
				try {
					Socket sock = server.accept();

					// Connect
					Thread th2 = new Thread(() -> {
						// Connect to remote
						try {
							Socket remoteSocket;
							if (encryptUpstream) {
								remoteSocket = factory.createSocket(remoteAddr, remotePort);
							} else {
								remoteSocket = new Socket(remoteAddr, remotePort);
							}
							
							// Client object
							ProxyClient cl = new ProxyClient();
							cl.local = sock;
							cl.remote = remoteSocket;
							clients.add(cl);
							
							// IO
							Thread io = new Thread(() -> {
								while (sock.isConnected()) {
									try {
										int i = sock.getInputStream().read();
										if (i == -1) 
											throw new IOException();
										remoteSocket.getOutputStream().write(i);
									} catch (Exception e) {
										try {
											sock.close();
											remoteSocket.close();
										} catch (IOException e1) {
										}
										clients.remove(cl);
										break;
									}
								}
							}, "Local -> Remote IO");
							io.setDaemon(true);
							io.start();
							io = new Thread(() -> {
								while (sock.isConnected()) {
									try {
										int i = remoteSocket.getInputStream().read();
										if (i == -1) 
											throw new IOException();
										sock.getOutputStream().write(i);
									} catch (Exception e) {
										try {
											sock.close();
											remoteSocket.close();
										} catch (IOException e1) {
										}
										clients.remove(cl);
										break;
									}
								}
							}, "Remote -> Local IO");
							io.setDaemon(true);
							io.start();							
						} catch (Exception e) {
							try {
								sock.close();
							} catch (IOException e1) {
							}
						}
					}, "Client connector");
					th2.setDaemon(true);
					th2.start();
				} catch (IOException e) {
					break;
				}
			}
		}, "Proxy Server");
		th.setDaemon(true);
		th.start();
	}

	public void stop() {
		// Disconnect all
		ProxyClient[] cls = getClients();
		for (ProxyClient cl : cls) {
			try {
				cl.local.close();
			} catch (Exception e) {
			}
			try {
				cl.remote.close();
			} catch (Exception e) {
			}
			clients.remove(cl);
		}

		// Close
		try {
			server.close();
		} catch (IOException e) {
		}
	}

}
