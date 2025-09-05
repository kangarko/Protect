package org.mineacademy.protect.model.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.database.SimpleResultSet;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.protect.model.ArmorSlot;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.model.ScanCause;
import org.mineacademy.protect.operator.Rule;
import org.mineacademy.protect.settings.Settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Represents a spied command.
 */
@Getter
@ToString
public final class Item extends ProtectRow implements HoldsItems {

	private final ItemStack[] items;
	private final String match;
	private final String ruleName;
	private final InventorySnapshot inventory;

	private Item(final String cause, final Location location, final ItemStack[] items, final String match, final String ruleName) {
		super(location, cause, null, null);

		Valid.checkBoolean(items.length > 0, "No items to log");

		this.items = items;
		this.match = match;
		this.ruleName = ruleName;
		this.inventory = InventorySnapshot.fromEmpty();
	}

	private Item(final Player player, final InventorySnapshot snapshot, final ItemStack[] items, final String match, final String ruleName) {
		super(player.getLocation(), player.getName(), player.getUniqueId(), player.getGameMode());

		Valid.checkBoolean(items.length > 0, "No items to log");

		this.items = items;
		this.match = match;
		this.ruleName = ruleName;
		this.inventory = snapshot;
	}

	Item(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);

		this.items = SerializeUtil.deserializeItemArrayStrict(resultSet, "Items");
		this.match = resultSet.getStringStrict("RuleMatch");
		this.ruleName = resultSet.getStringStrict("RuleName");
		this.inventory = InventorySnapshot.fromDatabaseItems(SerializeUtil.deserializeItemArray(resultSet, "Inventory"));

