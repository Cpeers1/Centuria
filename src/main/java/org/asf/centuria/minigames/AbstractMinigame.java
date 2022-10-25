package org.asf.centuria.minigames;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;

/**
 * 
 * Minigame abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AbstractMinigame {

	private boolean messagesInitialized = false;
	private HashMap<String, Method> minigameMessageHandlers = new HashMap<String, Method>();

	/**
	 * Handles minigame message
	 * 
	 * @param player  Player sending the packet
	 * @param message Minigame message to handle
	 */
	public void handleMessage(Player player, MinigameMessagePacket message) {
		if (!messagesInitialized) {
			messagesInitialized = true;
			for (Method mth : getClass().getMethods()) {
				if (mth.isAnnotationPresent(MinigameMessage.class)) {
					// Check params
					if (mth.getParameterCount() == 2 && mth.getParameters()[0].getType().isAssignableFrom(Player.class)
							&& mth.getParameters()[1].getType().isAssignableFrom(XtReader.class)
							&& !Modifier.isStatic(mth.getModifiers()) && !Modifier.isAbstract(mth.getModifiers())) {
						try {
							mth.setAccessible(true);
							MinigameMessage msgI = (MinigameMessage) mth.getAnnotation(MinigameMessage.class);
							minigameMessageHandlers.put(msgI.value(), mth);
						} catch (Exception e) {
							Centuria.logger.error("Failed to register minigame message: " + mth.getName()
									+ " (minigame: " + getClass().getSimpleName() + ")", e);
						}
					}
				}
			}
		}

		// Find handler
		if (minigameMessageHandlers.containsKey(message.command)) {
			XtReader rd = new XtReader(message.data);
			try {
				minigameMessageHandlers.get(message.command).invoke(this, player, rd);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Centuria.logger.error("Minigame error! (minigame: " + getClass().getSimpleName() + ")", e);
			}
		} else
			Centuria.logger.error("No minigame message handler for: " + message.command + " (minigame: "
					+ getClass().getSimpleName() + ", data: " + message.data + ")");
	}

	/**
	 * Instantiates the game
	 * 
	 * @return New AbstractMinigame instance
	 */
	public abstract AbstractMinigame instantiate();

	/**
	 * Checks if this minigame supports the given minigame level ID
	 * 
	 * @param levelID Level ID
	 * @return True if this minigame handler supports the given minigame level
	 */
	public abstract boolean canHandle(int levelID);

	/**
	 * Called when a player joins a minigame
	 * 
	 * @param player Player joining the minigame
	 */
	public abstract void onJoin(Player player);

}
