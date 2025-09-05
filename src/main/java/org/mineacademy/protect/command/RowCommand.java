package org.mineacademy.protect.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.mineacademy.protect.menu.ItemMenu;
import org.mineacademy.protect.model.db.HoldsItems;
import org.mineacademy.protect.model.db.ProtectRow;
import org.mineacademy.protect.model.db.ProtectTable;

/**
 * An internal command to manipulate rows.
 */
final class RowCommand extends ProtectSubCommand {

	RowCommand() {
		super("row");

		this.setUsage("<param> <table> <rowId>");
		this.setMinArguments(3);
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleSubCommand#showInHelp()
	 */
	@Override
	protected boolean showInHelp() {
		return false;
	}

	@Override
	protected void onCommand() {
		this.checkConsole();

		final String param = this.args[0];
		final ProtectTable table = this.findEnum(ProtectTable.class, this.args[1]);
		final int rowId = this.findInt(2, "Invalid row number '{number}'.");

		tellInfo("Querying " + table.getKey() + " row id " + rowId + " async...");

		runTaskAsync(() -> {
			final ProtectRow row = table.getRow(rowId);
			this.checkNotNull(row, "Row id " + rowId + " not found in table " + table.getKey() + ".");

			if ("menu".equals(param)) {
				this.checkBoolean(row instanceof HoldsItems, "Row id " + rowId + " in table " + table.getKey() + " does not hold an item.");

				runTask(() -> ItemMenu.showTo(this.getPlayer(), (ProtectRow & HoldsItems) row));

			} else if ("remove".equals(param)) {
				row.delete();

				tellSuccess("&7Row id " + rowId + " removed from " + table.getKey() + " database.");

			} else
				returnInvalidArgs(param);
		});
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWord("menu", "remove");
			case 2:
				return this.completeLastWord(Arrays
						.asList(ProtectTable.values())
						.stream()
						.filter(table -> this.args[0].equals("menu") ? HoldsItems.class.isAssignableFrom(table.getRowClass()) : true)
						.collect(Collectors.toList()));
			case 3:
				return this.completeLastWord("");
		}

		return NO_COMPLETE;
	}
}