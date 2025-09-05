package org.mineacademy.protect.command;

import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.PermsSubCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.protect.model.Permissions;

/**
 * The main plugin command group executing its subcommands
 */
@AutoRegister
public final class ProtectCommandGroup extends SimpleCommandGroup {

	@Override
	protected String getHeaderPrefix() {
		return "&4&l";
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommandGroup#registerSubcommands()
	 */
	@Override
	protected void registerSubcommands() {
		this.registerSubcommand(ProtectSubCommand.class);
		this.registerDefaultSubcommands();

		this.registerSubcommand(new PermsSubCommand(Permissions.class));
	}
}
