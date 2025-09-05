package org.mineacademy.protect.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.mineacademy.protect.menu.InventoryMenu;
import org.mineacademy.protect.model.InventoryType;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.model.TemporaryStorage;

/**
 * The command to view offline player inventory content.
 */
final class InvCommand extends ProtectSubCommand {

	InvCommand() {
		super("inv|i");

		this.setDescription("View player inventory content. Supports online and offline players. Auto-completes players that just left.");
		this.setUsage("<type> <player> [viewer]");
		this.setMinArguments(2);
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} regular &6<name/uuid> [viewer] &7- Open inventory.",
				"/{label} {sublabel} armor &6<name/uuid> [viewer] &7- Open armor content.",
				"/{label} {sublabel} enderchest &6<name/uuid> [viewer] &7- Open ender chest."
		};
	}

	@Override
	protected void onCommand() {
		final InventoryType type = this.findEnum(InventoryType.class, this.args[0]);
		this.checkPerm(Permissions.Command.INV.replace("{type}", type.getKey()));

		CommandSender viewer = this.getSender();

		if (this.args.length == 3)
			viewer = this.findPlayer(this.args[2]);

		final CommandSender finalViewer = viewer;

		this.findOfflinePlayer(this.args[1], targetOffline -> {
			if (type != InventoryType.ENDERCHEST)
				this.checkBoolean(!targetOffline.getName().equals(finalViewer.getName()), finalViewer.getName() + " is already viewing his own inventory.");

			final boolean readOnly = !this.hasPerm(Permissions.Command.INV_WRITE);

			InventoryMenu.show(finalViewer, type, targetOffline, readOnly);
		});
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWord(Arrays.asList(InventoryType.values()).stream()
						.filter(type -> this.hasPerm(Permissions.Command.INV.replace("{type}", type.getKey())))
						.map(InventoryType::getKey)
						.collect(Collectors.toList()));
			case 2:
				return this.completeLastWord(this.completeLastWordPlayerNames(), TemporaryStorage.getPlayerNames());
			case 3:
				return this.completeLastWordPlayerNames().stream().filter(name -> !name.toLowerCase().equals(this.args[2])).collect(Collectors.toList());
		}

		return NO_COMPLETE;
	}
}