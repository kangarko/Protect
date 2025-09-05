package org.mineacademy.protect.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.protect.model.db.Item;
import org.mineacademy.protect.model.db.ProtectTable;
import org.mineacademy.protect.operator.Rules;

public final class FilterRule extends Filter {

	private final Set<String> rules = new HashSet<>();

	public FilterRule() {
		super("rule");
	}

	@Override
	public boolean isApplicable(Table table) {
		return table == ProtectTable.ITEMS;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"rule:<ruleName|ruleName2> - Show results for the given rule names.", };
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Rules.getInstance().getRuleNames();
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.rules.clear();

		for (final String split : value.split("\\|"))
			this.rules.add(split);

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return this.rules.contains(((Item) row).getRuleName());
	}
}
