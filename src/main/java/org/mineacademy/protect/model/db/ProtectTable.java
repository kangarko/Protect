package org.mineacademy.protect.model.db;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.SimpleDatabase;
import org.mineacademy.fo.database.SimpleDatabase.TableCreator;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.Platform;

import lombok.Getter;

/**
 * Represents a table in the database
 */
@Getter
public enum ProtectTable implements Table {

	/**
	 * Command spy table
	 */
	COMMAND("command", CommandSpy.class) {

		@Override
		public void onTableCreate(TableCreator creator) {
			super.onTableCreate(creator);

			creator.add("Command", "text");
		}
	},

	/**
	 * Command spy table
	 */
	ITEMS("items", Item.class) {

		@Override
		public void onTableCreate(TableCreator creator) {
			super.onTableCreate(creator);

			creator
					.add("Items", "longtext")
					.add("RuleMatch", "text")
					.add("RuleName", "text")
					.add("Inventory", "longtext");
		}
	},

	/**
	 * Creative mode action log table
	 */
	CREATIVE("creative", CreativeLog.class) {

		@Override
		public void onTableCreate(TableCreator creator) {
			super.onTableCreate(creator);

			creator
					.add("Action", "text")
					.add("Target", "text");
		}
	},

	/**
	 * Commerce transactions table
	 */
	TRANSACTION("transaction", Transaction.class) {
		@Override
		public void onTableCreate(TableCreator creator) {
			super.onTableCreate(creator);

			creator
					.add("Plugin", "text")
					.add("TransactionType", "text")
					.add("Price", "float")
					.add("ShopOwner", "text")
					.add("ShopOwnerUid", "text")
					.add("Item", "longtext")
					.add("Amount", "int");
		}
	};

	private final String key;
	private final String name;
	private final Class<? extends Row> rowClass;

	ProtectTable(String key, Class<? extends Row> rowClass) {
		this.key = key;
		this.name = Platform.getPlugin().getName() + "_" + ChatUtil.capitalize(key);
		this.rowClass = rowClass;
	}

	@Override
	public SimpleDatabase getDatabase() {
		return Database.getInstance();
	}

	@Override
	public void onTableCreate(TableCreator creator) {
		creator
				.addAutoIncrement("Id", "int")
				.addDefault("Date", "datetime", "NULL")
				.add("Server", "text")
				.add("Location", "text")
				.add("World", "text")
				.add("Player", "text")
				.add("PlayerUid", "text")
				.add("Gamemode", "text")
				.setPrimaryColumn("Id");
	}
}
