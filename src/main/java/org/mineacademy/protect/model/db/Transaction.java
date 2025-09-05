package org.mineacademy.protect.model.db;

import java.sql.SQLException;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.database.SimpleResultSet;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.settings.Settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a transaction
 */
@Getter
public final class Transaction extends ProtectRow implements HoldsItems {

	private final String plugin;
	private final Type type;
	private final double price;
	@Nullable
	private final String shopOwnerName;
	@Nullable
	private final UUID shopOwnerUid;
	private final ItemStack item;
	private final int amount;

	/*
	 * Used to create new transactions
	 */
	private Transaction(Location shopLocation, Player buyer, String plugin, Type type, double price, Player showOwner, ItemStack item, int amount) {
		this(shopLocation, buyer.getName(), buyer.getUniqueId(), buyer.getGameMode(), plugin, type, price, showOwner.getName() == null ? "" : showOwner.getName(), showOwner.getUniqueId() == null ? null : showOwner.getUniqueId(), item, amount);
	}

	/*
	 * Used to create new transactions
	 */
	private Transaction(Location shopLocation, Player buyer, String plugin, Type type, double price, String showOwner, UUID showOwnerUid, ItemStack item, int amount) {
		this(shopLocation, buyer.getName(), buyer.getUniqueId(), buyer.getGameMode(), plugin, type, price, showOwner, showOwnerUid, item, amount);
	}

	private Transaction(Location location, String buyer, UUID buyerUid, GameMode gamemode, String plugin, Type type, double price, String showOwnerName, UUID shopOwnerUid, ItemStack item, int amount) {
		super(location, buyer, buyerUid, gamemode);

		this.plugin = plugin;
		this.type = type;
		this.price = price;
		this.shopOwnerName = showOwnerName;
		this.shopOwnerUid = shopOwnerUid;
		this.item = item;
		this.amount = amount;
	}

	Transaction(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);

