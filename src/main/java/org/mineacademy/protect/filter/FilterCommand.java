package org.mineacademy.protect.filter;

import java.util.Arrays;
import java.util.Collection;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.protect.model.FastMatcher;
import org.mineacademy.protect.model.db.CommandSpy;
import org.mineacademy.protect.model.db.ProtectTable;

public final class FilterCommand extends Filter {

	private FastMatcher commandPattern;

	public FilterCommand() {
		super("command");
	}

	@Override
	public boolean isApplicable(Table table) {
		return table == ProtectTable.COMMAND;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"command:<command> - Show results matching the given command label. Spaces not supported."
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Arrays.asList("/command");
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.commandPattern = FastMatcher.compile(value.toLowerCase());

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		String command = ((CommandSpy) row).getCommand().split(" ")[0];

		command = ChatUtil.replaceDiacritic(command);
		command = CompChatColor.stripColorCodes(command);

		return this.commandPattern.find(command.toLowerCase());
	}
}
