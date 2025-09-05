package org.mineacademy.protect.model.db;

import java.sql.Timestamp;

import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.SimpleDatabase;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.protect.model.TemporaryStorage;
import org.mineacademy.protect.settings.Settings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The main database handler
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Database extends SimpleDatabase {

	/**
	 * The singleton instance
	 */
	@Getter
	private static final Database instance = new Database();

	@Override
	public Table[] getTables() {
		return ProtectTable.values();
	}

	@Override
	protected void onConnected() {
		for (final Table table : ProtectTable.values())
			Platform.runTaskAsync(() -> {
				for (final Row rawRow : this.getRows(table)) {
					final ProtectRow row = (ProtectRow) rawRow;

					if (!row.getPlayer().contains(" ") && !row.getPlayer().isEmpty())
						TemporaryStorage.addPlayer(row.getPlayer(), row.getPlayerUid());

					if (!row.getServer().isEmpty())
						TemporaryStorage.addServerName(row.getServer());

					if (!row.getLocation().getWorldName().isEmpty())
						TemporaryStorage.addWorldName(row.getLocation().getWorldName());

					if (row instanceof Transaction)
						TemporaryStorage.addPluginName(((Transaction) row).getPlugin());
				}
			});
	}

	/**
	 * Remove old db entries as per settings
	 */
	public void purgeOldEntries() {
		final Timestamp timestamp = new Timestamp(System.currentTimeMillis() - Settings.REMOVE_ENTRIES_OLDER_THAN.getTimeMilliseconds());

		for (final Table table : ProtectTable.values())
			this.deleteOlderThan(table, timestamp);
	}
}
