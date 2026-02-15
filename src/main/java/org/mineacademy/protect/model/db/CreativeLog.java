package org.mineacademy.protect.model.db;

import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.database.SimpleResultSet;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.settings.Settings;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a logged creative mode action.
 */
@Getter
public final class CreativeLog extends ProtectRow {

	private final String action;

	private final String target;

	private CreativeLog(final Player player, final String action, final String target) {
		super(player.getLocation(), player.getName(), player.getUniqueId(), player.getGameMode());

		this.action = action;
		this.target = target;
	}

	CreativeLog(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);

		this.action = resultSet.getStringStrict("Action");
		this.target = resultSet.getStringStrict("Target");
	}

	@Override
	public ProtectTable getTable() {
		return ProtectTable.CREATIVE;
	}

	@Override
	public String getBypassPermission() {
		return Permissions.Bypass.CREATIVE;
	}

	@Override
	public String getNotifyPermission() {
		return Permissions.Notify.CREATIVE;
	}

	@Override
	public boolean isBroadcastEnabled() {
		return Settings.CreativeLog.BROADCAST;
	}

	@Override
	public String getBroadcastFormat() {
		return Settings.CreativeLog.BROADCAST_FORMAT;
	}

	@Override
	public String getDiscordChannel() {
		return Settings.CreativeLog.DISCORD_CHANNEL;
	}

	@Override
	public String getDiscordFormat() {
		return Settings.CreativeLog.DISCORD_FORMAT;
	}

	@Override
	public SerializedMap toMap() {
		return super.toMap().putArray(
				"Action", this.action,
				"Target", this.target);
	}

	@Override
	protected String replaceVariables(String format) {
		return Variables.builder().placeholderArray(
				"action", this.action,
				"target", ChatUtil.capitalizeFully(this.target)).replaceLegacy(super.replaceVariables(format));
	}

	@Override
	protected SimpleComponent onChatLineCreate(SimpleComponent component) {
		component = component.appendMiniAmpersand("&7: &f" + this.action + " ");
		component = component.appendMiniAmpersand("&e" + ChatUtil.capitalizeFully(this.target));
		component = component.onHoverLegacy(
				"&7Action: &f" + this.action,
				"&7Target: &f" + ChatUtil.capitalizeFully(this.target),
				"",
				"&7Click to copy target.");
		component = component.onClickSuggestCmd(this.target);

		return component;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Log a creative mode action.
	 *
	 * @param player
	 * @param action
	 * @param target
	 */
	public static void log(@NonNull final Player player, @NonNull final String action, @NonNull final String target) {
		new CreativeLog(player, action, target)
				.broadcast()
				.saveIfSenderNotBypasses(player);
	}
}
