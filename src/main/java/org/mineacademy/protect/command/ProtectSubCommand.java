package org.mineacademy.protect.command;

import org.mineacademy.fo.command.SimpleSubCommand;

/**
 * The subcommand hosting all subcommands for this plugin.
 */
abstract class ProtectSubCommand extends SimpleSubCommand {

	/**
	 * Create a new subcommand.
	 *
	 * @param sublabel
	 */
	ProtectSubCommand(final String sublabel) {
		super(sublabel);
	}
}
