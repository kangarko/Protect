package org.mineacademy.protect.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTItem;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.model.db.HoldsItems;
import org.mineacademy.protect.model.db.Item;
import org.mineacademy.protect.model.db.ProtectRow;
import org.mineacademy.protect.model.db.Transaction;

public final class ItemMenu<T extends ProtectRow & HoldsItems> extends Menu {

	private final T row;

	private final Button viewPlayerInventoryButton;
	private final int viewPlayerInventoryButtonSlot;

	private final Button viewContainerInventoryButton;
	private final int viewContainerInventoryButtonSlot;

	private final Button viewconfiscatedItemButton;
	private final int viewConfiscatedItemButtonSlot;

	private ItemMenu(T row) {
		this.row = row;

		this.setTitle("Item Menu");
		this.setSize(9 * 3);

		final boolean hasPlayerInventory = row instanceof Item && ((Item) row).getInventory().hasPlayerInventory();
		final boolean hasContainer = row instanceof Item && ((Item) row).getInventory().hasContainer();

		if (hasPlayerInventory) {
			this.viewPlayerInventoryButtonSlot = 9 + 2;
			this.viewConfiscatedItemButtonSlot = 9 + 6;
		} else {
			this.viewPlayerInventoryButtonSlot = -1;
			this.viewConfiscatedItemButtonSlot = 9 * 1 + 4;
		}

		this.viewContainerInventoryButtonSlot = hasContainer ? 9 + 4 : -1;

		this.viewPlayerInventoryButton = hasPlayerInventory ? new ButtonMenu(() -> new ViewPlayerInventoryMenu((Item) row), ItemCreator.from(
				CompMaterial.ENCHANTED_BOOK,
				"View Player Inventory",
				"",
				"Click to open the player",
				"inventory in the state",
				"before the scan with",
				"items that were taken.")) : Button.makeEmpty();

		this.viewContainerInventoryButton = hasContainer ? new ButtonMenu(() -> new ViewContainerInventoryMenu((Item) row), ItemCreator.from(
				CompMaterial.CHEST,
				"View Container",
				"",
				"Click to open the container",
				"which was scanned in the",
				"state before anything",
				"was taken.").glow(true)) : Button.makeEmpty();

		this.viewconfiscatedItemButton = new ButtonMenu(() -> new ViewConfiscatedItemsMenu(row), ItemCreator.from(
				CompMaterial.ENDER_CHEST,
				(row instanceof Item ? "View Confiscated Items" : row instanceof Transaction ? "View Transaction Items" : "View Unknown Items"),
				"",
				"&fAmount: &7" + row.getItems().length,
				"",
				"Click to open the items",
				"altered or taken",
				"during the scan."));
	}

