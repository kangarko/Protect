package org.mineacademy.protect.api;

import org.bukkit.entity.Item;
import org.bukkit.event.HandlerList;
import org.mineacademy.protect.model.ScanCause;

import lombok.Getter;
import lombok.Setter;

/**
 * An event that is executed when an item is scanned upon spawning in the world.
 */
@Getter
@Setter
public final class ItemScanEvent extends AbstractScanEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The scanned item.
	 */
	private final Item item;

	public ItemScanEvent(ScanCause cause, Item item) {
		super(cause);

		this.item = item;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}