package org.mineacademy.protect.model;

import org.mineacademy.fo.PlayerUtil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The different inventory types
 */
@Getter
@RequiredArgsConstructor
public enum InventoryType {

	/**
	 * The regular inventory
	 */
	REGULAR("regular", "inventory", PlayerUtil.PLAYER_INV_SIZE),

	/**
	 * The armor inventory
	 */
	ARMOR("armor", "armor content", 9 * 4),

	/**
	 * The ender chest inventory
	 */
	ENDERCHEST("enderchest", "ender chest", PlayerUtil.PLAYER_INV_SIZE - 9);

	/**
	 * The non obfuscated key
	 */
	private final String key;

	/**
	 * The message string
	 */
	private final String messageString;

	/**
	 * The size of the inventory
	 */
	private final int inventorySize;
}