	@Override
	public ItemStack getItemAt(int slot) {

		if (slot == this.viewPlayerInventoryButtonSlot)
			return this.viewPlayerInventoryButton.getItem();

		if (slot == this.viewContainerInventoryButtonSlot)
			return this.viewContainerInventoryButton.getItem();

		if (slot == this.viewConfiscatedItemButtonSlot)
			return this.viewconfiscatedItemButton.getItem();

		return NO_ITEM;
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"&7Items altered: &f" + this.row.getItems().length,
				"&7Date: &f" + TimeUtil.getFormattedDate(this.row.getDate()),
				"&7Server: &f" + this.row.getServer(),
				"&7Location: &f" + this.row.getLocationFormatted(),
				"&7Player: &f" + this.row.getPlayer(),
				"&7UUID: &f" + this.row.getPlayerUid(),
				"&7Gamemode: &f" + this.row.getGamemode()
		};
	}

	public static <T extends ProtectRow & HoldsItems> void showTo(Player player, T row) {
		new ItemMenu<>(row).displayTo(player);
	}

	private class ViewPlayerInventoryMenu extends Menu {

		private final Item item;

		@Position(start = StartPosition.BOTTOM_CENTER)
		private final Button restoreButton;

		public ViewPlayerInventoryMenu(Item item) {
			super(ItemMenu.this);

			this.item = item;

			this.setTitle("Player Inventory View");
			this.setSize(PlayerUtil.PLAYER_INV_SIZE + 9 * 2);

			this.restoreButton = Button.makeSimple(
					CompMaterial.CHEST,
					"Restore",
					"Give the inventory back\n" +
							"to the player. Override\n" +
							"his current inventory.\n" +
							"Cannot be undone.\n" +
							"\n" +
							"&cNB: We will take his items again\n" +
							"&cunless you give the player the\n" +
							"&c" + Permissions.Bypass.SCAN + " permission.",
					player -> {
						final Player target = Remain.getPlayerByUUID(item.getPlayerUid());

						if (target == null) {
							this.animateTitle("&4Player is offline");
							Common.tell(player, "&cThe player must be online to restore his inventory.");

							return;
						}

						if (target.equals(player)) {
							this.getViewer().closeInventory();

							Common.tell(target, "&cYou are restoring your own inventory, so we closed the menu.");
						} else
							this.animateTitle("&2Inventory restored!");

						this.item.getInventory().restore(target);
					});
		}

		@Override
		protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
			return slot < this.getSize() - 9 && action != InventoryAction.MOVE_TO_OTHER_INVENTORY;
		}

		@Override
		public ItemStack getItemAt(int slot) {
			if (slot < PlayerUtil.PLAYER_INV_SIZE) {
				int index = 0;

				if (slot >= 0 && slot <= 26)
					index = 9 + slot;
				else if (slot >= 27 && slot <= 35)
					index = slot - 27;
				else
					throw new IllegalArgumentException("Invalid slot: " + slot);

				return this.item.getInventory().getSurvivalItem(index);
			}

			if (slot >= 36 && slot <= 39)
				return this.item.getInventory().getArmorItem(slot - 36);

			if (slot == 9 * 4 + 5)
				return this.item.getInventory().getOffhand();

			return NO_ITEM;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"This menu shows " + this.item.getPlayer() + "'s",
					"inventory as they had it before the",
					"scan. You are allowed to drag items",
					"back to your inventory.",
					"",
					"The first 4 rows store the survival,",
					"inventory. The last row stores",
					"armor and offhand.",
					"",
					"&7Player: &f" + this.item.getPlayer(),
					"&7Date: &f" + TimeUtil.getFormattedDate(this.item.getDate()),
			};
		}
	}

	private class ViewContainerInventoryMenu extends Menu {

		private final Item item;
		private final int containerSize;

		public ViewContainerInventoryMenu(Item item) {
			super(ItemMenu.this);

			this.item = item;

			int adjustedSize = this.item.getInventory().getContainerSize();

			// if the rawSize is not a multiple of 9, we need to round it up to the nearest multiple of 9
			if (adjustedSize % 9 != 0)
				adjustedSize = adjustedSize + (9 - (adjustedSize % 9));

			if (adjustedSize > 9 * 6) {
				Common.log("Container size is too big (" + adjustedSize + "), setting to 9*6: " + item);

				adjustedSize = 9 * 6;
			}

			this.containerSize = adjustedSize;

			this.setTitle("Container Inventory");
			this.setSize(this.containerSize + 9);
		}

		@Override
		protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
			return slot < this.getSize() - 9 && action != InventoryAction.MOVE_TO_OTHER_INVENTORY;
		}

		@Override
		public ItemStack getItemAt(int slot) {
			if (slot < this.containerSize && this.item.getInventory().getContainerSize() > slot)
				return this.item.getInventory().getContainerSlot(slot);

			return NO_ITEM;
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"This menu shows the container that",
					this.item.getPlayer() + " has opened",
					"before the scan. You can drag items",
					"back to you inventory.",
					"",
					"&7Player: &f" + this.item.getPlayer(),
					"&7Date: &f" + TimeUtil.getFormattedDate(this.item.getDate()),
			};
		}
	}

	private class ViewConfiscatedItemsMenu extends MenuPaged<ItemStack> {

		private final T item;

		public ViewConfiscatedItemsMenu(T item) {
			super(ItemMenu.this, item.getItems());

			this.item = item;

			this.setTitle(item instanceof Item ? "Confiscated Items" : item instanceof Transaction ? "Transaction Items" : "Unknown Items");
		}

		@Override
		protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
			return false;
		}

		@Override
		protected ItemStack convertToItemStack(ItemStack item) {
			return ItemCreator.fromItemStack(item).lore(
					"",
					"&b&l< &7Left click to add to ",
					"your inventory. Careful:",
					"Hacked items might kick you",
					"or crash your server.",
					"",
					"&a&l> &7Right click to print NBT tag",
					"to the server console.").make();
		}

		@Override
		protected void onPageClick(Player player, ItemStack item, ClickType click) {
			if (click == ClickType.LEFT) {
				player.getInventory().addItem(item);

				animateTitle("&2Item added to inventory!");

			} else if (click == ClickType.RIGHT) {
				Common.log("Printing item information: ");
				Common.log(
						"Type: " + item.getType(),
						"Bukkit's toString(): " + item.toString(),
						"NBT: " + new NBTItem(item).toString());

				animateTitle("&2NBT tag printed to console!");
			}
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"This menu shows all items that",
					"were taken or cloned from the",
					"player either during a",
					"transaction or a rule scan.",
					"",
					"&fTotal amount of items: &7" + this.item.getItems().length,
			};
		}
	}
}
