package org.mineacademy.protect.command;

import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.CompEnchantment;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompPotionEffectType;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The command to view offline player inventory content.
 */
final class EditItemCommand extends ProtectSubCommand {

	EditItemCommand() {
		super("edititem|ei");

		this.setMinArguments(2);
		this.setDescription("Edit held item's properties.");
		this.setUsage("<name/lore/potion/enchant/amount> [param]");
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} name <value> - Set held item's name. Use & or MiniMessage color codes.",
				"/{label} {sublabel} name reset - Resets the name.",
				"/{label} {sublabel} lore <line1|line2> - Set held item's lore. Use & or MiniMessage color codes.",
				"/{label} {sublabel} lore reset - Resets the lore.",
				"/{label} {sublabel} potion <type> <duration> <amplifier> - Set held item's potion effect. Available: https://mineacademy.org/potions",
				"&7Example: /{label} {sublabel} potion speed 1m30s 1",
				"/{label} {sublabel} potion <type> reset - Remove a custom effect from item.",
				"/{label} {sublabel} enchant <type> <level> - Set held item's enchant. Available: https://mineacademy.org/enchants",
				"/{label} {sublabel} enchant reset - Remove all enchantments.",
				"/{label} {sublabel} amount 64 - Set held item's amount to 64.",
		};
	}

	@Override
	protected void onCommand() {
		this.checkConsole();

		final Player player = this.getPlayer();
		final Param param = Param.find(this.args[0]);
		this.checkNoSuchType(param, "param", "{0}", Param.values());

		final String value = this.args[1];
		final String theRest = this.joinArgs(1);

		final ItemStack item = player.getInventory().getItemInHand();
		this.checkBoolean(!CompMaterial.isAir(item), "You must hold an item to edit it.");

		if (param == Param.ENCHANT) {
			if ("reset".equals(value)) {
				item.getEnchantments().keySet().forEach(item::removeEnchantment);
				tellSuccess("All enchantments removed from the item.");

				return;
			} else {
				final Enchantment enchant = CompEnchantment.getByName(value);
				this.checkNoSuchType(enchant, "enchant", value, CompEnchantment.getEnchantments());

				final int level = this.args.length > 2 ? this.findInt(2, 0, Integer.MAX_VALUE, "Provide a valid enchant level.") : 1;
				final boolean natural = enchant.canEnchantItem(item);

				item.addUnsafeEnchantment(enchant, level);
				tellSuccess((natural ? "Natural" : "Unnatural") + " enchantment " + enchant.getName() + " lvl " + level + " added.");

				return;
			}
		}

		final ItemMeta meta = item.getItemMeta();
		this.checkNotNull(meta, "Item in hand has no meta.");

		if (param == Param.NAME) {
			if ("reset".equals(value)) {
				meta.setDisplayName(null);

				tellSuccess("Item name removed.");
			} else {
				meta.setDisplayName(SimpleComponent.fromMiniAmpersand("&r" + theRest).toLegacySection());

				tellSuccess("Item name set to '" + theRest + "&7'.");
			}

			item.setItemMeta(meta);

		} else if (param == Param.LORE) {
			if ("reset".equals(value)) {
				meta.setLore(null);

				tellSuccess("Item lore removed.");

			} else {
				final List<String> lore = Common.toList(CompChatColor.translateColorCodes("&r&7" + theRest.replace("|", "|&r&7")).split("\\|"));

				meta.setLore(lore);
				tellSuccess("Item lore set to '" + theRest + "&7'.");
			}

			item.setItemMeta(meta);

		} else if (param == Param.POTION) {
			this.checkBoolean(meta instanceof PotionMeta, "Item in hand is not a potion.");

			if ("reset".equals(value)) {
				((PotionMeta) meta).clearCustomEffects();

				tellSuccess("All custom effects removed from the item.");

			} else {
				this.checkArgs(3, "Usage: /{label} {sublabel} {0} <effect> <duration> <amplifier>");

				final PotionEffectType potion = CompPotionEffectType.getByName(value);
				this.checkNoSuchType(potion, "effect", value, CompPotionEffectType.getPotions());

				long durationTicks = this.findTimeMillis(args[2]) / 50L;

				if (durationTicks > Integer.MAX_VALUE)
					durationTicks = Integer.MAX_VALUE;

				final int amplifier = this.findInt(3, 0, Integer.MAX_VALUE, "Provide a valid amplifier.");

				Remain.setPotion(item, potion, durationTicks, amplifier);
				getPlayer().setItemInHand(item);

				tellSuccess("Added effect " + ChatUtil.capitalizeFully(potion.getName()) + " " + amplifier + " for " + TimeUtil.formatTimeDays(durationTicks / 20L) + ".");
			}

		} else if (param == Param.AMOUNT) {
			final int amount = this.findInt(1, 1, 999, "Provide a valid amount from 1 to 999.");

			item.setAmount(amount);
			tellSuccess("Item amount set to " + amount + "." + (amount >= 384 ? " Note that Protect will remove the item on the next scan as it is invalid." : ""));
		}
	}

	@Override
	public List<String> tabComplete() {
		final Param param = this.args.length > 0 ? Param.find(this.args[0]) : null;

		switch (this.args.length) {
			case 1:
				return this.completeLastWord(Param.values());

			case 2: {
				if (param == Param.ENCHANT)
					return this.completeLastWord(CompEnchantment.getEnchantments(), "reset");

				else if (param == Param.POTION)
					return this.completeLastWord(CompPotionEffectType.getPotions(), "reset");

				else if (param == Param.AMOUNT)
					return this.completeLastWord(1, 10, 64);

				else
					return this.completeLastWord("reset");
			}

			case 3: {
				if (param == Param.ENCHANT)
					return this.completeLastWord(1, 5);

				else if (param == Param.POTION)
					return this.completeLastWord("2m", "10s", "1m30s");

				else
					return NO_COMPLETE;
			}

			case 4: {
				if (param == Param.POTION)
					return this.completeLastWord(1, 5, 10);

				else
					return NO_COMPLETE;
			}
		}

		return NO_COMPLETE;
	}

	@RequiredArgsConstructor
	@Getter
	private enum Param {
		NAME("name"),
		LORE("lore"),
		POTION("potion"),
		ENCHANT("enchant"),
		AMOUNT("amount");

		private final String name;

		public static Param find(String name) {
			name = name.toLowerCase();

			for (final Param param : values())
				if (param.getName().equals(name))
					return param;

			return null;
		}
	}
}