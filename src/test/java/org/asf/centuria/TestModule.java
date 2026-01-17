package org.asf.centuria;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.SaveMode;
import org.asf.centuria.accounts.SaveSettings;
import org.asf.centuria.modules.ICenturiaModule;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.events.accounts.AccountPreloginEvent;
import org.asf.centuria.modules.events.chatcommands.ChatCommandEvent;
import org.asf.centuria.modules.events.chatcommands.ModuleCommandSyntaxListEvent;
import org.asf.centuria.modules.events.interactions.InteractionSuccessEvent;
import org.asf.centuria.modules.events.servers.DirectorServerStartupEvent;
import org.asf.centuria.modules.events.updates.ServerUpdateEvent;
import org.asf.connective.io.IoUtil;
import org.asf.connective.io.PrependedBufferStream;
import org.asf.connective.lambda.LambdaPushContext;
import org.asf.connective.lambda.LambdaRequestContext;

public class TestModule implements ICenturiaModule {

	@Override
	public String id() {
		return "test";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
	}

	@EventListener
	public void update(ServerUpdateEvent event) {
	}

	@EventListener
	public void interactionSuccess(InteractionSuccessEvent event) {
	}

	@EventListener
	public void prelogin(AccountPreloginEvent event) {
	}

	@EventListener
	public void serverStart(DirectorServerStartupEvent event) {
		event.getServer().registerHandler("test", (LambdaPushContext ctx) -> {
			byte[] read = IoUtil.readAllBytes(ctx.getRequest().getBodyStream());
			read = read;
		}, "POST");
		event.getServer().registerHandler("test", (LambdaPushContext ctx) -> {
			byte[] read = IoUtil.readAllBytes(ctx.getRequest().getBodyStream());
			read = read;
		}, "PUT");
		event.getServer().registerHandler("test", (LambdaRequestContext ctx) -> {
			ctx = ctx;
		}, "GET");
		event.getServer().registerHandler("test", (LambdaRequestContext ctx) -> {
			ctx = ctx;
		}, "DELETE");
		event.getServer().registerHandler("test", (LambdaRequestContext ctx) -> {
			ctx = ctx;
		}, "DELETE");
		event.getServer().registerHandler("test:12345", (LambdaRequestContext ctx) -> {
			String target = ctx.getRequestPath().substring(1);

			// Switch
			ctx.getResponse().switchProtocolsConnect(client -> {
				OutputStream output = client.getOutputStream();
				InputStream input = client.getInputStream();
				PrependedBufferStream buffer;
				if (input instanceof PrependedBufferStream)
					buffer = (PrependedBufferStream) input;
				else
					buffer = new PrependedBufferStream(input);
				input = buffer;
				while (true) {
					try {
						String cmd = readStreamLine(buffer);
						output.write(("HI " + cmd + "\n").getBytes("UTF-8"));
					} catch (IOException e) {
						client.closeConnection();
						break;
					}
				}
			});
		}, "CONNECT");
	}

	private String readStreamLine(PrependedBufferStream strm) throws IOException {
		// Read a number of bytes
		byte[] content = new byte[20480];
		int read = strm.read(content, 0, content.length);
		if (read <= -1) {
			// Failed
			return null;
		} else {
			// Trim array
			content = Arrays.copyOfRange(content, 0, read);

			// Find newline
			String newData = new String(content, "UTF-8");
			if (newData.contains("\n")) {
				// Found newline
				String line = newData.substring(0, newData.indexOf("\n"));
				int offset = line.length() + 1;
				int returnLength = content.length - offset;
				if (returnLength > 0) {
					// Return
					strm.returnToBuffer(Arrays.copyOfRange(content, offset, content.length));
				}
				return line.replace("\r", "");
			} else {
				// Read more
				while (true) {
					byte[] addition = new byte[20480];
					read = strm.read(addition, 0, addition.length);
					if (read <= -1) {
						// Failed
						strm.returnToBuffer(content);
						return null;
					}

					// Trim
					addition = Arrays.copyOfRange(addition, 0, read);

					// Append
					byte[] newContent = new byte[content.length + addition.length];
					for (int i = 0; i < content.length; i++)
						newContent[i] = content[i];
					for (int i = content.length; i < newContent.length; i++)
						newContent[i] = addition[i - content.length];
					content = newContent;

					// Find newline
					newData = new String(content, "UTF-8");
					if (newData.contains("\n")) {
						// Found newline
						String line = newData.substring(0, newData.indexOf("\n"));
						int offset = line.length() + 1;
						int returnLength = content.length - offset;
						if (returnLength > 0) {
							// Return
							strm.returnToBuffer(Arrays.copyOfRange(content, offset, content.length));
						}
						return line.replace("\r", "");
					}
				}
			}
		}
	}

	@EventListener
	public void registerCommands(ModuleCommandSyntaxListEvent event) {
		event.addCommandSyntaxMessage("test");
		event.addCommandSyntaxMessage("migrate");
		event.addCommandSyntaxMessage("creativesave");
		event.addCommandSyntaxMessage("switchsave");
	}

	@EventListener
	public void runCommand(ChatCommandEvent event) {
		if (event.getCommandID().equals("test")) {
			event.respond("Test 123");
			Centuria.systemMessage(event.getAccount().getOnlinePlayerInstance(), "test", true);
		} else if (event.getCommandID().equals("migrate")) {
			if (event.getAccount().getSaveMode() == SaveMode.MANAGED) {
				event.respond("Already using managed data");
				return;
			}

			event.getAccount().getOnlinePlayerInstance().client
					.sendPacket("%xt%mod:ft%-1%disconnect%Disconnected%Account data migration in progress%Log out%");
			event.getAccount().migrateSaveDataToManagedMode();
		} else if (event.getCommandID().equals("creativesave")) {
			if (event.getAccount().getSaveMode() != SaveMode.MANAGED) {
				event.respond("Not using managed data");
				return;
			}

			if (event.getAccount().getSaveManager().createSave("creative")) {
				PlayerInventory inv = event.getAccount().getSaveManager().getSaveSpecificInventoryOf("creative");
				SaveSettings settings = inv.getSaveSettings();
				settings.giveAllAvatars = true;
				settings.giveAllClothes = true;
				settings.giveAllCurrency = true;
				settings.giveAllFurnitureItems = true;
				settings.giveAllMods = true;
				settings.giveAllResources = true;
				settings.giveAllSanctuaryTypes = true;
				settings.giveAllWings = true;
				inv.writeSaveSettings();
				event.respond("Done");
			} else
				event.respond("Failed");
		} else if (event.getCommandID().equals("switchsave")) {
			if (event.getAccount().getSaveMode() != SaveMode.MANAGED) {
				event.respond("Not using managed data");
				return;
			}

			event.getAccount().getSaveManager().switchSave(event.getCommandArguments()[0]);
			event.getAccount().getOnlinePlayerInstance().client
					.sendPacket("%xt%mod:ft%-1%disconnect%Disconnected%Save switched%Log out%");
		}
	}

}
