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

public final class FilterPrice extends Filter {

	private RangedValue price;

	public FilterPrice() {
		super("price");
	}

	@Override
	public boolean isApplicable(Table table) {
		return table == ProtectTable.TRANSACTION;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"price:<min-max> or price:value - Show results in the given price range.",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Arrays.asList("10-20", "10");
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		if ("-".equals(value)) {
			Messenger.error(audience, "Price cannot be empty.");

			return false;
		}

		if (value.charAt(value.length() - 1) == '-' || value.charAt(0) == '-') {
			Messenger.error(audience, "Price range must be in the format 'min-max' or 'value'. The value must be positive.");

			return false;
		}

		try {
			this.price = RangedValue.fromString(value);

			if (this.price.getMinLong() < 0 || this.price.getMaxLong() < 0) {
				Messenger.error(audience, "Price cannot be negative.");

				return false;
			}

			return true;

		} catch (final Throwable ex) {
			Messenger.error(audience, "Invalid price or price range '" + value + "'.");

			return false;
		}
	}

	@Override
	public boolean canDisplay(Row row) {
		final double price = ((Transaction) row).getPrice();

		return this.price.isInRangeDouble(price);
	}
}
