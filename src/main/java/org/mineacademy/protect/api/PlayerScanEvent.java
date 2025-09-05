package org.mineacademy.protect.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.mineacademy.protect.model.ScanCause;

import lombok.Getter;
import lombok.Setter;

/**
 * An event that is executed when a player is scanned
 */
@Getter
@Setter
public final class PlayerScanEvent extends AbstractScanEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The player who owns the inventory, null if none.
	 */
	private final Player player;

	public PlayerScanEvent(Player player, ScanCause cause) {
		super(cause);

		this.player = player;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}