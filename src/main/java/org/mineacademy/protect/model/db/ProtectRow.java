package org.mineacademy.protect.model.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.database.RowLocationDate;
import org.mineacademy.fo.database.SimpleResultSet;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.protect.settings.Settings;

import lombok.Getter;
import lombok.ToString;

/**
 * Represents a row in the database
 */
@Getter
@ToString
public abstract class ProtectRow extends RowLocationDate {

	/**
	 * The server this event happened on
	 */
	private final String server;

	/**
	 * The player who caused this event
	 */
	private final String player;

	/**
	 * The player UUID who caused this event
	 */
	private final UUID playerUid;

	/**
	 * The gamemode of the player who caused this event
	 */
	private final GameMode gamemode;

	/**
	 * Create a new row
	 *
	 * @param resultSet
	 * @throws SQLException
	 */
	ProtectRow(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);

		this.server = resultSet.getStringStrict("Server");
		this.player = resultSet.getStringStrict("Player");
		this.playerUid = resultSet.getUniqueId("PlayerUid");
		this.gamemode = resultSet.getEnum("Gamemode", GameMode.class);
	}

	/**
	 * Create a new row
	 *
	 * @param id
	 * @param date
	 * @param server
	 * @param location
	 * @param player
	 * @param playerUid
	 * @param gamemode
	 */
	protected ProtectRow(Location location, String player, UUID playerUid, GameMode gamemode) {
		super(SerializeUtil.serializeLocationToSimple(location));

		this.server = Platform.getCustomServerName();
		this.player = player;
		this.playerUid = playerUid;
		this.gamemode = gamemode;
	}

	/**
	 * The permission to bypass logging this row
	 *
	 * @return
	 */
	public abstract String getBypassPermission();

	/**
	 * The permission to notify logging this row
	 *
	 * @return
	 */
	public abstract String getNotifyPermission();

	/**
	 * Whether broadcast is enabled for this row
	 *
	 * @return
	 */
	public abstract boolean isBroadcastEnabled();

	/**
	 * Get the broadcast format
	 *
	 * @return
	 */
	public abstract String getBroadcastFormat();

	/**
	 * Get the discord channel
	 *
	 * @return
	 */
	public abstract String getDiscordChannel();

	/**
	 * Get the discord format
	 *
	 * @return
	 */
	public abstract String getDiscordFormat();

	/**
	 * Replace variables in the given format
	 *
	 * @param format
	 * @return
	 */
	protected String replaceVariables(String format) {
		return Variables.builder().placeholderArray(
				"date", TimeUtil.getFormattedDate(this.getDate()),
				"server", this.getServer(),
				"server_name", this.getServer(),
				"location", this.getLocationFormatted(),
				"world", this.getLocation().getWorldName(),
				"player", this.getPlayer(),
				"player_uid", this.getPlayerUid() != null ? this.getPlayerUid().toString() : "",
				"gamemode", this.getGamemode()).replaceLegacy(format);
	}

	/**
	 * Called when creating the map
	 */
	@Override
	public SerializedMap toMap() {
		return super.toMap().putArray(
				"Server", this.server,
				"Player", this.player,
				"PlayerUid", this.playerUid != null ? this.playerUid.toString() : "NULL",
				"Gamemode", this.gamemode != null ? ReflectionUtil.getEnumName(this.gamemode) : "NULL");
	}

	/**
	 * Serialize this row into a line
	 *
	 * @return
	 */
	public final SimpleComponent toChatLine() {
		final List<String> hover = new ArrayList<>();

		hover.add("Date: &f" + TimeUtil.getFormattedDate(this.getDate()));
		hover.add("Server: &f" + this.getServer());
		hover.add("Location: &f" + this.getLocationFormatted());
		hover.add("");
		hover.add("Click to copy timestamp.");

		SimpleComponent component = SimpleComponent
				.empty()
				.appendMiniAmpersand("&8[&4X&8]")
				.onHoverLegacy(
						"Click to remove row id " + this.getId() + ".",
						"&cWarning: This action cannot be undone.")
				.onClickRunCmd("/" + Settings.MAIN_COMMAND_ALIASES.get(0) + " row remove " + this.getTable() + " " + this.getId())

				.appendPlain(" ")

				.appendMiniAmpersand("&7" + TimeUtil.getFormattedDateMonth(this.getDate()))
				.onHoverLegacy(Common.toArray(hover))

				.onClickSuggestCmd(this.getDate() + "");

		component = component
				.appendMiniAmpersand("&6 " + this.player);

		if (this.gamemode != null && this.playerUid != null) {
			component = component
					.onHoverLegacy(
							"Gamemode: &f" + ChatUtil.capitalizeFully(this.gamemode),
							"",
							"Click to copy UUID.")
					.onClickSuggestCmd(this.playerUid.toString());
		}

		return this.onChatLineCreate(component);
	}

	public final ProtectRow broadcast() {
		if (this.isBroadcastEnabled()) {
			String format = this.getBroadcastFormat();

			if (format != null && !format.equals("none")) {
				format = this.replaceVariables(format);

				Common.broadcastWithPerm(this.getNotifyPermission(), format);
			}
		}

		final String discordChannel = this.getDiscordChannel();

		if (HookManager.isDiscordSRVLoaded() && discordChannel != null && !discordChannel.equals("none")) {
			String format = this.getDiscordFormat();

			if (format != null && !format.equals("none")) {
				format = this.replaceVariables(format);

				HookManager.sendDiscordMessage(discordChannel, format);
			}
		}

		return this;
	}

	/**
	 * Called when creating the chat line
	 *
	 * @param component
	 */
	protected abstract SimpleComponent onChatLineCreate(SimpleComponent component);

	/**
	 * Write this row into the database
	 *
	 * @param sender
	 */
	public final void saveIfSenderNotBypasses(@Nullable final CommandSender sender) {
		if (sender == null || this.getBypassPermission() == null || !sender.hasPermission(this.getBypassPermission())) {
			this.insertToQueue();

		} else
			Common.logTimed(60 * 60 * 3, "Note: Not logging " + sender.getName() + "'s " + this.getTable().getKey() + " because he had '" + this.getBypassPermission() + "' permission.");
	}
}