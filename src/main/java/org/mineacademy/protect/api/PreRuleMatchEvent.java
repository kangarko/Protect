package org.mineacademy.protect.api;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.event.SimpleEvent;
import org.mineacademy.protect.operator.Rule;

import lombok.Getter;
import lombok.Setter;

/**
 * An event that is executed when a rule or a handler matches a message.
 * <p>
 * The event is fired before the rule edits the message in any way.
 */
@Getter
@Setter
public final class PreRuleMatchEvent extends SimpleEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The player interacting with the item, null if none
	 */
	@Nullable
	private final Player player;

	/**
	 * The item being matched
	 */
	private final ItemStack item;

	/**
	 * The matching rule
	 */
	private final Rule rule;

	/**
	 * Is the event cancelled?
	 */
	private boolean cancelled;

	public PreRuleMatchEvent(@Nullable Player player, ItemStack item, Rule rule) {
		super(!Bukkit.isPrimaryThread());

		this.player = player;
		this.item = item;
		this.rule = rule;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}