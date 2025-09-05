package org.mineacademy.protect.menu;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.model.ArmorSlot;
import org.mineacademy.protect.model.InventoryType;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.settings.Settings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * A menu to display offline inventory content of a player
 */
@Getter(value = AccessLevel.PROTECTED)
public abstract class InventoryMenu extends Menu {

	/**
	 * The player whose inventory we are viewing
	 */
	private final OfflinePlayer target;

	/**
	 * What kind of inventory we are viewing
	 */
	private final InventoryType inventoryType;

	/**
	 * Is this menu read only?
	 */
	private final boolean readOnly;

	/**
	 * The content, stored for performance reasons
	 */
	private ItemStack[] content;

	/**
	 * The armor content stored to prevent armor loss
	 */
	private final Map<ArmorSlot, ItemStack> armorContent = new HashMap<>();

	/*
	 * Open a new menu of the given type
	 */
	protected InventoryMenu(final OfflinePlayer target, final InventoryType inventoryType, final boolean readOnly) {
		this.target = target;
		this.inventoryType = inventoryType;
		this.readOnly = readOnly;

		setSize(inventoryType.getInventorySize());
		setTitle(target.getName() + "'s " + inventoryType.getMessageString());
	}

	/**
	 * Set the content of the inventory
	 *
	 * @param content
	 */
	public final void setContent(final ItemStack[] content) {
		this.content = content;
	}

	/**
	 * Add armor content to the menu
	 *
	 * @param slot
	 * @param item
	 */
	public final void addArmorContent(final ArmorSlot slot, final ItemStack item) {
		this.armorContent.put(slot, item);
	}

	/**
	 * Return the item at the given slot
	 */
	@Override
	public final ItemStack getItemAt(final int slot) {
		Valid.checkNotNull(this.content, "Call setContent() before calling getItemAt()");

		if (this.getInventoryType() == InventoryType.ARMOR) {

			for (final ArmorSlot armorSlot : ArmorSlot.values())
				if (armorSlot.isAvailable() && slot == armorSlot.getMenuSlot())
					return this.armorContent.get(armorSlot);

			if (slot == 9 + 1)
				return ItemCreator.from(CompMaterial.LEATHER_HELMET,
						"&6Helmet",
						"",
						"Slot below displays",
						"a helmet.")
						.make();

			if (slot == 9 + 2)
				return ItemCreator.from(CompMaterial.LEATHER_CHESTPLATE,
						"&6Chestplate",
						"",
						"Slot below displays",
						"a chestplate.")
						.make();

			if (slot == 9 + 3)
				return ItemCreator.from(CompMaterial.LEATHER_LEGGINGS,
						"&6Leggings",
						"",
						"Slot below displays",
						"leggings.")
						.make();

			if (slot == 9 + 4)
				return ItemCreator.from(CompMaterial.LEATHER_BOOTS,
						"&6Boots",
						"",
						"Slot below displays",
						"boots.")
						.make();

			if (ArmorSlot.HAS_OFF_HAND)
				if (slot == 9 + 7)
					return ItemCreator.from(CompMaterial.SHIELD,
							"&6Off-hand",
							"",
							"Slot below displays",
							"an off-hand item.")
							.make();

			return ItemCreator.fromMaterial(CompMaterial.ORANGE_STAINED_GLASS_PANE).name("&c").make();
		}

		return this.content[slot];
	}

