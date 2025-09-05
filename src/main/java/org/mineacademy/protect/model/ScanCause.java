package org.mineacademy.protect.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ScanCause {

	MANUAL("manual"),
	PERIOD("period"),
	PLAYER_JOIN("player join"),
	PLAYER_DEATH("player death"),
	WORLD_CHANGE("world change"),
	COMMAND("command"),
	INVENTORY_OPEN("inventory open"),
	ITEM_CLICK("item click"),
	ITEM_SPAWN("item spawn");

	@Getter
	private final String key;

	@Override
	public String toString() {
		return this.key;
	}
}
