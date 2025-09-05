package org.mineacademy.protect.task;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.model.ScanCause;
import org.mineacademy.protect.operator.Rule;

/**
 * Represents a task that scans player inventories periodically
 */
public final class PeriodScanTask extends SimpleRunnable {

	@Override
	public void run() {

		for (final Player player : Remain.getOnlinePlayers())
			try {
				Rule.filterPlayer(ScanCause.PERIOD, player);

			} catch (final Throwable t) {
				Common.warning("Error scanning player " + player.getName() + "'s inventory");

				t.printStackTrace();
			}
	}
}
