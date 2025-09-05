package org.mineacademy.protect.filter;

import java.util.Collection;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.protect.model.FastMatcher;
import org.mineacademy.protect.model.db.HoldsItems;
import org.mineacademy.protect.model.db.ProtectTable;

public final class FilterItem extends Filter {

	private FastMatcher material;

	public FilterItem() {
		super("item");
	}

	@Override
	public boolean isApplicable(Table table) {
		return table == ProtectTable.TRANSACTION || table == ProtectTable.ITEMS;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"item:<material> - Show results by material type. Use mineacademy.org/materials for names. Example: item:diamond_sword or item:*_sword",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Common.convertArrayToList(CompMaterial.values(), material -> material.name().toLowerCase());
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.material = FastMatcher.compile(value.toUpperCase());

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		final HoldsItems holdsItems = (HoldsItems) row;

		for (final ItemStack item : holdsItems.getItems())
			if (this.material.find(CompMaterial.fromItem(item).name()))
				return true;

		return false;
	}
}
