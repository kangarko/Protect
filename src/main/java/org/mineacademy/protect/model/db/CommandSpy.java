package org.mineacademy.protect.model.db;

import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.database.SimpleResultSet;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.settings.Settings;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a spied command.
 */
@Getter
public final class CommandSpy extends ProtectRow {

	private final String command;

	private CommandSpy(final Player player, final String command) {
		super(player.getLocation(), player.getName(), player.getUniqueId(), player.getGameMode());

		this.command = command;
	}

	CommandSpy(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);

		this.command = resultSet.getStringStrict("Command");
	}

	@Override
	public ProtectTable getTable() {
		return ProtectTable.COMMAND;
	}

	@Override
	public String getBypassPermission() {
		return Permissions.Bypass.COMMAND;
	}

	@Override
	public String getNotifyPermission() {
		return Permissions.Notify.COMMAND_SPY;
	}

	@Override
	public boolean isBroadcastEnabled() {
		return Settings.CommandLog.BROADCAST;
	}

	@Override
	public String getBroadcastFormat() {
		return Settings.CommandLog.BROADCAST_FORMAT;
	}

	@Override
	public String getDiscordChannel() {
		return Settings.CommandLog.DISCORD_CHANNEL;
	}

	@Override
	public String getDiscordFormat() {
		return Settings.CommandLog.DISCORD_FORMAT;
	}

	@Override
	public SerializedMap toMap() {
		return super.toMap().putArray("Command", this.command);
	}

	@Override
	protected String replaceVariables(String format) {
		return Variables.builder().placeholderArray(
				"type", Settings.CommandLog.BLOCK ? "block" : "spy",
				"command", this.command,
				"message", this.command).replaceLegacy(super.replaceVariables(format));
	}

	@Override
	protected SimpleComponent onChatLineCreate(SimpleComponent component) {
		component = component.appendMiniAmpersand("&7: &f");

		final int currentLength = component.toPlain().length();
		String commandCopy = this.command;

		if (currentLength + commandCopy.length() > 60)
			commandCopy = commandCopy.substring(0, 60 - currentLength - 3) + "...";

		component = component.appendMiniAmpersand(commandCopy);
		component = component.onHoverLegacy(
				"&7Command: &f" + this.command,
				"&7Click to copy.");

		component = component.onClickSuggestCmd(this.command);
		return component;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Log
	 *
	 * @param player
	 * @param command
	 */
	public static void log(@NonNull final Player player, @NonNull final String command) {
		new CommandSpy(player, command)
				.broadcast()
				.saveIfSenderNotBypasses(player);
	}
}
