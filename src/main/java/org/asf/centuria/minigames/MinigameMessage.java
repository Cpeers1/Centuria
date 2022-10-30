package org.asf.centuria.minigames;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Minigame message handler
 * 
 * @author Sky Swimmer
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface MinigameMessage {
	public String value();
}
