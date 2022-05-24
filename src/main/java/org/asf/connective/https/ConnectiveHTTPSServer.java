package org.asf.connective.https;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.asf.rats.ConnectiveHTTPServer;

public class ConnectiveHTTPSServer extends ConnectiveHTTPServer {

	private File keystoreFile = null;
	private SSLContext context = null;
	private char[] keystorePassword;

	public ConnectiveHTTPSServer() {
		port = 8043;
	}

	public void assignAsMainImplementation() {
		implementation = this;
	}

	@Override
	public void start() throws IOException {
		if (socket != null)
			throw new IllegalStateException("Server already running!");

		if (keystoreFile == null) {
			String keystore = "keystore.jks";
			String dir = System.getProperty("rats.config.dir") == null ? "." : System.getProperty("rats.config.dir");
			if (this.ip.isLoopbackAddress()) {
				keystore = "keystore.localhost.jks";
			} else if (!this.ip.toString().equals("/0.0.0.0")) {
				String str = ip.getHostAddress().replaceAll(":|\\.", "-");

				keystore = "keystore-" + str + ".jks";
			}
			keystoreFile = new File(dir + "/" + keystore);
			keystorePassword = Files.readString(Path.of(dir + "/" + keystore + ".password")).toCharArray();
		}

		try {
			context = getContext(keystoreFile, keystorePassword);
		} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException
				| CertificateException e) {
			throw new RuntimeException(e);
		}

		super.start();
	}

	@Override
	public ServerSocket getServerSocket(int port, InetAddress addr) throws IOException {
		return context.getServerSocketFactory().createServerSocket(port, 0, addr);
	}

	public void setKeystoreFile(File keystore, char[] password) {
		keystoreFile = keystore;
		keystorePassword = password;
	}

	public SSLContext getContext(File keystore, char[] password)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException,
			CertificateException, FileNotFoundException, IOException {
		KeyStore mainStore = KeyStore.getInstance("JKS");
		mainStore.load(new FileInputStream(keystore), password);

		KeyManagerFactory managerFactory = KeyManagerFactory.getInstance("SunX509");
		managerFactory.init(mainStore, password);

		SSLContext cont = SSLContext.getInstance("TLS");
		cont.init(managerFactory.getKeyManagers(), null, null);

		return cont;
	}

}
