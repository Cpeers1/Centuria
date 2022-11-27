package org.asf.centuria.discord.handlers.discord.interactions.buttons;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.discord.LinkUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public class DownloadSingleplayerLauncherHandler {

	/**
	 * Handles the singleplayer launcher download button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Find owner UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Check link
		if (!LinkUtils.isPairedWithCenturia(userID)) {
			// Return error
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
			msg.content("Could not locate a Centuria account linked with your Discord account.");
			msg.ephemeral(true);
			return event.reply(msg.build());
		} else {
			new Thread(() -> {
				try {
					// Find account
					CenturiaAccount account = LinkUtils.getAccountByDiscordID(userID);
					event.deferReply().block();

					// Download
					InputStream strm = new URL("https://aerialworks.ddns.net/extra/emuferal/Launcher.zip").openStream();

					// Recompress in memory
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					ZipOutputStream zip = new ZipOutputStream(output);
					ZipInputStream inp = new ZipInputStream(strm);
					while (true) {
						ZipEntry entry = inp.getNextEntry();
						if (entry == null)
							break;
						if (entry.getName().startsWith("java-"))
							continue;
						if (entry.getName().endsWith("Launcher.jar")) {
							zip.putNextEntry(new ZipEntry(entry.getName()));
							// Okay we need to edit the launcher so it isnt as massive
							String[] allowedPaths = new String[] { "emulogo_purple", "org/apache/commons", "org/asf",
									"org/tukaani" };
							ByteArrayOutputStream o = new ByteArrayOutputStream();
							byte[] data = inp.readAllBytes();
							JarInputStream inpJar = new JarInputStream(new ByteArrayInputStream(data));
							Manifest man = null;
							while (true) {
								ZipEntry jarEntry = inpJar.getNextEntry();
								if (jarEntry == null)
									break;
								if (jarEntry.getName().equals("META-INF/MANIFEST.MF")) {
									man = new Manifest();
									man.read(inpJar);
								}
							}
							inpJar.close();
							inpJar = new JarInputStream(new ByteArrayInputStream(data));
							JarOutputStream jar = new JarOutputStream(o, man);
							while (true) {
								ZipEntry jarEntry = inpJar.getNextEntry();
								if (jarEntry == null)
									break;
								if (!Stream.of(allowedPaths).anyMatch(t -> jarEntry.getName().startsWith(t)))
									continue;
								jar.putNextEntry(new ZipEntry(jarEntry.getName()));
								if (jarEntry.isDirectory()) {
									jar.closeEntry();
									continue;
								}
								inpJar.transferTo(jar);
								jar.closeEntry();
							}
							jar.close();
							inpJar.close();
							zip.write(o.toByteArray());
							zip.closeEntry();
							continue;
						}
						zip.putNextEntry(new ZipEntry(entry.getName()));
						if (entry.isDirectory()) {
							zip.closeEntry();
							continue;
						}
						inp.transferTo(zip);
						zip.closeEntry();
					}
					strm.close();

					// Add account login
					String loginName = account.getLoginName();
					String uuid = account.getAccountID();
					String dsp = account.getDisplayName();
					zip.putNextEntry(new ZipEntry("inventories/"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("displaynames/"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + uuid + ".looks/"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + uuid + ".sanctuary.looks/"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + uuid + ".looks/active.look"));
					zip.write(account.getActiveLook().getBytes("UTF-8"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + uuid + ".sanctuary.looks/active.look"));
					zip.write(account.getActiveSanctuaryLook().getBytes("UTF-8"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + loginName));
					zip.write((uuid + "\n" + loginName).getBytes("UTF-8"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + uuid));
					zip.write((uuid + "\n" + loginName + "\n" + account.isPlayerNew() + "\n" + dsp + "\n0")
							.getBytes("UTF-8"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("displaynames/" + dsp));
					zip.write(uuid.getBytes("UTF-8"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + uuid + ".cred"));
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry("accounts/" + uuid + ".credsave"));
					zip.closeEntry();

					// Add inventory data
					addItemToZip(account.getPlayerInventory(), uuid, "1", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "10", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "100", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "102", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "104", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "105", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "110", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "111", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "2", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "201", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "3", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "300", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "302", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "303", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "304", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "311", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "4", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "400", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "5", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "6", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "7", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "8", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "9", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "avatars", zip);
					addItemToZip(account.getPlayerInventory(), uuid, "level", zip);

					// Close
					zip.close();
					output.close();

					// Build message
					MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
					msg.content(
							"Here you have a Singleplayer Launcher.\nThis specific zip INCLUDES your player data.\n\nNote that some items arent included for server security.\nPlease also note that due to file limits, the bundled java is not included, you will need to copy `java-17` from a online launcher to this launcher for the launcher to work.");

					// Add file
					msg.addFile("EmuFeral Singleplayer Launcher.zip", new ByteArrayInputStream(output.toByteArray()));
					event.getInteraction().getChannel().block().createMessage(msg.build()).block();
					event.deleteReply().block();
				} catch (Exception e) {
					event.editReply("Failed to create the launcher").block();
				}
			}).start();
		}

		// Default response
		return Mono.empty();
	}

	private static void addItemToZip(PlayerInventory inv, String uuid, String item, ZipOutputStream zipStrm)
			throws UnsupportedEncodingException, IOException {
		if (inv.containsItem(item))
			transferDataToZip(zipStrm, "inventories/" + uuid + "/" + item + ".json",
					inv.getItem(item).toString().getBytes("UTF-8"));
	}

	private static void transferDataToZip(ZipOutputStream zip, String file, byte[] data) throws IOException {
		zip.putNextEntry(new ZipEntry(file));
		zip.write(data);
		zip.closeEntry();
	}

}
