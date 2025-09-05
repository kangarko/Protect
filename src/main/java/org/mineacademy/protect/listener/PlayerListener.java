package org.mineacademy.protect.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.menu.InventoryMenu;
import org.mineacademy.protect.model.TemporaryStorage;
import org.mineacademy.protect.settings.Settings;

/**
 * A listener for player events
 */
@AutoRegister
public final class PlayerListener implements Listener {

	/**
	 * When a player joins, remove their name from the cache
	 * and refresh all inventories for all players
	 *
	 * @param event
	 */
	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		TemporaryStorage.removePlayer(player);
		this.refreshInventories(player);
	}

	/**
	 * When a player quits, add their name to the cache
	 * and refresh all inventories for all players
	 *
	 * @param event
	 */
	@EventHandler
	public void onQuit(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();

		TemporaryStorage.addPlayer(player);
		this.refreshInventories(player);
	}

	/*
	 * Refresh all inventories for all players
	 */
	private void refreshInventories(final Player target) {
		for (final Player online : Remain.getOnlinePlayers()) {
			final Menu otherMenu = Menu.getMenu(online);

			if (otherMenu instanceof InventoryMenu && ((InventoryMenu) otherMenu).isOwnedBy(target)) {
				online.closeInventory();

				Common.tell(online, SimpleComponent
						.fromMiniAmpersand("&7This inventory is no longer available as the player's game state has updated. ")
						.appendMiniAmpersand("&nClick here to reopen it")
						.onHoverLegacy("Click to reopen the inventory")
						.onClickSuggestCmd("/" + Settings.MAIN_COMMAND_ALIASES.get(0) + " inv " + ((InventoryMenu) otherMenu).getInventoryType().toString().toLowerCase() + " " + target.getName())
						.appendMiniAmpersand("&7."));
			}
		}
	}
}
