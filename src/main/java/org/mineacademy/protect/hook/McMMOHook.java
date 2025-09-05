package org.mineacademy.protect.hook;

import org.bukkit.entity.Player;

import com.gmail.nossr50.api.AbilityAPI;

import lombok.NonNull;

/**
 * A hook for mcMMO
 */
public final class McMMOHook {

	private static McMMOInjector hook;

	public static void hook() {
		hook = new McMMOInjector();
	}

	/**
	 * Check if the player has any mcMMO ability
	 *
	 * @param player
	 * @return
	 */
	public static boolean isUsingAbility(final Player player) {
		try {
			return hook != null && hook.isUsingAbility(player);

		} catch (final NoClassDefFoundError err) {
			// Oh for plugin reloaders
			return false;
		}
	}
}

final class McMMOInjector {

	boolean isUsingAbility(@NonNull final Player player) {
		try {
			return AbilityAPI.isAnyAbilityEnabled(player);

		} catch (final NullPointerException ex) {
			return false; // some weird issue with mcMMO
		}
	}
}