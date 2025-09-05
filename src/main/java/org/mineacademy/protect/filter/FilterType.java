package org.mineacademy.protect.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.protect.model.db.ProtectTable;
import org.mineacademy.protect.model.db.Transaction;

public final class FilterType extends Filter {

	private final Set<Transaction.Type> type = new HashSet<>();

	public FilterType() {
		super("type");
	}

	@Override
	public boolean isApplicable(Table table) {
		return table == ProtectTable.TRANSACTION;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"type:<type|type2> - Show results for the given transaction type. Available: " + Common.join(Transaction.Type.values()),
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Common.convertArrayToList(Transaction.Type.values(), Transaction.Type::getKey);
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.type.clear();

		for (final String split : value.split("\\|"))
			try {
				this.type.add(Transaction.Type.fromKey(split));

			} catch (final IllegalArgumentException ex) {
				Messenger.error(audience, "No such transaction type '" + split + "'. Available: " + Common.join(Transaction.Type.values()));

				return false;
			}

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return this.type.contains(((Transaction) row).getType());
	}
}
