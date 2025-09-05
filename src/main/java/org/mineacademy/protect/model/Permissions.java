package org.mineacademy.protect.model;

import org.mineacademy.fo.command.annotation.Permission;
import org.mineacademy.fo.command.annotation.PermissionGroup;

/**
 * This class holds all permissions used by the plugin.
 */
public final class Permissions {

	private Permissions() {
	}

	@PermissionGroup("Permissions for plugin commands.")
	public static final class Command {

		@Permission("Permission to reload the plugin.")
		public static final String RELOAD = "protect.command.reload";

		@Permission("Permission to view inventory of a player. Replace type with 'regular', 'armor' or 'enderchest'.")
		public static final String INV = "protect.command.inv.{type}";

		@Permission("Permission to write to the inventory of a player.")
		public static final String INV_WRITE = "protect.command.inv.write";

		@Permission("Permission to close the inventory of a player.")
		public static final String INV_CLOSE = "protect.command.invclose";
	}

	@PermissionGroup("Permissions for bypassing certain checks.")
	public static final class Bypass {

		@Permission("Your inventory won't be scanned. By default, this permission is false even for operators.")
		public static final String SCAN = "protect.bypass.scan";

		@Permission("Your commands will be ignored from command spy.")
		public static final String COMMAND = "protect.bypass.command";

		@Permission("Your shop actions will leave no traces.")
		public static final String TRANSACTION = "protect.bypass.transaction";

		@Permission("Permission to bypass item limit for WorldEdit operations. Since some operations work with thousands of items, we do not allow only certain items to be bypassed because of the performance impact could freeze the server.")
		public static final String WORLDEDIT = "protect.bypass.worldedit";
	}

	@PermissionGroup("Permissions for notifications.")
	public static final class Notify {

		@Permission("Permission to receive alerts when a command is executed.")
		public static final String COMMAND_SPY = "protect.notify.commandspy";

		@Permission("Permission to receive alerts when a transaction is completed.")
		public static final String TRANSACTION = "protect.notify.transaction";

		@Permission("Permission to receive alerts when an item is matched against rules.")
		public static final String ITEM = "protect.notify.item";
	}

	@Permission("Permission to be placed in a group. Replace group name with the actual group name.")
	public static final String GROUP = "protect.group.{group_name}";
}