package org.mineacademy.protect.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.model.TemporaryStorage;

/**
 * The command to view a player's playtime.
 */
final class PlaytimeCommand extends ProtectSubCommand {

	PlaytimeCommand() {
		super("playtime|pt");

		this.setDescription("See how long a player stayed on the server.");
		this.setUsage("[player/top]");
	}

	@Override
	protected void onCommand() {
		final boolean hasParam = this.args.length > 0;
		final String param = hasParam ? this.args[0] : "";

		if ("top".equals(param)) {
			this.tellInfo("Calculating playtimes, this may take a while...");

			this.runTaskAsync(() -> {
				final Map<Long, String> playtimes = new TreeMap<>();

				for (final OfflinePlayer diskPlayer : Bukkit.getOfflinePlayers()) {
					final long valueInSeconds = PlayerUtil.getStatistic(diskPlayer, Remain.getPlayTimeStatisticName()) / 20;

					playtimes.put(valueInSeconds, diskPlayer.getName());
				}

				final List<SimpleComponent> lines = new ArrayList<>();

				for (final Map.Entry<Long, String> entry : playtimes.entrySet())
					lines.add(SimpleComponent.fromMiniAmpersand("<center>&f" + entry.getValue() + " &6- &7" + (TimeUtil.formatTimeDays(entry.getKey()))));

				Collections.reverse(lines);

				new ChatPaginator()
						.setFoundationHeader("Top Playtimes")
						.setPages(lines)
						.send(this.audience);
			});

		} else {
			this.checkBoolean(hasParam || this.isPlayer(), "When running from console, you must specify a player name!");

			if (hasParam)
				this.findOfflinePlayer(param, this::handlePlaytime);
			else
				this.handlePlaytime(this.getPlayer());
		}
	}

	private void handlePlaytime(OfflinePlayer player) {
		final long minutes = PlayerUtil.getStatistic(player, Remain.getPlayTimeStatisticName()) / 20;

		this.tellInfo(player.getName() + " has spent " + (TimeUtil.formatTimeDays(minutes)) + " on this server.");
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWord(Common.getPlayerNames(), TemporaryStorage.getPlayerNames(), "top");
		}

		return NO_COMPLETE;
	}
}