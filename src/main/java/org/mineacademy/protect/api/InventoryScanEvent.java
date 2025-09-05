package org.mineacademy.protect.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.mineacademy.protect.model.ScanCause;

import lombok.Getter;
import lombok.Setter;

/**
 * An event that is executed when an inventory is scanned.
 */
@Getter
@Setter
public final class InventoryScanEvent extends AbstractScanEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The player who owns the inventory, null if none.
	 */
	private final Player player;

	/**
	 * The scanned inventory.
	 */
	private final Inventory inventory;

	public InventoryScanEvent(Player player, ScanCause cause, Inventory inventory) {
		super(cause);

		this.player = player;
		this.inventory = inventory;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}