package org.asf.emuferal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class APIProcessor extends HttpUploadProcessor {

	public static String KeyID = UUID.randomUUID().toString();
	private static SecureRandom rnd = new SecureRandom();

	@Override
	public void process(String contentType, Socket client, String method) {
		String path = this.getRequestPath();
		try {
			byte[] body = new byte[0];
			if (method.toUpperCase().equals("POST")) {
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
				body = strm.toByteArray();
				strm.close();
			}

			switch (path) {
			case "/ca/request-token": {
				// Hardcoded response as i have no clue how to do this
				this.setBody(
						"{\"challenge\":\"kOLl8r71tG1343qobkIvdJSGuXxUZBQUtHTq7Npe91l51TrpaGLZf4nPIjSCNxniUdpdHvOfcCzV2TQRn5MXab08vwGizt0NiDmzAdWrzQMYDjgTYz7Xqbzqds2LaYTa\",\"iv\":\"03KJ2tNeasisn7vI42W49IJpObpQirvu\"}"
								.getBytes());
				break;
			}
			case "/a/authenticate": {
				// Parse body
				JsonObject login = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();

				// Generate new JWT
				String id = UUID.randomUUID().toString();
				if (!new File("accounts").exists())
					new File("accounts").mkdirs();
				File uf = new File("accounts/" + login.get("username").getAsString());
				if (uf.exists()) {
					id = Files.readAllLines(uf.toPath()).get(0);
				} else {
					// Save account details
					Files.writeString(uf.toPath(), id + "\n" + login.get("username").getAsString());
					Files.writeString(new File("accounts/" + id).toPath(),
							id + "\n" + login.get("username").getAsString() + "\ntrue\n"
									+ login.get("username").getAsString() + "\n"
									+ new File("accounts").listFiles().length);
				}

				// Check password
				File credentialFile = new File("accounts/" + id + ".cred");
				if (credentialFile.exists()) {
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
					byte[] cHash = getHash(salt, login.get("password").getAsString().toCharArray());

					// Compare hashes
					if (hash.length != cHash.length) {
						// Not sure what to send but sending this causes an error in the next request
						// triggering the client into saying invalid password.
						this.setResponseCode(200);
						return;
					}
					for (int i = 0; i < hash.length; i++) {
						if (hash[i] != cHash[i]) {
							// Not sure what to send but sending this causes an error in the next request
							// triggering the client into saying invalid password.
							this.setResponseCode(200);
							return;
						}
					}
				} else if (new File("accounts/" + id + ".credsave").exists()) {
					// Generate salt and hash
					byte[] salt = salt();
					byte[] hash = getHash(salt, login.get("password").getAsString().toCharArray());

					// Save
					FileOutputStream strm = new FileOutputStream(credentialFile);
					strm.write(ByteBuffer.allocate(4).putInt(salt.length).array());
					strm.write(salt);
					strm.write(ByteBuffer.allocate(4).putInt(hash.length).array());
					strm.write(hash);
					strm.close();

					// Delete request
					new File("accounts/" + id + ".credsave").delete();
				}

				JsonObject headers = new JsonObject();
				headers.addProperty("alg", "RS256");
				headers.addProperty("kid", KeyID);
				headers.addProperty("typ", "JWT");
				String headerD = Base64.getUrlEncoder().encodeToString(headers.toString().getBytes("UTF-8"));

				JsonObject payload = new JsonObject();
				payload.addProperty("iat", System.currentTimeMillis() / 1000);
				payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (24 * 60 * 60));
				payload.addProperty("iss", "EmuFeral");
				payload.addProperty("sub", "EmuFeral");
				payload.addProperty("uuid", id);
				String payloadD = Base64.getUrlEncoder().encodeToString(payload.toString().getBytes("UTF-8"));

				// Send response
				JsonObject response = new JsonObject();
				response.addProperty("uuid", id);
				response.addProperty("refresh_token", id);
				response.addProperty("auth_token", headerD + "." + payloadD + "."
						+ Base64.getUrlEncoder().encodeToString("NO_SIGNATURE".getBytes()));
				response.addProperty("rename_required", !credentialFile.exists()); // request user name if new or
																					// migrating
				response.addProperty("rename_required_key", "");
				response.addProperty("email_update_required", false);
				response.addProperty("email_update_required_key", "");
				setBody(response.toString());
				break;
			}
			case "/dn/update_display_name": {
				// Parse body
				JsonObject change = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
				String newName = change.get("new_display_name").getAsString();

				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());
				JsonObject jP = JsonParser
						.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
						.getAsJsonObject();

				// Find account
				String id = UUID.randomUUID().toString();
				String name = "spark";
				String account = name;
				String uid = "0";
				File uf = new File("accounts/" + jP.get("uuid").getAsString());
				boolean newUser = true;
				if (uf.exists()) {
					id = Files.readAllLines(uf.toPath()).get(0);
					account = Files.readAllLines(uf.toPath()).get(1);
					newUser = Files.readAllLines(uf.toPath()).get(2).equals("true");
					name = Files.readAllLines(uf.toPath()).get(3);
					uid = Files.readAllLines(uf.toPath()).get(4);
				} else {
					this.setResponseCode(403);
					this.setResponseMessage("Access denied");
					return;
				}

				// Save new name
				name = newName;
				Files.writeString(new File("accounts/" + id).toPath(),
						id + "\n" + account + "\n" + newUser + "\n" + name + "\n" + uid);

				// Tell authorization to save password
				new File("accounts/" + id + ".credsave").createNewFile();

				break;
			}
			case "/u/user": {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());
				JsonObject payload = JsonParser
						.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
						.getAsJsonObject();

				// Find account
				String id = UUID.randomUUID().toString();
				String name = "spark";
				String account = name;
				File uf = new File("accounts/" + payload.get("uuid").getAsString());
				boolean newUser = true;
				if (uf.exists()) {
					id = Files.readAllLines(uf.toPath()).get(0);
					account = Files.readAllLines(uf.toPath()).get(1);
					newUser = Files.readAllLines(uf.toPath()).get(2).equals("true");
					name = Files.readAllLines(uf.toPath()).get(3);
				} else {
					Files.writeString(uf.toPath(), id + "\n" + name);
					Files.writeString(new File("accounts/" + id).toPath(),
							id + "\n" + account + "\ntrue\n" + name + "\n" + new File("accounts").listFiles().length);
				}

				// Send a response
				JsonObject privacy = new JsonObject();
				privacy.addProperty("voice_chat", "following");
				File privacyFile = new File("accounts/" + id + ".privacy");
				if (privacyFile.exists()) {
					privacy = JsonParser.parseString(Files.readString(privacyFile.toPath())).getAsJsonObject();
				} else {
					Files.writeString(privacyFile.toPath(), privacy.toString());
				}
				JsonObject response = new JsonObject();
				response.addProperty("country_code", "US");
				response.addProperty("display_name", name);
				response.addProperty("enhanced_customization", newUser);
				response.addProperty("language", "en");
				response.add("privacy", privacy);
				response.addProperty("username", account);
				response.addProperty("uuid", id);
				setBody(response.toString());
				break;
			}
			case "/u/settings": {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());
				JsonObject payload = JsonParser
						.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
						.getAsJsonObject();

				// Find account
				String id = UUID.randomUUID().toString();
				String name = "spark";
				String account = name;
				File uf = new File("accounts/" + payload.get("uuid").getAsString());
				if (uf.exists()) {
					id = Files.readAllLines(uf.toPath()).get(0);
					account = Files.readAllLines(uf.toPath()).get(1);
					name = Files.readAllLines(uf.toPath()).get(3);
				} else {
					Files.writeString(uf.toPath(), id + "\n" + name);
					Files.writeString(new File("accounts/" + id).toPath(),
							id + "\n" + account + "\ntrue\n" + name + "\n" + new File("accounts").listFiles().length);
				}

				// Parse body
				JsonObject settings = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();

				token = token;

				break;
			}
			case "/xp/xp-details": {
				setBody(""); // should cause the question mark // TODO: levels
				break;
			}
			default:
				if (path.startsWith("/ca/auth/")) {
					// Build the JWT
					JsonObject headers = new JsonObject();
					headers.addProperty("alg", "RS256");
					headers.addProperty("kid", KeyID);
					headers.addProperty("typ", "JWT");
					String headerD = Base64.getUrlEncoder().encodeToString(headers.toString().getBytes("UTF-8"));

					JsonObject payload = new JsonObject();
					payload.addProperty("iat", System.currentTimeMillis() / 1000);
					payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (24 * 60 * 60));
					payload.addProperty("iss", "EmuFeral");
					payload.addProperty("sub", "EmuFeral");
					String payloadD = Base64.getUrlEncoder().encodeToString(payload.toString().getBytes("UTF-8"));

					// Send response
					JsonObject response = new JsonObject();
					response.addProperty("autorization_key", headerD + "." + payloadD + "."
							+ Base64.getUrlEncoder().encodeToString("NO_SIGNATURE".getBytes()));
					setBody(response.toString());
				} else {
					setResponseCode(400);
					path = path;
					setBody("{}");
				}
				break;
			}
		} catch (Exception e) {
			e = e;
		}
	}

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

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new APIProcessor();
	}

	@Override
	public String path() {
		return "/";
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
