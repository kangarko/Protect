package org.mineacademy.protect.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.model.TemporaryStorage;
import org.mineacademy.protect.model.db.ProtectRow;

public final class FilterPlayerUid extends Filter {

	private final Set<UUID> playerUids = new HashSet<>();

	public FilterPlayerUid() {
		super("playeruid");
	}

	@Override
	public boolean isApplicable(Table table) {
		return true;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"playeruid:<uuid|uuid2> - Show results for the given player UUID.",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		final Set<String> names = new HashSet<>();

		for (final UUID uuid : TemporaryStorage.getPlayerUids())
			names.add(uuid.toString());

		for (final Player online : Remain.getOnlinePlayers())
			names.add(online.getUniqueId().toString());

		return names;
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.playerUids.clear();

		for (final String split : value.split("\\|"))
			try {
				this.playerUids.add(UUID.fromString(split));

			} catch (final IllegalArgumentException ex) {
				Messenger.error(audience, "Malformed UUID '" + split + "'.");

				return false;
			}

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return this.playerUids.contains(((ProtectRow) row).getPlayerUid());
	}
}
