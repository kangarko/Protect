package org.mineacademy.protect.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBT;
import org.mineacademy.fo.remain.nbt.NBTItem;
import org.mineacademy.fo.remain.nbt.NbtApiException;

/**
 * The command to scan an online player inventory.
 */
final class InspectCommand extends ProtectSubCommand {

	InspectCommand() {
		super("inspect");

		this.setValidArguments(1, 2);
		this.setDescription("Print items in a container you are looking at and remove crashed items without opening the container.");
		this.setUsage("<print/remove> [slot]");
	}

	@Override
	protected void onCommand() {
		this.checkConsole();

		final String param = this.args[0].toLowerCase();

		final Block block = Remain.getTargetBlock(this.getPlayer(), 6);
		this.checkBoolean(!CompMaterial.isAir(block), "You must be looking at a block!");

		final BlockState state = block.getState();

		if (!"print".equals(param) && !"remove".equals(param))
			returnInvalidArgs(param);

		if (!(state instanceof InventoryHolder)) {
			try {
				NBT.get(state, nbt -> {
					Common.log("NBT data for " + state.getClass().getSimpleName() + " at " + SerializeUtil.serializeLocation(block.getLocation()) + ": " + nbt);
				});

				tellInfo("You were looking at a block which is a not a container but has a block state. Printing it to the console...");
				return;

			} catch (final NbtApiException ex) {
				returnTell("You must be looking at a container block!");
			}
		}

		final InventoryHolder holder = (InventoryHolder) state;
		this.checkBoolean(holder.getInventory() != null, "This container does not have an inventory!");

		final ItemStack[] contents = holder.getInventory().getContents();

		if ("print".equals(param)) {
			final String fileName = "dumps/" + TimeUtil.getFormattedDate().replace(":", ".") + ".txt";

			this.tellNoPrefix(
					"&8" + Common.chatLineSmooth(),
					"&6&lPrinting " + contents.length + " items:",
					"&7See " + fileName + " for NBT details.",
					"&8" + Common.chatLineSmooth());

			FileUtil.write(fileName,
					"#",
					"# Dumping container at " + SerializeUtil.serializeLocation(block.getLocation()) + " with " + contents.length + " items:",
					"#");

			for (int i = 0; i < contents.length; i++) {
				final ItemStack item = contents[i];

				if (item != null)
					this.tellNoPrefix("&8[&6" + i + "&8] &7" + item.toString());

				FileUtil.write(fileName, "[" + i + "] " + (item == null ? null : item + " / NBT: " + new NBTItem(item)));
			}

		} else if ("remove".equals(param)) {
			this.checkArgs(2, "You must specify the slot to remove!");

			final int slot = this.findInt(1, "Slot must be a number!");
			this.checkBoolean(slot >= 0 && slot < contents.length, "Slot must be between 0 and " + (holder.getInventory().getSize() - 1) + "!");

			holder.getInventory().clear(slot);
			tellSuccess("Slot " + slot + " has been cleared.");

		} else
			this.returnInvalidArgs(param);
	}

	@Override
	public List<String> tabComplete() {
		if (!this.isPlayer())
			return NO_COMPLETE;

		switch (this.args.length) {
			case 1:
				return completeLastWord("print", "remove");

			case 2: {
				if ("remove".equals(this.args[0])) {
					final Block block = Remain.getTargetBlock(this.getPlayer(), 6);

					if (!CompMaterial.isAir(block)) {
						final BlockState state = block.getState();

						if (state instanceof InventoryHolder) {
							final InventoryHolder holder = (InventoryHolder) state;
							final ItemStack[] contents = holder.getInventory().getContents();

							final List<Integer> nonEmptySlots = new ArrayList<>();

							for (int slot = 0; slot < contents.length; slot++)
								if (contents[slot] != null)
									nonEmptySlots.add(slot);

							return this.completeLastWord(nonEmptySlots);
						}
					}
				}
			}
		}

		return NO_COMPLETE;
	}
}