package org.mineacademy.protect.filter;

import java.util.Arrays;
import java.util.Collection;

import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.protect.model.db.ProtectTable;
import org.mineacademy.protect.model.db.Transaction;

public final class FilterAmount extends Filter {

	private RangedValue amount;

	public FilterAmount() {
		super("amount");
	}

	@Override
	public boolean isApplicable(Table table) {
		return table == ProtectTable.TRANSACTION;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"amount:<min-max> or amount:amount - Show results in the given amount range.",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Arrays.asList("1-10", "64", "500");
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		if ("-".equals(value)) {
			Messenger.error(audience, "Amount cannot be empty.");

			return false;
		}

		if (value.charAt(value.length() - 1) == '-' || value.charAt(0) == '-') {
			Messenger.error(audience, "Amount range must be in the format 'min-max' or 'value'. The value must be positive.");

			return false;
		}

		try {
			this.amount = RangedValue.fromString(value);

			if (this.amount.getMinLong() < 0 || this.amount.getMaxLong() < 0) {
				Messenger.error(audience, "Amount cannot be negative.");

				return false;
			}

			return true;

		} catch (final Throwable ex) {
			Messenger.error(audience, "Invalid amount or amount range '" + value + "': " + ex.getMessage());

			return false;
		}
	}

	@Override
	public boolean canDisplay(Row row) {
		final double amount = ((Transaction) row).getAmount();

		return this.amount.isInRangeDouble(amount);
	}
}