	/**
	 * Return if the given slot is a valid slot
	 */
	@Override
	protected final boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, final InventoryAction action) {
		if (location != MenuClickLocation.MENU)
			return true;

		if (this.readOnly)
			return false;

		if (this.inventoryType == InventoryType.ARMOR) {
			for (final ArmorSlot armorSlot : ArmorSlot.values())
				if (armorSlot.isAvailable() && slot == armorSlot.getMenuSlot())
					return true;

			return false;
		}

		return !this.readOnly;
	}

	@Override
	protected void onMenuClose(final Player player, final Inventory inventory) {
		throw new UnsupportedOperationException("Implement onMenuClose() in your subclass");
	}

	/**
	 * Set the armor content to the target player
	 *
	 * @param target
	 * @param inventory
	 */
	protected final void giveArmorFromInventory(final Player target, final Inventory inventory) {
		final PlayerInventory targetInventory = target.getInventory();

		targetInventory.setHelmet(inventory.getItem(9 * 2 + 1));
		targetInventory.setChestplate(inventory.getItem(9 * 2 + 2));
		targetInventory.setLeggings(inventory.getItem(9 * 2 + 3));
		targetInventory.setBoots(inventory.getItem(9 * 2 + 4));

		if (ArmorSlot.HAS_OFF_HAND)
			targetInventory.setItemInOffHand(inventory.getItem(9 * 2 + 7));
	}

	/**
	 * Return if this menu is the given player's
	 *
	 * @param player
	 * @return
	 */
	public final boolean isOwnedBy(final Player player) {
		return this.target.getUniqueId().equals(player.getUniqueId());
	}

	/**
	 * Return if this menu is the given player's
	 *
	 * @param player
	 * @return
	 */
	public final boolean isOwnedBy(final OfflinePlayer player) {
		return this.target.getUniqueId().equals(player.getUniqueId());
	}

	/**
	 * Return the inventory type
	 *
	 * @return
	 */
	public final InventoryType getInventoryType() {
		return this.inventoryType;
	}

	/*
	 * Display this menu to the given player. Console and legacy versions will print the content as string instead
	 */
	private final void handleDisplay(final CommandSender viewer) {
		final boolean viewerIsPlayer = viewer instanceof Player;
		final Player viewerPlayer = viewerIsPlayer ? (Player) viewer : null;

		final boolean targetIsOnline = this.target.isOnline();

		// NBT offline reading is not supported in legacy versions
		if ((!targetIsOnline && MinecraftVersion.olderThan(V.v1_7)) || !viewerIsPlayer) {
			Common.tell(viewer, "Content of " + this.target.getName() + "'s " + this.inventoryType.getMessageString() + ":");

			if (this.inventoryType == InventoryType.REGULAR || this.inventoryType == InventoryType.ENDERCHEST)
				for (int i = 0; i < this.inventoryType.getInventorySize(); i++)
					Common.tell(viewer, "&8[" + i + "] &7" + this.content[i]);
			else
				for (final ArmorSlot armorSlot : ArmorSlot.values())
					if (armorSlot.isAvailable())
						Common.tell(viewer, "&8" + armorSlot.toString() + ": &7" + this.armorContent.get(armorSlot));

		} else {

			// Prevent opening the same inventory twice
			final SimpleComponent component = SimpleComponent.fromMiniAmpersand("&c{other_player} is already viewing " + this.getTarget().getName() + " 's offline inventory.");
			final String command = "/" + Settings.MAIN_COMMAND_ALIASES.get(0) + " invclose {other_player}";

			if (viewer.hasPermission(Permissions.Command.INV_CLOSE)) {
				component.appendMiniAmpersand(" Type ");
				component.appendMiniAmpersand("'&n" + command + "&c'").onHoverLegacy("Click to run this command.").onClickSuggestCmd(command);
				component.appendMiniAmpersand(" to close their inventory.");
			}

			for (final Player online : Remain.getOnlinePlayers()) {
				final Menu otherMenu = Menu.getMenu(online);

				if (!targetIsOnline && otherMenu instanceof InventoryMenu && ((InventoryMenu) otherMenu).isOwnedBy(this.target)) {
					component.replaceBracket("other_player", online.getName());
					Common.tell(online, component);

					return;
				}

				if (targetIsOnline && this.inventoryType == InventoryType.ARMOR && otherMenu instanceof OnlineInventoryMenu) {
					final OnlineInventoryMenu otherInventoryMenu = (OnlineInventoryMenu) otherMenu;

					if (otherInventoryMenu.isOwnedBy(this.getTarget()) && otherInventoryMenu.getInventoryType() == InventoryType.ARMOR) {
						component.replaceBracket("other_player", online.getName());
						Common.tell(online, component);

						return;
					}
				}
			}

			this.displayTo((Player) viewer);

			CompSound.ENTITY_FIREWORK_ROCKET_BLAST.play(viewerPlayer, 1, 1);
			Messenger.success(viewerPlayer, "Now " + (readOnly ? "reading" : "editing") + " " + this.inventoryType.getMessageString() + " of " + this.target.getName() + ".");
		}
	}

	/**
	 * Override the method to display the menu
	 */
	@Override
	protected void onDisplay(final InventoryDrawer drawer, final Player player) {

		// Bukkit handles live inventory viewing with its own sync so we leave it to them
		if (this.target.isOnline() && !this.readOnly && (this.inventoryType == InventoryType.REGULAR || this.inventoryType == InventoryType.ENDERCHEST)) {
			final Player targetPlayer = this.target.getPlayer();
			final Inventory liveInventory = this.inventoryType == InventoryType.REGULAR ? targetPlayer.getInventory() : this.inventoryType == InventoryType.ENDERCHEST ? targetPlayer.getEnderChest() : null;

			player.openInventory(liveInventory);

		} else
			super.onDisplay(drawer, player);
	}

	/**
	 * Show player inventory
	 *
	 * @param viewer
	 * @param type
	 * @param target
	 * @param readOnly
	 */
	public static void show(@NonNull final CommandSender viewer, @NonNull final InventoryType type, @NonNull final OfflinePlayer target, final boolean readOnly) {
		final InventoryMenu menu = target.isOnline() ? new OnlineInventoryMenu(target.getPlayer(), type, readOnly) : new OfflineInventoryMenu(target, type, readOnly);

		menu.handleDisplay(viewer);
	}
}