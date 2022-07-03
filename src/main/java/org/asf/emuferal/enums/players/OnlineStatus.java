package org.asf.emuferal.enums.players;

public enum OnlineStatus {

	Offline(-1), LoggingIn(0), LoggedInToRoom(1);

	public int value;

	OnlineStatus(int value) {
		this.value = value;
	}
	
}
