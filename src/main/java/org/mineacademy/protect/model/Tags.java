package org.mineacademy.protect.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Stores NBT tags.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Tags {

	/**
	 * Tag on items that were already cloned to prevent taking twice.
	 */
	public static final String CLONED = "Protect_Cloned";
}