		Valid.checkBoolean(this.items.length > 0, "No items to log");
	}

	@Override
	public ProtectTable getTable() {
		return ProtectTable.ITEMS;
	}

	@Override
	public String getBypassPermission() {
		return null;
	}

	@Override
	public String getNotifyPermission() {
		return Permissions.Notify.ITEM;
	}

	@Override
	public boolean isBroadcastEnabled() {
		return Settings.Rules.BROADCAST;
	}

	@Override
	public String getBroadcastFormat() {
		return Settings.Rules.BROADCAST_FORMAT;
	}

	@Override
	public String getDiscordChannel() {
		return Settings.Rules.DISCORD_CHANNEL;
	}

	@Override
	public String getDiscordFormat() {
		return Settings.Rules.DISCORD_FORMAT;
	}

	@Override
	public SerializedMap toMap() {
		return super.toMap().putArray(
				"Items", this.items,
				"RuleMatch", this.match,
				"RuleName", this.ruleName,
				"Inventory", this.inventory != null ? this.inventory.getTotalInventoryAlltogether() : "NULL");
	}

	@Override
	protected String replaceVariables(String format) {
		return Variables.builder().placeholderArray(
				"item_amount", this.items.length > 0 ? this.items[0].getAmount() : 0,
				"item_type", this.items.length > 0 ? ChatUtil.capitalizeFully(this.items[0].getType()) : "unknown",
				"rule_match", this.match,
				"rule_name", this.ruleName).replaceLegacy(super.replaceVariables(format));
	}

	@Override
	protected SimpleComponent onChatLineCreate(SimpleComponent component) {
		component = component.appendMiniAmpersand("&7: &f");

		{ // Items
			if (this.items.length == 1) {
				final int amount = this.items[0].getAmount();
				final CompMaterial compMaterial = CompMaterial.fromItem(this.items[0]);

				component = component.appendMiniAmpersand((amount > 1 ? amount + "x " : "") + Common.limit(ChatUtil.capitalizeFully(compMaterial), 20));
				component = component.onHoverLegacy(
						"&7Item: &f" + ChatUtil.capitalizeFully(compMaterial),
						"",
						"Click to open menu where you",
						"can restore or get the item.");

			} else {
				final Map<CompMaterial, Integer> counts = new HashMap<>();

				for (final ItemStack item : this.items) {
					final CompMaterial material = CompMaterial.fromMaterial(item.getType());

					counts.put(material, counts.getOrDefault(material, 0) + item.getAmount());
				}

				// find if all materials are the  same, if yes, print "x items of y"
				boolean same = true;
				CompMaterial first = null;

				for (final CompMaterial material : counts.keySet()) {
					if (first == null)
						first = material;
					else if (first != material) {
						same = false;

						break;
					}
				}

				if (same) {
					int totalAmount = 0;

					for (final int amount : counts.values())
						totalAmount += amount;

					component = component.appendMiniAmpersand(totalAmount + "x " + ChatUtil.capitalizeFully(first));

				} else
					component = component.appendMiniAmpersand(this.items.length + " items");

				// add counts to hover and limit to 10 lines
				final List<String> hover = new ArrayList<>();

				for (final CompMaterial material : counts.keySet()) {
					hover.add("&6" + counts.get(material) + "x " + Common.limit(ChatUtil.capitalizeFully(material), 20));

					if (hover.size() > 1) {
						hover.add("- and " + (counts.size() - 1) + " more items...");

						break;
					}
				}

				hover.add("");
				hover.add("Click to open menu where you");
				hover.add("can restore or get the item.");

				component = component.onHoverLegacy(Common.toArray(hover));
			}

			component = component.onClickRunCmd("/" + Settings.MAIN_COMMAND_ALIASES.get(0) + " row menu " + this.getTable().getKey() + " " + this.getId());
		}

		component = component.appendMiniAmpersand(" &7(");

		{ // Rule name
			final int currentLength = component.toPlain().length();
			final int newLength = currentLength + this.ruleName.length();

			final int substringLength = Math.max(1, 59 - currentLength - 3);
			component = component.appendMiniAmpersand(newLength > 59 ? this.ruleName.substring(0, substringLength) + "..." : this.ruleName);

			component = component.onHoverLegacy(
					"&7Rule: &f" + Common.getOrDefault(this.ruleName, "no name"),
					"&7Match: &f" + this.match,
					"",
					"Click to copy rule name.");

			component = component.onClickSuggestCmd(this.ruleName);
		}

		component = component.appendMiniAmpersand("&7)");

		return component;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Log item confiscation from the given player
	 *
	 * @param player
	 * @param snapshot
	 * @param items
	 * @param rule
	 */
	public static void log(@NonNull final Player player, @NonNull InventorySnapshot snapshot, @NonNull final ItemStack[] items, @NonNull final Rule rule) {
		new Item(player, snapshot, items, rule.getMatch(), rule.getName())
				.broadcast()
				.saveIfSenderNotBypasses(player);
	}

	/**
	 * Log item confiscation from the given player
	 *
	 * @param cause
	 * @param location
	 * @param items
	 * @param rule
	 */
	public static void log(@NonNull final ScanCause cause, @NonNull final Location location, @NonNull final ItemStack[] items, @NonNull final Rule rule) {
		new Item(cause.getKey(), location, items, rule.getMatch(), rule.getName())
				.broadcast()
				.saveIfSenderNotBypasses(null);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	@AllArgsConstructor
	public static final class InventorySnapshot {

		/**
		 * The size of the combined inventory, including the offhand slot
		 */
		public static final int TOTAL_PLAYER_SLOTS = PlayerUtil.PLAYER_INV_SIZE + 4 + 1;

		/**
		 * The player survival inventory snapshot
		 */
		private final ItemStack[] survival;

		/**
		 * The player armor snapshot
		 */
		private final ItemStack[] armor;

		/**
		 * The player offhand snapshot, null on legacy servers
		 */
		@Nullable
		private final ItemStack offhand;

		/**
		 * The extra inventory, if any
		 */
		@Nullable
		private final ItemStack[] container;

		/**
		 * Get the combined inventory as a single array used in the database
		 *
		 * @return
		 */
		public ItemStack[] getTotalInventoryAlltogether() {
			final List<ItemStack> combinedInventory = new ArrayList<>();

			if (this.survival != null) {
				Valid.checkBoolean(this.survival.length == PlayerUtil.PLAYER_INV_SIZE, "Expected " + PlayerUtil.PLAYER_INV_SIZE + " survival slots, got " + this.survival.length);

				for (final ItemStack item : this.survival)
					combinedInventory.add(item);
			}

			if (this.armor != null) {
				Valid.checkBoolean(this.armor.length == 4, "Expected 4 armor slots, got " + this.armor.length);

				for (final ItemStack armor : this.armor)
					combinedInventory.add(armor);
			}

			if (ArmorSlot.HAS_OFF_HAND && this.offhand != null)
				combinedInventory.add(this.offhand);

			if (this.container != null)
				for (final ItemStack item : this.container)
					combinedInventory.add(item);

			return combinedInventory.toArray(new ItemStack[combinedInventory.size()]);
		}

		/**
		 * Get the item in the given survival slot
		 *
		 * @param slot
		 * @return the item or null if not found
		 */
		public ItemStack getSurvivalItem(int slot) {
			Valid.checkBoolean(this.hasPlayerInventory(), "This inventory snapshot does not have a player inventory");
			Valid.checkBoolean(slot < PlayerUtil.PLAYER_INV_SIZE, "Slot " + slot + " is not in the survival inventory which is only " + PlayerUtil.PLAYER_INV_SIZE + " slots long");

			return this.survival[slot];
		}

		/**
		 * Get the item in the given armor slot
		 *
		 * @param slot
		 * @return the item or null if not found
		 */
		public ItemStack getArmorItem(int slot) {
			Valid.checkBoolean(slot < 4, "Slot " + slot + " is not in the armor inventory which is only 4 slots long");

			// reverse, slot 0 is actually end of armor array
			return this.armor[3 - slot];
		}

		/**
		 * Get the item in the offhand slot
		 *
		 * @return the item or null if not found
		 */
		public ItemStack getOffhand() {
			return offhand;
		}

		/**
		 * Get the item in the given container slot
		 *
		 * @param slot
		 * @return the item or null if not found
		 */
		public ItemStack getContainerSlot(int slot) {
			Valid.checkNotNull(this.container, "This inventory snapshot does not have a container inventory");
			Valid.checkBoolean(slot < this.container.length, "Slot " + slot + " is not in the container inventory which is only " + this.container.length + " slots long");

			return this.container[slot];
		}

		/**
		 *
		 * @return
		 */
		public boolean hasPlayerInventory() {
			return this.survival != null;
		}

		/**
		 * Return true if the container inventory is present
		 *
		 * @return
		 */
		public boolean hasContainer() {
			return this.container != null;
		}

		/**
		 * Get the size of the container inventory
		 *
		 * @return
		 */
		public int getContainerSize() {
			return this.container.length;
		}

		/**
		 * Get the size of the inventory
		 *
		 * @return
		 */
		public int getTotalSize() {
			return (this.hasPlayerInventory() ? this.survival.length : 0) + this.armor.length + (ArmorSlot.HAS_OFF_HAND ? 1 : 0) + (this.container != null ? this.container.length : 0);
		}

		/**
		 * Restore the inventory to the given player, including armor and offhand
		 * His old inventory will be overridden
		 *
		 * @param player
		 */
		public void restore(Player player) {
			Valid.checkBoolean(this.hasPlayerInventory(), "Cannot restore non-player inventory to " + player.getName());

			player.getInventory().setContents(this.survival);
			player.getInventory().setArmorContents(this.armor);

			if (ArmorSlot.HAS_OFF_HAND)
				player.getInventory().setItemInOffHand(this.offhand);
		}

		/*
		 * Prevent calling toString on this class
		 */
		@Override
		public String toString() {
			return "InventorySnapshot{}";
		}

		/**
		 * Create a new inventory snapshot from the given combined inventory
		 *
		 * @param inventory
		 * @return
		 */
		public static InventorySnapshot fromDatabaseItems(ItemStack[] inventory) {
			return inventory != null && inventory.length > 0 ? InventorySnapshot.fromCombined(inventory) : InventorySnapshot.fromEmpty();
		}

		/**
		 * Create a new empty inventory snapshot
		 *
		 * @return
		 */
		public static InventorySnapshot fromEmpty() {
			return new InventorySnapshot(null, new ItemStack[4], null, null);
		}

		/**
		 * Create a new inventory snapshot from the given combined inventory
		 *
		 * @param combinedInventory
		 * @return
		 */
		public static InventorySnapshot fromCombinedClone(ItemStack[] combinedInventory) {
			return fromCombined0(combinedInventory, true);
		}

		/**
		 * Create a new inventory snapshot from the given combined inventory
		 *
		 * @param combinedInventory
		 * @return
		 */
		public static InventorySnapshot fromCombined(ItemStack[] combinedInventory) {
			return fromCombined0(combinedInventory, false);
		}

		private static InventorySnapshot fromCombined0(ItemStack[] combinedInventory, boolean clone) {

			ItemStack[] survivalContent = new ItemStack[PlayerUtil.PLAYER_INV_SIZE];
			ItemStack[] armorContent = new ItemStack[4];
			ItemStack offHand = null;

			boolean limitReached = false;

			if (combinedInventory.length >= PlayerUtil.PLAYER_INV_SIZE) {
				for (int slot = 0; slot < survivalContent.length; slot++) {
					final ItemStack item = combinedInventory[slot];

					survivalContent[slot] = !clone ? item : (item == null ? null : item.clone());
				}

			} else {
				survivalContent = null;

				limitReached = true;
			}

			if (combinedInventory.length >= PlayerUtil.PLAYER_INV_SIZE + 4) {
				for (int slot = 0; slot < armorContent.length; slot++) {
					final ItemStack item = combinedInventory[PlayerUtil.PLAYER_INV_SIZE + slot];

					armorContent[slot] = !clone ? item : (item == null ? null : item.clone());
				}
			} else {
				armorContent = null;

				limitReached = true;
			}

			if (!limitReached && ArmorSlot.HAS_OFF_HAND) {
				if (combinedInventory.length < 41)
					limitReached = true;

				else {
					final ItemStack item = combinedInventory[40];

					offHand = !clone ? item : (item == null ? null : item.clone());
				}
			}

			final ItemStack[] containerContent = combinedInventory.length > TOTAL_PLAYER_SLOTS ? new ItemStack[combinedInventory.length - TOTAL_PLAYER_SLOTS] : null;

			if (containerContent != null)
				for (int slot = 0; slot < containerContent.length; slot++) {
					final ItemStack item = combinedInventory[TOTAL_PLAYER_SLOTS + slot];

					containerContent[slot] = !clone ? item : (item == null ? null : item.clone());
				}

			return new InventorySnapshot(survivalContent, armorContent, offHand, containerContent);
		}
	}
}
