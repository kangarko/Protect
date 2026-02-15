package org.mineacademy.protect.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.model.db.CreativeLog;
import org.mineacademy.protect.settings.Settings;

/**
 * Listener that logs block and entity actions for players in monitored gamemodes.
 */
@AutoRegister
public final class CreativeLogListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!Settings.CreativeLog.ENABLED || !Settings.CreativeLog.BLOCK_PLACE)
			return;

		final Player player = event.getPlayer();

		if (this.shouldSkip(player))
			return;

		CreativeLog.log(player, "placed", CompMaterial.fromBlock(event.getBlock()).toString());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!Settings.CreativeLog.ENABLED || !Settings.CreativeLog.BLOCK_BREAK)
			return;

		final Player player = event.getPlayer();

		if (this.shouldSkip(player))
			return;

		CreativeLog.log(player, "broke", CompMaterial.fromBlock(event.getBlock()).toString());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityPlace(EntityPlaceEvent event) {
		if (!Settings.CreativeLog.ENABLED || !Settings.CreativeLog.ENTITY_PLACE)
			return;

		final Player player = event.getPlayer();

		if (player == null || this.shouldSkip(player))
			return;

		CreativeLog.log(player, "spawned", event.getEntity().getType().toString());
	}

	private boolean shouldSkip(Player player) {
		if (player.hasPermission(Permissions.Bypass.CREATIVE))
			return true;

		return !Settings.CreativeLog.GAMEMODES.contains(player.getGameMode());
	}
}
