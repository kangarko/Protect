package org.mineacademy.protect.command;

import java.util.List;

import org.bukkit.entity.Player;

/**
 * The command to view offline player inventory content.
 */
final class InvCloseCommand extends ProtectSubCommand {

	InvCloseCommand() {
		super("invclose|ic");

		this.setDescription("Close a player's open inventory.");
		this.setUsage("<player>");
		this.setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		final Player target = this.findPlayer(this.args[0]);

		target.closeInventory();
		this.tellSuccess("You have closed " + target.getName() + "'s inventory.");
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWordPlayerNames();
		}

		return NO_COMPLETE;
	}
}