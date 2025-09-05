package org.mineacademy.protect.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * The command to import items from an NBT string.
 */
final class ImportCommand extends ProtectSubCommand {

	ImportCommand() {
		super("import");

		this.setDescription("Import an itemstack to your hand from a NBT string, see the 'export' subcommand.");
		this.setUsage("<player> chat <string> or file <path>");
		this.setMinArguments(3);
	}

	@Override
	protected void onCommand() {
		final Player target = this.findPlayer(this.args[0]);
		final String param = this.args[1].toLowerCase();

		String source;

		if ("chat".equals(param))
			source = this.joinArgs(2);

		else if ("file".equals(param)) {
			final String file = this.args[2];
			final String path = "exports/" + (file.endsWith(".txt") ? file : file + ".txt");
			final List<String> lines = FileUtil.readLinesFromFile(path);

			this.checkBoolean(lines != null && !lines.isEmpty(), "No such file: " + path + ". Available: " + Common.join(FileUtil.getFiles("exports", "txt")));
			this.checkBoolean(lines.size() > 0, "File " + path + " is empty.");

			source = lines.get(0);

		} else {
			this.returnTell("Invalid param, available: chat or file");

			return;
		}

		try {
			final ItemStack item = Bukkit.getUnsafe().deserializeItemFromJson(CommonCore.GSON.fromJson(source, JsonObject.class));

			if (item != null) {
				target.getInventory().addItem(item);

				this.tellSuccess("&aSuccessfully &7deserialized &6" + ChatUtil.capitalizeFully(item.getType()) + "&7.");

			} else
				this.tellError("&cUnable &7to deserialize item from: " + source);

		} catch (final JsonSyntaxException ex) {
			this.tellError("&cMalformed &7input! Expected to get a valid JSON, got: " + source);

		} catch (final NoSuchMethodError err) {
			this.tellError("This command requires a modern Minecraft version.");
		}
	}

	@Override
	public List<String> tabComplete() {
		switch (this.args.length) {
			case 1:
				return this.completeLastWordPlayerNames();

			case 2:
				return this.completeLastWord("chat", "file");

			case 3: {
				final String param = this.args[1].toLowerCase();

				if ("file".equals(param))
					return this.completeLastWord(FileUtil.getFiles("exports", "txt"));
			}
		}

		return NO_COMPLETE;
	}
}