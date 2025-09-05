package org.mineacademy.protect.command;

import java.util.List;
import java.util.Map;

import org.bukkit.FireworkEffect;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompPotionEffectType;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

/**
 * The command to view offline player inventory content.
 */
final class ItemInfoCommand extends ProtectSubCommand {

	ItemInfoCommand() {
		super("iteminfo|if");

		this.setDescription("Show your or a player's held item's info.");
		this.setUsage("<meta/string/nbt> [player]");
		this.setMinArguments(1);
	}

	@Override
	protected String[] getMultilineUsageMessage() {
		return new String[] {
				"/{label} {sublabel} meta [player] - Print the name, id, data, enchants, potions and firework effects.",
				"/{label} {sublabel} string [player] - Print the ItemStack toString() info from Bukkit.",
				"/{label} {sublabel} nbt [player] - Print the full item nbt tag.",
		};
	}

	@Override
	protected void onCommand() {
		final Param param = Param.find(this.args[0]);
		this.checkNoSuchType(param, "param", "{0}", Param.values());

		final Player player = this.findPlayerOrSelf(1);
		final ItemStack item = player.getInventory().getItemInHand();

		this.checkBoolean(!CompMaterial.isAir(item), player.getName() + " holds no item.");

		if (param == Param.META) {
			final boolean hasData = MinecraftVersion.olderThan(V.v1_13);

			tellNoPrefix(
					"",
					"&6&lItem information:",
					"",
					"&7Type: &f" + item.getType() + (hasData && item.getData().getData() != 0 ? ":" + item.getData().getData() : "") + (hasData ? " (ID: " + item.getType().getId() + ":" + item.getData().getData() + ")" : ""),
					"&7Durability: &f" + item.getDurability());

			if (MinecraftVersion.olderThan(V.v1_13))
				this.audience.sendMessage(SimpleComponent
						.fromMiniNative("<gray><u>Modern name:<reset> ")
						.onHoverLegacy("&6Use this name in rules", "&6for maximum compatibility")
						.appendPlain(CompMaterial.fromItem(item).name()));

			if (item.hasItemMeta()) {
				final ItemMeta meta = item.getItemMeta();

				try {
					tellNoPrefix(SimpleComponent.fromMiniAmpersand("&7Display name: &f").append(meta.hasDisplayName() ? meta.displayName() : Component.text("unset")));

				} catch (final NoSuchMethodError ex) {
					tellNoPrefix("&7Display name: &f" + (meta.hasDisplayName() ? meta.getDisplayName() : "unset"));
				}

				tellNoPrefix("&7Lore: &f" + (meta.hasLore() ? "{" : "unset"));

				if (meta.hasLore()) {

					try {
						for (final Component lore : meta.lore())
							tellNoPrefix(SimpleComponent.fromMiniAmpersand("  &7- &r").append(lore));

					} catch (final NoSuchMethodError ex) {
						for (final String lore : meta.getLore())
							tellNoPrefix("  &7- &r" + lore);
					}

					tellNoPrefix("}");
				}

				try {
					tellNoPrefix("&7Component: &f" + meta.getAsComponentString());

				} catch (final NoSuchMethodError err) {
					try {
						tellNoPrefix("&7Component: &f" + meta.getAsString());

					} catch (final NoSuchMethodError err2) {
						// Ignore
					}
				}

				try {
					if (meta.hasCustomModelData())
						tellNoPrefix("&7Model data: &f" + meta.getCustomModelData());

				} catch (final NoSuchMethodError err) {
					// Ignore
				}

				if (meta instanceof PotionMeta) {

					try {
						if (((PotionMeta) meta).hasBasePotionType())
							tellNoPrefix("&7Base type: &f" + ((PotionMeta) meta).getBasePotionType());

					} catch (final NoSuchMethodError err) {
						// Too old MC
					}

					final List<PotionEffect> effects = ((PotionMeta) meta).getCustomEffects();

					if (!effects.isEmpty())
						tellNoPrefix("&7Effects: &f" + String.join(", ", Common.convertList(effects, effect -> CompPotionEffectType.getLoreName(effect.getType()) + " " + (effect.getAmplifier() + 1) + " (" + TimeUtil.formatTimeShort(effect.getDuration() / 20) + ")")));

				} else if (meta instanceof FireworkMeta) {
					final List<FireworkEffect> effects = ((FireworkMeta) meta).getEffects();

					if (!effects.isEmpty())
						tellNoPrefix("&7Effects: &f" + String.join(", ", Common.convertList(effects, effect -> ChatUtil.capitalizeFully(effect.getType()) + " (colors: " + Common.join(effect.getColors()) + ", fade colors: " + Common.join(effect.getFadeColors()) + ")")));
				}
			} else
				tellNoPrefix("&7Item has no custom meta.");

			final Map<Enchantment, Integer> enchants = item.getEnchantments();

			if (!enchants.isEmpty())
				tellNoPrefix("&7Enchants: &f" + String.join(", ", Common.convertMapToList(enchants, (key, value) -> ChatUtil.capitalizeFully(key.getName()) + " " + MathUtil.toRoman(value))));

		} else if (param == Param.STRING) {
			tellNoPrefix("&6Item information:");

			final String string = item.toString();

			if (string.length() > Short.MAX_VALUE / 2) {
				tellNoPrefix("toString() is too long (" + string.length() + " characters), printing to console instead.");

				Common.log(string);
			} else
				tell(SimpleComponent.fromMiniAmpersand("&7toString(): &f" + string).onHoverLegacy("&7Click to copy").onClickSuggestCmd(string));

		} else if (param == Param.NBT) {
			tellNoPrefix("&6Item information:");

			final String nbt = new NBTItem(item).toString();

			if (nbt.length() > Short.MAX_VALUE / 2) {
				tellNoPrefix("Item NBT tag is too long (" + nbt.length() + " characters), printing to console instead.");

				Common.log(nbt);
			} else
				tell(SimpleComponent.fromMiniAmpersand("&7NBT: &f" + nbt).onHoverLegacy("&7Click to copy").onClickSuggestCmd(nbt));
		}
	}

	@Override
	public List<String> tabComplete() {
		final Param param = this.args.length > 0 ? Param.find(this.args[0]) : null;

		switch (this.args.length) {
			case 1:
				return this.completeLastWord(Param.values());
			case 2:
				return param != null ? this.completeLastWordPlayerNames() : NO_COMPLETE;
		}

		return NO_COMPLETE;
	}

	@RequiredArgsConstructor
	@Getter
	private enum Param {
		META("meta"),
		STRING("string"),
		NBT("nbt");

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