		this.plugin = resultSet.getStringStrict("Plugin");
		this.type = resultSet.getEnumStrict("TransactionType", Transaction.Type.class);
		this.price = resultSet.getDoubleStrict("Price");
		this.shopOwnerName = resultSet.getString("ShopOwner"); // nullable
		this.shopOwnerUid = resultSet.getUniqueId("ShopOwnerUid"); // nullable
		this.item = SerializeUtil.deserializeItem(resultSet, "Item");
		this.amount = resultSet.getIntStrict("Amount");
	}

	@Override
	public ProtectTable getTable() {
		return ProtectTable.TRANSACTION;
	}

	@Override
	public String getBypassPermission() {
		return Permissions.Bypass.TRANSACTION;
	}

	@Override
	public String getNotifyPermission() {
		return Permissions.Notify.TRANSACTION;
	}

	@Override
	public boolean isBroadcastEnabled() {
		return Settings.TransactionLog.BROADCAST;
	}

	@Override
	public String getBroadcastFormat() {
		return this.isBuy() ? Settings.TransactionLog.BROADCAST_FORMAT_BUY : Settings.TransactionLog.BROADCAST_FORMAT_SELL;
	}

	@Override
	public String getDiscordChannel() {
		return Settings.TransactionLog.DISCORD_CHANNEL;
	}

	@Override
	public String getDiscordFormat() {
		return this.isBuy() ? Settings.TransactionLog.DISCORD_FORMAT_BUY : Settings.TransactionLog.DISCORD_FORMAT_SELL;
	}

	public boolean isBuy() {
		return this.type == Type.BUY;
	}

	@Override
	public ItemStack[] getItems() {
		return new ItemStack[] { this.item };
	}

	@Override
	public SerializedMap toMap() {
		return super.toMap().putArray(
				"Plugin", this.plugin,
				"TransactionType", this.type.toString(),
				"Price", String.valueOf(this.price),
				"ShopOwner", this.shopOwnerName == null ? "NULL" : this.shopOwnerName,
				"ShopOwnerUid", this.shopOwnerUid == null ? "NULL" : this.shopOwnerUid.toString(),
				"Item", this.item,
				"Amount", this.amount);
	}

	@Override
	protected String replaceVariables(String format) {
		return Variables.builder().placeholderArray(
				"plugin", this.plugin,
				"transaction_type", this.type.getLocalized(),
				"price", this.price,
				"price_formatted", this.getPriceWithCurrency(),
				"shop_owner", this.shopOwnerName,
				"shop_owner_uid", this.shopOwnerUid == null ? null : this.shopOwnerUid.toString(),
				"item_type", ChatUtil.capitalizeFully(this.item.getType()),
				"amount", this.amount).replaceLegacy(super.replaceVariables(format));
	}

	@Override
	protected SimpleComponent onChatLineCreate(SimpleComponent component) {

		component = component.appendMiniAmpersand(" ");

		component = component.appendMiniAmpersand("&7" + (this.type == Type.BUY ? "&a&lB" : "&6&lS") + "&r&7");
		component = component.onHoverLegacy(
				"&7Operation: &f" + this.type.getLocalized(),
				"&7Show owner: &f" + this.shopOwnerName,
				"&7Plugin: &f" + this.plugin);
		component = component.onClickSuggestCmd(this.plugin);

		component = component.appendMiniAmpersand(" ");

		if (this.shopOwnerName != null) {
			component = component.appendMiniAmpersand(this.shopOwnerName);
			component = component.onHoverLegacy(
					"&7Seller: &f" + this.shopOwnerName,
					"&7Seller UUID: &f" + this.shopOwnerUid,
					"&7Click to copy.");
			component = component.onClickSuggestCmd(this.shopOwnerUid.toString());

		} else
			component = component.appendMiniAmpersand("adminshop");

		component = component.appendMiniAmpersand(" ");

		component = component.appendMiniAmpersand("&f" + this.amount + "x " + ChatUtil.capitalizeFully(this.item.getType()));
		component = component.onHoverLegacy(
				"&7Click to open menu where you",
				"&7can get the item involved",
				"&7in this transaction.");

		component = component.onClickRunCmd("/" + Settings.MAIN_COMMAND_ALIASES.get(0) + " row menu " + this.getTable().getKey() + " " + this.getId());

		final String price = MathUtil.formatTwoDigits(this.price);

		component = component.appendMiniAmpersand(" &7for ");
		component = component.appendMiniAmpersand(this.getPriceWithCurrency());
		component = component.onHoverLegacy("&7Click to copy price amount.");
		component = component.onClickSuggestCmd(price + "");

		return component;
	}

	private String getPriceWithCurrency() {
		final String currency = ((int) this.price) == 1 ? HookManager.getCurrencySingular() : HookManager.getCurrencyPlural();

		return "$".equals(currency) ? currency + price : "€".equals(currency) ? price + currency : price + " " + currency;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	public static void logPlayer(final Location shopLocation, final Type type, final Player buyer, final String plugin, final double price, final String shopOwner, final UUID shopOwnerUid, final ItemStack item, final int amount) {
		new Transaction(shopLocation, buyer, plugin, type, price, shopOwner, shopOwnerUid, item, amount)
				.broadcast()
				.saveIfSenderNotBypasses(buyer);
	}

	public static void logPlayer(final Location shopLocation, final Type type, final Player buyer, final String plugin, final double price, final Player shopOwner, final ItemStack item, final int amount) {
		new Transaction(shopLocation, buyer, plugin, type, price, shopOwner, item, amount)
				.broadcast()
				.saveIfSenderNotBypasses(buyer);
	}

	public static void logServer(final Location shopLocation, final Type type, final Player buyer, final String plugin, final double price, final ItemStack item, final int amount) {
		new Transaction(shopLocation, buyer, plugin, type, price, null, item, amount)
				.broadcast()
				.saveIfSenderNotBypasses(buyer);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a transaction type
	 */
	@RequiredArgsConstructor
	public enum Type {

		BUY("buy") {
			@Override
			public String getLocalized() {
				return "Buy";
			}

			@Override
			public String getLocalizedPast() {
				return "Bought";
			}
		},

		SELL("sell") {
			@Override
			public String getLocalized() {
				return "Sell";
			}

			@Override
			public String getLocalizedPast() {
				return "Sold";
			}
		};

		/**
		 * The saveable non-obfuscated key
		 */
		@Getter
		private final String key;

		/**
		 * The localized name
		 * @return
		 */
		public abstract String getLocalized();

		/**
		 * The localized name past
		 * @return
		 */
		public abstract String getLocalizedPast();

		/**
		 * Returns {@link #getKey()}
		 */
		@Override
		public String toString() {
			return this.key;
		}

		/**
		 * Attempt to load a transaction type from the given config key
		 *
		 * @param key
		 * @return
		 */
		public static Type fromKey(final String key) {
			for (final Type mode : values())
				if (mode.key.equalsIgnoreCase(key))
					return mode;

			throw new IllegalArgumentException("No such transaction type: " + key + " Available: " + Common.join(values()));
		}

		/**
		 * Return the transaction type from the given enum
		 *
		 * @param enumValue
		 * @return
		 */
		public static Type fromEnum(final Enum<?> enumValue) {
			return fromKey(enumValue.name());
		}

		/**
		 * Return the transaction type from the given boolean
		 *
		 * @param isBuying
		 * @return
		 */
		public static Type fromBoolean(final boolean isBuying) {
			return isBuying ? BUY : SELL;
		}
	}
}
