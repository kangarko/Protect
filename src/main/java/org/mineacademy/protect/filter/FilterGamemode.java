package org.mineacademy.protect.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.protect.model.db.ProtectRow;

public final class FilterGamemode extends Filter {

	// The values we are filtering for. These fields are reset every time the filter is used
	private final Set<GameMode> gamemodes = new HashSet<>();

	// Make a public no arguments constructor
	public FilterGamemode() {
		super("gamemode");
	}

	// Return where the filter can be used, i.e. true means everywhere, or "return table == Table.ITEMS;"
	// for filters used on the items table only
	@Override
	public boolean isApplicable(Table table) {
		return true;
	}

	// Return how the filter is displayed in help of the command
	@Override
	public String[] getUsages() {
		return new String[] {
				"gamemode:<gamemode|gamemode2> - Show results for the given gamemode.",
		};
	}

	// Tab complete value values for the filter
	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Common.convertArrayToList(ReflectionUtil.getEnumValues(GameMode.class), mode -> ReflectionUtil.getEnumName(mode).toLowerCase());
	}

	// Load fields when the filter is used
	// Return true if the value is valid, false otherwise
	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.gamemodes.clear();

		for (final String split : value.split("\\|"))
			try {
				this.gamemodes.add(GameMode.valueOf(split.toUpperCase()));

			} catch (final IllegalArgumentException ex) {
				Messenger.error(audience, "No such gamemode '" + split + "'. Available: " + Common.join(GameMode.values()));

				return false;
			}

		return true;
	}

	// Filter the database entries
	// You can cast Row to Item, Transaction or Command depending on #isApplicable(Table)
	@Override
	public boolean canDisplay(Row row) {
		return this.gamemodes.contains(((ProtectRow) row).getGamemode());
	}
}
