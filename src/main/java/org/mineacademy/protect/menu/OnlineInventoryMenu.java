package org.mineacademy.protect.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.mineacademy.protect.model.ArmorSlot;
import org.mineacademy.protect.model.InventoryType;

public final class OnlineInventoryMenu extends InventoryMenu {

	protected OnlineInventoryMenu(final Player target, final InventoryType inventoryType, final boolean readOnly) {
		super(target, inventoryType, readOnly);

		final PlayerInventory inventory = target.getInventory();

		if (inventoryType == InventoryType.REGULAR || inventoryType == InventoryType.ARMOR) {
			this.setContent(inventory.getContents());

			if (inventoryType == InventoryType.ARMOR) {
				final ItemStack[] armorContent = inventory.getArmorContents();

				for (final ArmorSlot armorSlot : ArmorSlot.values())
					if (armorSlot == ArmorSlot.OFF_HAND) {
						if (ArmorSlot.HAS_OFF_HAND)
							this.addArmorContent(armorSlot, inventory.getItemInOffHand());

					} else
						this.addArmorContent(armorSlot, armorContent[armorSlot.getInvSlot()]);
			}

		} else
			this.setContent(target.getEnderChest().getContents());
	}

	@Override
	protected void onMenuClose(final Player player, final Inventory inventory) {
		if (this.isReadOnly())
			return;

		final Player target = this.getTarget();

		if (target.isOnline() && this.getInventoryType() == InventoryType.ARMOR)
			this.giveArmorFromInventory(target, inventory);
	}

	@Override
	protected Player getTarget() {
		return (Player) super.getTarget();
	}
}
