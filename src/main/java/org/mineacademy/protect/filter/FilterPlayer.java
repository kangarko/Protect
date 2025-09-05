package org.mineacademy.protect.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.protect.model.TemporaryStorage;
import org.mineacademy.protect.model.db.ProtectRow;

public final class FilterPlayer extends Filter {

	private final Set<String> playerNames = new HashSet<>();

	public FilterPlayer() {
		super("player");
	}

	@Override
	public boolean isApplicable(Table table) {
		return true;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"player:<name|name2> - Show results from the given players. Separate multiple by |.",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		final Set<String> names = new HashSet<>();

		names.addAll(TemporaryStorage.getPlayerNames());
		names.addAll(Common.getPlayerNames());

		return names;
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.playerNames.clear();

		for (final String split : value.split("\\|"))
			this.playerNames.add(split);

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return this.playerNames.contains(((ProtectRow) row).getPlayer());
	}
}
