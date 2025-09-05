package org.mineacademy.protect.listener;

import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.model.db.CommandSpy;
import org.mineacademy.protect.settings.Settings;

/**
 * Handle command spy
 */
@AutoRegister
public final class CommandLogListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCommand(final PlayerCommandPreprocessEvent event) {
		final Player player = event.getPlayer();
		final String message = event.getMessage();

		if (!Settings.CommandLog.ENABLED || message.isEmpty() || player.hasPermission(Permissions.Bypass.COMMAND))
			return;

		// Skip commands player does not have permission to run
		final Command command = Common.findCommand(message);
		String permission = null;

		if (command != null) {
			permission = command.getPermission();

			if (permission == null && command instanceof PluginCommand)
				permission = ((PluginCommand) command).getPlugin().getName().toLowerCase() + "." + command.getLabel();

			if (permission != null && !player.hasPermission(permission))
				return;
		}

		final String regexMessage = (message.startsWith("//") ? "\\/\\/" : "\\/") + message.substring(1);

		if (Settings.CommandLog.COMMAND_LIST.regexMatch(regexMessage)) {
			CommandSpy.log(player, message);

			if (Settings.CommandLog.BLOCK) {
				final String noPermMessage = command == null || command.getPermissionMessage() == null ? Settings.CommandLog.BLOCK_FALLBACK_MESSAGE : command.getPermissionMessage().replace("<permission>", command.getPermission());
				Common.tell(player, noPermMessage);

				event.setCancelled(true);
			}
		}
	}
}