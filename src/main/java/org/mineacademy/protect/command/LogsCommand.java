package org.mineacademy.protect.command;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.protect.model.db.ProtectRow;
import org.mineacademy.protect.model.db.ProtectTable;

/**
 * The command to view database logs.
 */
final class LogsCommand extends ProtectSubCommand {

	LogsCommand() {
		super("logs");

		this.setDescription("Browse database logs.");
		this.setUsage("[params]");
		this.setMinArguments(1);
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} <table> - Displays all logs in the table.",
				"/{label} {sublabel} <table> [filters] - Displays logs matching the given filters.",
				"/{label} {sublabel} <table> clear - Deletes all logs from the table.",
				"/{label} {sublabel} <table> clear [filters] - Deletes logs matching the given filters.",
				"/{label} {sublabel} <table> ? - Lists all filters you can use.",
				"",
				"&7Example: /{label} {sublabel} items date:1m player:Notch"
		};
	}

	@Override
	protected void onCommand() {
		final ProtectTable table = this.findEnum(ProtectTable.class, this.args[0]);
		final String param = this.args.length > 1 ? this.args[1] : "";

		if ("?".equals(param)) {
			final List<SimpleComponent> pages = new ArrayList<>();

			for (final Filter filter : Filter.getFilters())
				if (filter.isApplicable(table))
					for (final String usage : filter.getUsages()) {
						pages.add(SimpleComponent.fromMiniAmpersand("&7- &c" + usage));
						pages.add(SimpleComponent.empty());
					}

			new ChatPaginator(10)
					.setFoundationHeader("&6" + ChatUtil.capitalizeFully(table.getKey()) + " Filters")
					.setPages(pages)
					.send(this.audience);

			return;
		}

		tellInfo("Processing table " + table.getKey() + " async...");

		final Tuple<String, List<Filter>> parsed = this.parseFilters(table, this.joinArgs(1));

		if (!"clear".equals(param))
			checkBoolean(parsed.getKey().isEmpty(), "Only type filters in the command. Found string: '" + parsed.getKey() + "'");

		final List<Filter> filters = parsed.getValue();

		runTaskAsync(() -> {

			final List<SimpleComponent> pages = new ArrayList<>();
			final List<SimpleComponent> results = new ArrayList<>();
			final List<ProtectRow> filteredRows = new ArrayList<>();

			int allResults = 0;
			int displayedResults = 0;

			boolean atLeastOne = false;
			final List<ProtectRow> rows = table.getRows();

			for (final ProtectRow row : rows) {
				allResults++;

				boolean canDisplay = true;

				for (final Filter filter : filters)
					if (!filter.canDisplay(row)) {
						canDisplay = false;

						break;
					}

				if (canDisplay) {
					filteredRows.add(row);

					results.add(row.toChatLine());

					atLeastOne = true;
					displayedResults++;
				}
			}

			if (!atLeastOne && filters.isEmpty())
				returnTell("&cTable " + table.getKey() + " is empty.");

			if ("clear".equals(param)) {
				if (!atLeastOne && !filters.isEmpty())
					returnTell("&cNothing to clear according to given filters.");

				for (final ProtectRow row : filteredRows)
					row.delete();

				returnTell("&cCleared " + filteredRows.size() + " logs from table " + table.getKey() + ".");
			}

			pages.add(SimpleComponent.fromMiniAmpersand("&cFilters: " + (filters.isEmpty() ? "None (showing " + displayedResults + " results)" : Common.join(filters, filter -> filter.getIdentifier()) + " (showing " + displayedResults + "/" + allResults + " results)")));
			pages.add(SimpleComponent.empty());

			if (!atLeastOne)
				pages.add(SimpleComponent.fromMiniAmpersand("&cNo results found." + (filters.isEmpty() ? "" : " Try adjusting your filters.")));

			pages.addAll(results);

			new ChatPaginator()
					.setFoundationHeader(ChatUtil.capitalize("&6Listing " + table.getKey() + " logs"))
					.setPages(pages)
					.send(this.audience);
		});
	}

	@Override
	public List<String> tabComplete() {
		if (this.args.length == 1)
			return this.completeLastWord(ProtectTable.values());

		else if (this.args.length > 1) {

			if ("?".equals(this.args[1]))
				return NO_COMPLETE;

			final ProtectTable table;

			try {
				table = this.findEnum(ProtectTable.class, this.args[0]);

			} catch (final Throwable ex) {
				return NO_COMPLETE;
			}

			final List<String> filters = new ArrayList<>();
			final String lastCompletion = this.args[this.args.length - 1];

			for (final Filter filter : Filter.getFilters())
				if (filter.isApplicable(table)) {
					final String filterLabel = filter.getIdentifier() + ":";

					if (lastCompletion.contains(filterLabel) && lastCompletion.startsWith(filterLabel)) {
						for (final String filterComplete : Common.getOrDefault(filter.tabComplete(this.getAudience()), new ArrayList<String>()))
							filters.add(filterLabel + filterComplete);

					} else
						filters.add(filterLabel);
				}

			filters.add("?");
			filters.add("clear");

			return this.completeLastWord(filters);
		}

		return NO_COMPLETE;
	}
}