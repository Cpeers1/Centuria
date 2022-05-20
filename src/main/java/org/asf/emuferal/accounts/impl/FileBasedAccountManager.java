package org.asf.emuferal.accounts.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;

public class FileBasedAccountManager extends AccountManager {

	private static SecureRandom rnd = new SecureRandom();

	@Override
	public String authenticate(String username, char[] password) {
		// Check name validity
		if (!username.matches("^[A-Za-z0-9@\\-._#]+$"))
			return null;

		// Find the account
		String id = null;
		if (!new File("accounts").exists())
			new File("accounts").mkdirs();
		File uf = new File("accounts/" + username);
		if (uf.exists()) {
			try {
				id = Files.readAllLines(uf.toPath()).get(0);
			} catch (IOException e) {
			}
		} else
			return null;

		// If it has one, check password
		File credentialFile = new File("accounts/" + id + ".cred");
		if (hasPassword(id) && !isPasswordUpdateRequested(id)) {
			try {
				// Load credentials
				FileInputStream strm = new FileInputStream(credentialFile);
				int length = ByteBuffer.wrap(strm.readNBytes(4)).getInt();
				byte[] salt = new byte[length];
				for (int i = 0; i < length; i++) {
					salt[i] = (byte) strm.read();
				}
				length = ByteBuffer.wrap(strm.readNBytes(4)).getInt();
				byte[] hash = new byte[length];
				for (int i = 0; i < length; i++) {
					hash[i] = (byte) strm.read();
				}
				strm.close();

				// Get current hash
				byte[] cHash = getHash(salt, password);

				// Compare hashes
				if (hash.length != cHash.length) {
					return null;
				}
				for (int i = 0; i < hash.length; i++) {
					if (hash[i] != cHash[i]) {
						return null;
					}
				}
			} catch (IOException e) {
				return null;
			}
		}

		// Login is valid
		return id;
	}

	@Override
	public boolean isPasswordUpdateRequested(String userID) {
		return new File("accounts/" + userID + ".credsave").exists();
	}

	@Override
	public void makePasswordUpdateRequested(String userID) {
		try {
			new File("accounts/" + userID + ".credsave").createNewFile();
		} catch (IOException e) {
		}
	}

	@Override
	public boolean hasPassword(String userID) {
		return new File("accounts/" + userID + ".cred").exists();
	}

	@Override
	public boolean updatePassword(String userID, char[] password) {
		try {
			// Generate salt and hash
			byte[] salt = salt();
			byte[] hash = getHash(salt, password);

			// Save
			File credentialFile = new File("accounts/" + userID + ".cred");
			FileOutputStream strm = new FileOutputStream(credentialFile);
			strm.write(ByteBuffer.allocate(4).putInt(salt.length).array());
			strm.write(salt);
			strm.write(ByteBuffer.allocate(4).putInt(hash.length).array());
			strm.write(hash);
			strm.close();

			// Delete request
			if (new File("accounts/" + userID + ".credsave").exists())
				new File("accounts/" + userID + ".credsave").delete();
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public String register(String username) {
		// Check name validity
		if (!username.matches("^[A-Za-z0-9@\\-._#]+$"))
			return null;

		try {
			// Create folder
			if (!new File("accounts").exists())
				new File("accounts").mkdirs();

			// Find a ID that isn't in use
			String id = UUID.randomUUID().toString();
			while (new File("accounts/" + id).exists())
				id = UUID.randomUUID().toString();

			// Save account details
			File uf = new File("accounts/" + username);
			Files.writeString(uf.toPath(), id + "\n" + username);
			Files.writeString(new File("accounts/" + id).toPath(),
					id + "\n" + username + "\ntrue\n" + username + "\n" + new File("accounts").listFiles().length);

			// Return account ID
			return id;
		} catch (IOException e) {
		}

		return null; // Failure
	}

	@Override
	public EmuFeralAccount getAccount(String userID) {
		// Find the account
		File uf = new File("accounts/" + userID);
		if (uf.exists())
			try {
				return new FileBasedAccountObject(uf);
			} catch (IOException e) {
				return null;
			}
		else
			return null;
	}

	// Salt and hash
	private static byte[] salt() {
		byte[] salt = new byte[32];
		rnd.nextBytes(salt);
		return salt;
	}

	public static byte[] getHash(byte[] salt, char[] password) {
		KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			return null;
		}
	}

}
