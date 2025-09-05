package org.mineacademy.protect.menu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.remain.nbt.NBTCompound;
import org.mineacademy.fo.remain.nbt.NBTCompoundList;
import org.mineacademy.fo.remain.nbt.NBTContainer;
import org.mineacademy.fo.remain.nbt.NBTFile;
import org.mineacademy.fo.remain.nbt.NBTItem;
import org.mineacademy.fo.remain.nbt.ReadWriteNBT;
import org.mineacademy.protect.model.ArmorSlot;
import org.mineacademy.protect.model.InventoryType;

/**
 * A menu to display offline inventory content of a player
 */
public final class OfflineInventoryMenu extends InventoryMenu {

	/**
	 * The player nbt file opened for reading
	 */
	private final NBTFile nbtFile;

	/**
	 * The inventory section of player file
	 */
	private final NBTCompoundList nbtInventory;

	/*
	 * Open a new menu of the given type
	 */
	protected OfflineInventoryMenu(final OfflinePlayer target, final InventoryType inventoryType, final boolean readOnly) {
		super(target, inventoryType, readOnly);

		this.nbtFile = this.findNbtFile();
		this.nbtInventory = this.nbtFile.getCompoundList(this.getInventoryType() == InventoryType.ENDERCHEST ? "EnderItems" : "Inventory");

		this.readData();
	}

	/*
	 * Find the first suitable player file
	 */
	private NBTFile findNbtFile() {
		final File rootFolder = Bukkit.getWorldContainer();

		for (final File serverRootFile : rootFolder.listFiles()) {
			if (serverRootFile.isDirectory()) {
				final File maybePlayerFile = new File(serverRootFile, "playerdata/" + this.getTarget().getUniqueId() + ".dat");

				if (maybePlayerFile.exists()) {
					try {
						return new NBTFile(maybePlayerFile);

					} catch (final IOException ex) {
						Common.throwError(ex, "Failed to open offline " + this.getInventoryType().getKey() + " inventory for " + this.getTarget().getName());
					}

					break;
				}
			}
		}

		throw new IllegalArgumentException("Could not find offline " + this.getInventoryType().getKey() + " inventory for " + this.getTarget().getName());
	}

	/*
	 * Read the data from the player file
	 */
	private void readData() {
		final ItemStack[] regularContent = new ItemStack[PlayerUtil.PLAYER_INV_SIZE];

		if (this.getInventoryType() == InventoryType.REGULAR || this.getInventoryType() == InventoryType.ARMOR) {
			for (final ReadWriteNBT item : this.nbtInventory) {
				final int slot = item.getByte("Slot");

				if (slot >= 0 && slot <= PlayerUtil.PLAYER_INV_SIZE)
					regularContent[slot] = NBTItem.convertNBTtoItem((NBTCompound) item);

				for (final ArmorSlot armorSlot : ArmorSlot.values())
					if (armorSlot.getNmsSlot() == slot)
						this.addArmorContent(armorSlot, NBTItem.convertNBTtoItem((NBTCompound) item));
			}
		}

		else
			for (final ReadWriteNBT item : this.nbtInventory) {
				final int slot = item.getByte("Slot");

				regularContent[slot] = NBTItem.convertNBTtoItem((NBTCompound) item);
			}

		this.setContent(regularContent);
	}

	@Override
	protected void onMenuClose(final Player player, final Inventory inventory) {
		if (!this.isReadOnly()) {
			final ItemStack[] content = inventory.getContents();
			final InventoryType type = this.getInventoryType();

			// Joined in the meantime
			if (this.getTarget().isOnline()) {

				final Player targetPlayer = this.getTarget().getPlayer();
				final Inventory targetInventory = type == InventoryType.REGULAR ? targetPlayer.getInventory() : targetPlayer.getEnderChest();

				if (type == InventoryType.ARMOR)
					this.giveArmorFromInventory(targetPlayer, inventory);

				else
					for (int i = 0; i < this.getInventoryType().getInventorySize(); i++)
						targetInventory.setItem(i, inventory.getItem(i));

				return;
			}

			if (type == InventoryType.REGULAR || type == InventoryType.ENDERCHEST) {
				this.nbtInventory.clear();

				for (int slot = 0; slot < content.length; slot++) {
					final ItemStack item = content[slot];

					if (item != null) {
						final NBTContainer container = NBTItem.convertItemtoNBT(item);
						container.setByte("Slot", (byte) (slot));

						this.nbtInventory.addCompound(container);
					}
				}
			}

			if (type == InventoryType.REGULAR) {

				// Save armor content from saved armor slots on reading
				for (final Map.Entry<ArmorSlot, ItemStack> entry : this.getArmorContent().entrySet()) {
					final ItemStack item = entry.getValue();
					final ArmorSlot armorSlot = entry.getKey();

					if (item != null) {
						final NBTContainer container = NBTItem.convertItemtoNBT(item);
						container.setByte("Slot", (byte) (armorSlot.getNmsSlot()));

						this.nbtInventory.addCompound(container);
					}
				}

			} else if (type == InventoryType.ARMOR) {
				final List<ReadWriteNBT> regularInventoryBackup = new ArrayList<>();

				// Clear armor slots but keep the regular inventory
				for (final Iterator<ReadWriteNBT> it = this.nbtInventory.iterator(); it.hasNext();) {
					final ReadWriteNBT item = it.next();
					final int slot = item.getByte("Slot");

					if (ArmorSlot.fromNms(slot) == null)
						regularInventoryBackup.add(item);
				}

				this.nbtInventory.clear();
				this.nbtInventory.addAll(regularInventoryBackup);

				for (final ArmorSlot slot : ArmorSlot.values()) {
					final ItemStack item = inventory.getItem(slot.getMenuSlot());

					if (item != null) {
						final NBTContainer container = NBTItem.convertItemtoNBT(item);
						container.setByte("Slot", (byte) (slot.getNmsSlot()));

						this.nbtInventory.addCompound(container);
					}
				}
			}

			try {
				this.nbtFile.save();

			} catch (final IOException ex) {
				Common.throwError(ex, "Failed to save offline " + this.getInventoryType().getKey() + " inventory for " + this.getTarget().getName());
			}
		}
	}
}