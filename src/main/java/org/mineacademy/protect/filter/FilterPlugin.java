package org.mineacademy.protect.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.protect.model.TemporaryStorage;
import org.mineacademy.protect.model.db.ProtectTable;
import org.mineacademy.protect.model.db.Transaction;

public final class FilterPlugin extends Filter {

	private final Set<String> plugins = new HashSet<>();

	public FilterPlugin() {
		super("plugin");
	}

	@Override
	public boolean isApplicable(Table table) {
		return table == ProtectTable.TRANSACTION;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"plugin:<plugin|plugin2> - Show results for the given plugin.", };
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return TemporaryStorage.getPluginNames();
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.plugins.clear();

		for (final String split : value.split("\\|"))
			this.plugins.add(split);

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return this.plugins.contains(((Transaction) row).getPlugin());
	}
}
