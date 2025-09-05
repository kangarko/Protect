package org.mineacademy.protect.listener;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.model.FastMatcher;
import org.mineacademy.protect.model.ScanCause;
import org.mineacademy.protect.operator.ProtectOperator.CloneItemException;
import org.mineacademy.protect.operator.Rule;
import org.mineacademy.protect.settings.Settings;

/**
 * Class responsible for scanning triggers.
 */
@AutoRegister
public final class ScanListener implements Listener {

	/**
	 * Scan when a player joins
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		// Wait to prevent most issues with other plugins and world changes
		Platform.runTask(5, () -> this.scanPlayerIf(ScanCause.PLAYER_JOIN, Settings.Scan.PLAYER_JOIN, event.getPlayer()));
	}

	/**
	 * Scan when a player dies
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDeath(PlayerDeathEvent event) {
		this.scanPlayerIf(ScanCause.PLAYER_DEATH, Settings.Scan.PLAYER_DEATH, event.getEntity());
	}

	/**
	 * Scan when players switch worlds
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Platform.runTask(5, () -> this.scanPlayerIf(ScanCause.WORLD_CHANGE, Settings.Scan.WORLD_CHANGE, event.getPlayer()));
	}

	/**
	 * Scan when players open their inventory
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!Settings.Scan.INVENTORY_OPEN) // Move to top for performance reasons
			return;

		if (event.getInventory() == null || !(event.getPlayer() instanceof Player)) {
			Debugger.debug("scan", "Null or non player inventory skipped for " + event.getPlayer());

			return;
		}

		final Player player = (Player) event.getPlayer();
		final Inventory inventory = event.getInventory();

		if (Remain.getLocation(inventory) == null) {
			Debugger.debug("scan", "Ignoring virtual " + inventory.getType() + " inventory for " + player.getName());

			return;
		}

		final InventoryType type = Remain.invokeInventoryViewMethod(event, "getType");
		final String title = Remain.invokeInventoryViewMethod(event, "getTitle");
		final String defaultTitle = type.getDefaultTitle();

		if (inventory.getSize() != type.getDefaultSize() && inventory.getSize() != type.getDefaultSize() * 2) {
			Debugger.debug("scan", "Ignoring " + type + " inventory with custom size (" + inventory.getSize() + " != default size " + type.getDefaultSize() + ")");

			return;
		}

		if ((type == InventoryType.CHEST || type == InventoryType.ENDER_CHEST) && !title.equals(defaultTitle) && !this.isSpecialCase(title, defaultTitle)) {
			Debugger.debug("scan", "Ignoring " + type + " inventory with custom title (" + title + " &r!= default " + defaultTitle + ")");

			return;
		}

		if (Settings.Ignore.INVENTORY_TYPES.contains(type)) {
			Debugger.debug("scan", "Ignoring inventory type " + type + " for " + player.getName());

			return;
		}

		if (Settings.Ignore.INVENTORY_TITLES.startsWith(title)) {
			Debugger.debug("scan", "Ignoring inventory title '" + title + "' for " + player.getName());

			return;
		}

		Rule.filterOpenContainer(ScanCause.INVENTORY_OPEN, player, inventory, title);
	}

	// Special cases for newer Minecraft versions
	private boolean isSpecialCase(String title, String defTitle) {
		if (title.contains("container."))
			return true;

		if ((title.equals("Crafting") || title.equals("Large Chest") || title.equals("Minecart with Chest")) && (defTitle.equals("Chest") || defTitle.equals("Ender Chest")))
			return true;

		return title.equals("Minecart with Hopper") && defTitle.equals("Item Hopper");
	}

	/**
	 * Scan for item use
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL || event.getItem() == null)
			return;

		final Player player = event.getPlayer();

		// Fix bug in older Spigot versions where the event is called while browsing GUI
		if (player.hasMetadata(Menu.TAG_MENU_CURRENT))
			return;

		final String materialName = CompMaterial.fromItem(event.getItem()).name();

		Platform.runTask(1, () -> {
			for (final FastMatcher matcher : Settings.Scan.ITEM_USE)
				if (matcher.find(materialName))
					Rule.filterPlayer(ScanCause.ITEM_CLICK, player);
		});
	}

	/**
	 * Scan when items spawn
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onItemSpawn(ItemSpawnEvent event) {
		final Item item = event.getEntity();

		if (Settings.Scan.ITEM_SPAWN && item.getItemStack() != null) {
			for (final String metadataKey : Settings.Ignore.METADATA_ITEM)
				if (item.hasMetadata(metadataKey)) {
					Debugger.debug("scan", "Ignoring dropped item metadata " + metadataKey + " for " + item);

					return;
				}

			try {
				Rule.filterCause(ScanCause.ITEM_SPAWN, item.getLocation(), item);

			} catch (CloneItemException | EventHandledException ex) {
				if (ex instanceof EventHandledException) {
					if (((EventHandledException) ex).isCancelled())
						event.setCancelled(true);

				} else
					event.setCancelled(true);
			}
		}
	}

	/**
	 * Scan on command
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		// Wait to let the command give the player items to be scanned
		Platform.runTask(2, () -> this.scanPlayerIf(ScanCause.COMMAND, Settings.Scan.COMMAND.startsWith(event.getMessage()), event.getPlayer()));
	}

	/**
	 * Scan if the given toggle is enabled for the the player and his
	 * inventory.
	 *
	 * @param enabled
	 * @param player
	 */
	private void scanPlayerIf(ScanCause cause, boolean enabled, Player player) {
		if (enabled)
			Rule.filterPlayer(cause, player);
	}

}