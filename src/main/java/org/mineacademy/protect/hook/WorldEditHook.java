package org.mineacademy.protect.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.protect.PlayerCache;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.settings.Settings;

import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Hook into WorldEdit to limit the number of blocks a player can place in an edit session.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorldEditHook {

	@Getter
	private static final WorldEditHook instance = new WorldEditHook();

	/*
	 * Register the WorldEdit operation limiter
	 */
	public void register() {
		WorldEdit.getInstance().getEventBus().register(new Object() {

			@Subscribe(priority = Priority.NORMAL)
			public void filterBlocks(final EditSessionEvent event) {
				if (event.getStage() != Stage.BEFORE_HISTORY)
					return;

				final Actor actor = event.getActor();
				final Player player = actor != null && actor.isPlayer() ? Bukkit.getPlayer(actor.getUniqueId()) : null;

				if (player == null || !player.isOnline() || Settings.Ignore.WORLDS.contains(player.getWorld().getName()) || player.hasPermission(Permissions.Bypass.WORLDEDIT))
					return;

				final PlayerCache cache = PlayerCache.from(player);
				cache.resetEditSession();

				// Prevent player repeating limited operations too quickly
				if (cache.getLastWorldEditViolation() != 0 && Settings.WorldEdit.WAIT_THRESHOLD.isUnderLimitMs(cache.getLastWorldEditViolation())) {
					Common.tellLater(1, player, "&cPlease wait " + Settings.WorldEdit.WAIT_THRESHOLD.getFormattedWaitTime(cache.getLastWorldEditViolation()) + " before the next WorldEdit command.");

					cache.setEditSessionBlocked(true);
				}

				event.setExtent(new AbstractDelegateExtent(event.getExtent()) {

					@Override
					public boolean setBlock(final BlockVector3 location, final BlockStateHolder block) throws WorldEditException {

						if (cache.isEditSessionBlocked())
							return false;

						// Check group limits
						final int groupLimit = Settings.WorldEdit.getGroupLimit(player);

						if (cache.getTotalBlocksPlaced() + 1 > groupLimit) {
							blockEditSession(player, cache, "Block limit " + groupLimit + "x exceeded.");

							return false;
						}

						// Check block limits
						final String materialName = block.getBlockType().getId().split("\\:")[1];
						final int blockLimit = Settings.WorldEdit.BLOCK_LIMIT.getOrDefault(materialName, -1);

						if (blockLimit != -1) {
							final int placedAmount = cache.getBlocksPlaced(materialName) + 1;

							if (placedAmount > blockLimit) {
								blockEditSession(player, cache, ChatUtil.capitalizeFully(materialName) + (blockLimit == 0 ? " is prohibited." : " limit " + blockLimit + "x exceeded."));

								return false;
							}

							cache.setBlocksPlaced(materialName, placedAmount);
						}

						cache.increaseTotalBlocksPlaced();
						return super.setBlock(location, block);
					}
				});
			}
		});
	}

	/*
	 * Block the edit session and inform the player
	 */
	private void blockEditSession(final Player player, final PlayerCache cache, final String blockMessage) {
		if (!cache.isEditSessionBlocked()) {
			Common.tellLater(1, player, "&6WorldEdit operation limited. " + blockMessage);

			cache.setLastWorldEditViolation(System.currentTimeMillis());
		}

		cache.setEditSessionBlocked(true);
	}
}
