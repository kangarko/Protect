package org.mineacademy.protect.model;

import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Converts the armor slot from NMS to Bukkit and vice versa
 */
@Getter
@RequiredArgsConstructor
public enum ArmorSlot {

	/**
	 * The helmet slot
	 */
	HELMET(103, 3, 9 * 2 + 1),

	/**
	 * The chestplate slot
	 */
	CHESTPLATE(102, 2, 9 * 2 + 2),

	/**
	 * The leggings slot
	 */
	LEGGINGS(101, 1, 9 * 2 + 3),

	/**
	 * The boots slot
	 */
	BOOTS(100, 0, 9 * 2 + 4),

	/**
	 * The off hand slot
	 */
	OFF_HAND(-106, 8, 9 * 2 + 7) {
		@Override
		public boolean isAvailable() {
			return HAS_OFF_HAND;
		}
	};

	/**
	 * Whether the server version supports off-hand slot
	 */
	public static final boolean HAS_OFF_HAND = MinecraftVersion.atLeast(V.v1_9);

	/**
	 * The NMS slot
	 */
	private final int nmsSlot;

	/**
	 * The Bukkit slot
	 */
	private final int invSlot;

	/**
	 * The menu slot
	 */
	private final int menuSlot;

	public boolean isAvailable() {
		return true;
	}

	/**
	 * Get the Bukkit slot from the NMS slot
	 *
	 * @param nmsSlot
	 * @return
	 */
	public static ArmorSlot fromNms(final int nmsSlot) {
		for (final ArmorSlot armorSlot : values())
			if (armorSlot.getNmsSlot() == nmsSlot)
				return armorSlot;

		return null;
	}
}