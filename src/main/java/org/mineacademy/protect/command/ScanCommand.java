package org.mineacademy.protect.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.protect.model.ScanCause;
import org.mineacademy.protect.operator.Rule;

/**
 * The command to scan an online player inventory.
 */
final class ScanCommand extends ProtectSubCommand {

	ScanCommand() {
		super("scan|s");

		this.setValidArguments(0, 1);
		this.setDescription("Manually scan a player's inventory.");
		this.setUsage("[player]");
	}

	@Override
	protected void onCommand() {
		final Player target = this.findPlayerOrSelf(0);

		LagCatcher.start("protect");
		Rule.filterPlayer(ScanCause.MANUAL, target);
		LagCatcher.end("protect", 0);

		tellInfo("Scanned inventory of " + target.getName() + "." + (!this.isPlayer() ? " See console for details." : ""));
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