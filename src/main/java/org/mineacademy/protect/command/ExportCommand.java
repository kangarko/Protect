package org.mineacademy.protect.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.remain.CompMaterial;

import com.google.gson.JsonObject;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * The command to export items to a NBT string.
 */
final class ExportCommand extends ProtectSubCommand {

	ExportCommand() {
		super("export");

		this.setDescription("Export the item you or other player holds into a NBT string. By default we export to your clipboard, but you can use different flags to export to other places.");
		this.setUsage("[player] -console -file -chat");
		this.setMinArguments(0);
	}

	@Override
	protected void onCommand() {
		final Player target = this.findPlayerOrSelf(0);
		final ItemStack item = target.getItemInHand();
		final String flag = this.args.length > 1 ? this.args[1].toLowerCase() : "";

		this.checkBoolean(!CompMaterial.isAir(item), "Cannot export air. I know some countries export air in cans but this is Minecraft plugin, not here.");

		try {
			final JsonObject json = Bukkit.getUnsafe().serializeItemAsJson(item);
			final String intro = "&7Serialized &6" + ChatUtil.capitalizeFully(item.getType()) + ". &7";

			if (flag.isEmpty())
				this.tellSuccess(intro + "<hover:show_text:'<gold>Click to copy'><u><click:copy_to_clipboard:'"
						+ MiniMessage.miniMessage().escapeTags(json.toString()) + "'>Click here to copy it to clipboard</click>.</u></hover>.");

			else if ("-console".equals(flag))
				Common.log(intro + "Output: &f" + json);

			else if ("-file".equals(flag)) {
				final String path = "exports/" + System.currentTimeMillis() + "-" + target.getName() + "_" + item.getType().toString().toLowerCase() + ".txt";
				FileUtil.write(path, json.toString());

				this.tellSuccess(intro + "Wrote to: " + path);

			} else if ("-chat".equals(flag)) {
				this.tell(intro + "Output: &f" + CommonCore.GSON_PRETTY.toJson(json).replace("\"", "&7\"&f"));

			} else
				this.returnTell("Invalid flag. Accepted: -console, -file or -chat, one at the time.");

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
				return this.completeLastWord("-console", "-file", "-chat");
		}

		return NO_COMPLETE;
	}
}