package org.mineacademy.protect.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.CompEnchantment;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.protect.model.FastMatcher;
import org.mineacademy.protect.model.FastMathMatcher;
import org.mineacademy.protect.model.ScanCause;
import org.mineacademy.protect.model.Tags;
import org.mineacademy.protect.model.db.Item;
import org.mineacademy.protect.model.db.Item.InventorySnapshot;
import org.mineacademy.protect.settings.Settings;

import lombok.Getter;

@Getter
public abstract class ProtectOperator extends Operator {

	private Integer requireAmount;

	private final Set<ScanCause> requireCauses = new HashSet<>();
	private FastMatcher requireName;
	private Integer requireNameLength;
	private String requireLore;
	private Integer requireLoreLength;
	private FastMathMatcher requireDurability;
	private Integer requirePotionAmount;
	private Integer requirePotionDuration;
	private Integer requirePotionAmplifier;
	private Integer requireEnchantLevel;
	private Integer requireTagLength;
	private final Map<String, String> requirePersistentTags = new HashMap<>();

	private final Set<ScanCause> ignoreCauses = new HashSet<>();
	private final Set<String> ignoreTags = new HashSet<>();
	private final Set<FastMatcher> ignoreMaterials = new HashSet<>();
	private FastMatcher ignoreInventoryTitle;
	private Integer ignoreInventoryAmount;
	private Integer ignoreEnchantLevel;
	private final List<Enchantment> ignoreEnchants = new ArrayList<>();

	private boolean checkMaxStackSize;
	private boolean checkEnchantNotApplicable;
	private boolean checkEnchantTooHigh;

	private boolean disenchant = false;
	private boolean nerfEnchant = false;
	private boolean clone = false;
	private boolean confiscate = false;
	private boolean confiscateOverLimit = false;

	@Override
	public boolean onParse(String firstTwo, String theRestTwo, String[] args) {
		final String firstThree = Common.joinRange(0, 3, args);
		final String theRestThree = args.length >= 3 ? Common.joinRange(3, args) : "";

		final List<String> theRestTwoSplit = splitVertically(theRestTwo);

		if ("require amount".equals(firstTwo))
			this.requireAmount = Integer.parseInt(theRestTwo);

		else if ("require name length".equals(firstThree))
			this.requireNameLength = Integer.parseInt(theRestThree);

		else if ("require cause".equals(firstTwo)) {
			final String[] causes = theRestTwo.split("\\|");

			for (final String cause : causes)
				this.requireCauses.add(ReflectionUtil.lookupEnum(ScanCause.class, cause));
		}

		else if ("require name".equals(firstTwo))
			this.requireName = FastMatcher.compile(theRestTwo);

		else if ("require lore length".equals(firstThree))
			this.requireLoreLength = Integer.parseInt(theRestThree);

		else if ("require lore".equals(firstTwo))
			this.requireLore = theRestTwo;

		else if ("require durability".equals(firstTwo))
			this.requireDurability = FastMathMatcher.compile(theRestTwo);

		else if ("require potion amount".equals(firstThree))
			this.requirePotionAmount = Integer.parseInt(theRestThree);

		else if ("require potion duration".equals(firstThree))
			this.requirePotionDuration = Integer.parseInt(theRestThree);

		else if ("require potion amplifier".equals(firstThree))
			this.requirePotionAmplifier = Integer.parseInt(theRestThree);

		else if ("require enchant level".equals(firstThree))
			this.requireEnchantLevel = Integer.parseInt(theRestThree);

		else if ("require tag length".equals(firstThree))
			this.requireTagLength = Integer.parseInt(theRestThree);

		else if ("require persistent tag".equals(firstThree)) {
			final String[] split = theRestThree.split(" ");

			if (split.length != 2 && split.length != 1)
				throw new FoException("Invalid format for require persistent tag: " + theRestThree + ", expected syntax: <key> <value> or <key> to match any value if key exists", false);

			this.requirePersistentTags.put(split[0], split.length == 2 ? split[1] : "");
		}

		else if ("ignore cause".equals(firstTwo)) {
			final String[] causes = theRestTwo.split("\\|");

			for (final String cause : causes)
				this.ignoreCauses.add(ReflectionUtil.lookupEnum(ScanCause.class, cause));
		}

		else if ("ignore tag".equals(firstTwo))
			this.ignoreTags.add(theRestTwo);

		else if ("ignore material".equals(firstTwo))
			for (final String materialName : theRestTwoSplit)
				this.ignoreMaterials.add(FastMatcher.compile(materialName));

		else if ("ignore inventory title".equals(firstThree))
			this.ignoreInventoryTitle = FastMatcher.compile(theRestThree);

		else if ("ignore inventory amount".equals(firstThree))
			this.ignoreInventoryAmount = Integer.parseInt(theRestThree);

		else if ("ignore enchantlevel".equals(firstTwo))
			this.ignoreEnchantLevel = Integer.parseInt(theRestTwo);

		else if ("ignore enchant".equals(firstTwo))
			for (final String enchantName : theRestTwoSplit)
				this.ignoreEnchants.add(CompEnchantment.getByName(enchantName));

		else if ("check stack size".equals(firstThree))
			this.checkMaxStackSize = true;

		else if ("check enchant not-applicable".equals(firstThree))
			this.checkEnchantNotApplicable = true;

		else if ("check enchant too-high".equals(firstThree))
			this.checkEnchantTooHigh = true;

		else if ("then disenchant".equals(firstTwo)) {
			Valid.checkBoolean(!this.disenchant, "then disenchant already used on " + this);

			this.disenchant = true;

		} else if ("then nerf".equals(firstTwo)) {
			Valid.checkBoolean(!this.nerfEnchant, "then neft already used on " + this);

			this.nerfEnchant = true;

		} else if ("then clone".equals(firstTwo)) {
			Valid.checkBoolean(!this.clone, "then clone already used on " + this);

			this.clone = true;

		} else if ("then confiscate excess".equals(firstThree)) {
			Valid.checkBoolean(!this.confiscateOverLimit, "then confiscate excess already used on " + this);

			this.confiscateOverLimit = true;
		}

		else if ("then confiscate".equals(firstTwo) || "then take".equals(firstTwo) || "then deny".equals(firstTwo)) {
			Valid.checkBoolean(!this.confiscate, "then confiscate already used on " + this);

			this.confiscate = true;

		} else
			return super.onParse(firstTwo, theRestTwo, args);

		return true;
	}

	@Override
	protected SerializedMap collectOptions() {
		return super.collectOptions().putArray(
				"Require Cause", this.requireCauses,
				"Require Amount", this.requireAmount,
				"Require Name Length", this.requireNameLength,
				"Require Name", this.requireName,
				"Require Lore Length", this.requireLoreLength,
				"Require Lore", this.requireLore,
				"Require Durability", this.requireDurability,
				"Require Potion Amount", this.requirePotionAmount,
				"Require Potion Duration", this.requirePotionDuration,
				"Require Potion Amplifier", this.requirePotionAmplifier,
				"Require Enchant Level", this.requireEnchantLevel,
				"Require Tag Length", this.requireTagLength,
				"Require Persistent Tag", this.requirePersistentTags,
				"Ignore Cause", this.ignoreCauses,
				"Ignore Tags", this.ignoreTags,
				"Ignore Materials", this.ignoreMaterials,
				"Ignore Inventory Title", this.ignoreInventoryTitle,
				"Ignore Inventory Amount", this.ignoreInventoryAmount,
				"Ignore Enchant Level", this.ignoreEnchantLevel,
				"Ignore Enchants", this.ignoreEnchants,
				"Check Stack Size", this.checkMaxStackSize,
				"Check Enchant Unnatural", this.checkEnchantNotApplicable,
				"Check Enchant Too High", this.checkEnchantTooHigh,
				"Disenchant", this.disenchant,
				"Nerf Enchant", this.nerfEnchant,
				"Clone", this.clone,
				"Confiscate", this.confiscate,
				"Confiscate Excess", this.confiscateOverLimit);
	}

	public static class ProtectCheck<T extends ProtectOperator> extends OperatorCheck<T> {

		/**
		 * The current evaluated objects
		 */
		protected ScanCause cause;
		protected Location location;
		protected Operator ruleOrGroupEvaluated;
		protected Operator ruleForGroup;
		protected ItemStack item;
		protected CompMaterial material;
		protected String materialName;

		protected ItemStack[] contents;
		protected String inventoryTitle;
		protected InventorySnapshot inventorySnapshot;

		@Getter
		private boolean modified;

		@Getter
		private boolean ruleModified;

		private int removedExcess = 0;

		/**
		 * If player has multiple items matching the same rule, we bulk them
		 */
		@Getter
		private final Map<Rule, List<ItemStack>> loggedItems = new HashMap<>();

		/**
		 * Used to verbose only those rules that take some action not just match
		 */
		private final List<String> verboseMessage = new ArrayList<>();

		protected ProtectCheck(Player player) {
			super(player);
		}

		private boolean hasRequireMeta(T operator) {
			return operator.getRequireNameLength() != null ||
					operator.getRequireLoreLength() != null ||
					operator.getRequirePotionAmount() != null ||
					operator.getRequirePotionDuration() != null ||
					operator.getRequirePotionAmplifier() != null ||
					operator.getRequireTagLength() != null;
		}

		private boolean hasPotionFilters(T operator) {
			return operator.getRequirePotionAmount() != null || operator.getRequirePotionDuration() != null || operator.getRequirePotionAmplifier() != null;
		}

		private boolean hasEnchantFilters(T operator) {
			return !operator.getIgnoreEnchants().isEmpty() || operator.getRequireEnchantLevel() != null;
		}

		@Override
		protected boolean canFilter(T operator) {
			if (operator.getIgnoreCauses().contains(this.cause)) {
				Debugger.debug("operator", "\tIgnoring due to cause " + this.cause + " being in ignored causes: " + operator.getIgnoreCauses());

				return false;
			}

			if (!operator.getRequireCauses().isEmpty() && !operator.getRequireCauses().contains(this.cause)) {
				Debugger.debug("operator", "\tIgnoring due to cause " + this.cause + " not being in required causes: " + operator.getRequireCauses());

				return false;
			}

			if (!super.canFilter(operator))
				return false;

			final int amount = this.item.getAmount();
			final ItemMeta meta = this.item.getItemMeta();

			if (operator.getRequireAmount() != null && amount < operator.getRequireAmount()) {
				Debugger.debug("operator", "\tIgnoring due to amount " + amount + " being less than required " + operator.getRequireAmount());

				return false;
			}

			if (this.inventoryTitle != null && operator.getIgnoreInventoryTitle() != null && operator.getIgnoreInventoryTitle().find(this.inventoryTitle)) {
				Debugger.debug("operator", "\tIgnoring inventory title '" + this.inventoryTitle);

				return false;
			}

			if (meta == null && this.hasRequireMeta(operator)) {
				Debugger.debug("operator", "\tIgnoring due to missing meta while the rule requires some");

				return false;
			}

			if (meta != null) {
				if (!meta.hasDisplayName() && operator.getRequireNameLength() != null) {
					Debugger.debug("operator", "\tIgnoring due to missing display name while minimum length is required");

					return false;
				}

				if (operator.getRequireNameLength() != null && CompChatColor.stripColorCodes(meta.getDisplayName()).length() < operator.getRequireNameLength()) {
					Debugger.debug("operator", "\tIgnoring due to display name length being less than required " + operator.getRequireNameLength());

					return false;
				}

				if (operator.getRequireName() != null && !operator.getRequireName().find(CompChatColor.stripColorCodes(meta.getDisplayName()).trim())) {
					Debugger.debug("operator", "\tIgnoring due to display name '" + meta.getDisplayName() + "' not matching required pattern: " + operator.getRequireName());

					return false;
				}

				if (MinecraftVersion.atLeast(V.v1_16))
					if (!operator.getRequirePersistentTags().isEmpty()) {
						if (meta.getPersistentDataContainer() == null) {
							Debugger.debug("operator", "\tIgnoring due to missing persistent data container while the rule requires one");

							return false;
						}

						boolean found = false;

						final PersistentDataContainer container = meta.getPersistentDataContainer();
						final Map<String, Object> tags = ReflectionUtil.getFieldContent(container, "customDataTags");

						for (final NamespacedKey key : container.getKeys()) {
							final String requiredValue = operator.getRequirePersistentTags().get(key.getKey());

							if (requiredValue != null) {
								final Object tag = tags.get(key.toString());
								String nmsValue = tag.toString();

								if (nmsValue.startsWith("\"") && nmsValue.endsWith("\""))
									nmsValue = nmsValue.substring(1, nmsValue.length() - 1);

								Debugger.debug("operator", "\tFound persistent tag '" + key.getKey() + "' with value '" + nmsValue + "' (" + tag.getClass().getSimpleName() + ")");

								if (requiredValue.equalsIgnoreCase(nmsValue)) {
									Debugger.debug("operator", "\t\tMatched value " + nmsValue);
									found = true;

									break;
								}

								else if (requiredValue.isEmpty()) {
									Debugger.debug("operator", "\t\tMatched by any value since operator has only key specified. Found value: " + nmsValue);
									found = true;

									break;
								}
							}
						}

						if (!found) {
							Debugger.debug("operator", "\tIgnoring due to missing persistent tags '" + operator.getRequirePersistentTags() + "' on " + this.item.getType());

							return false;
						}
					}

				if (meta.hasLore()) {
					if (meta.getLore().contains("mcMMO Ability Tool")) {
						Debugger.debug("operator", "\tIgnoring mcMMO Ability Tool on " + this.item +
								(this.getPlayer() != null ? " for " + this.getPlayer().getName() : ""));

						return false;
					}
				}

				if (operator.getRequireLoreLength() != null || operator.getRequireLore() != null) {
					if (!meta.hasLore() && (operator.getRequireLoreLength() != null || operator.getRequireLore() != null)) {
						Debugger.debug("operator", "\tIgnoring due to missing lore while the rule requires one");

						return false;
					}

					final List<String> lore = meta.getLore();
					lore.removeIf(String::isEmpty);

					final String joinedLore = Common.join(lore, "|", line -> CompChatColor.stripColorCodes(line).trim());

					if (operator.getRequireLore() != null && !operator.getRequireLore().equalsIgnoreCase(joinedLore)) {
						Debugger.debug("operator", "\tIgnoring due to lore not matching required pattern: " + operator.getRequireLore() + " vs " + joinedLore);

						return false;
					}

					if (operator.getRequireLoreLength() != null && String.join("", lore).length() < operator.getRequireLoreLength()) {
						Debugger.debug("operator", "\tIgnoring due to lore length being less than required " + operator.getRequireLoreLength());

						return false;
					}
				}

				if (operator.getRequireDurability() != null && !operator.getRequireDurability().isInLimit(this.item.getDurability())) {
					Debugger.debug("operator", "\tIgnoring due to durability " + this.item.getDurability() + " not in required range " + operator.getRequireDurability());

					return false;
				}

				if (meta instanceof PotionMeta) {
					final PotionMeta potionMeta = (PotionMeta) meta;
					final List<PotionEffect> effects = potionMeta.getCustomEffects();

					if (effects.isEmpty() && this.hasPotionFilters(operator)) {
						Debugger.debug("operator", "\tIgnoring potion item with no effects while the rule requires some");

						return false;
					}

					if (operator.getRequirePotionAmount() != null && effects.size() < operator.getRequirePotionAmount()) {
						Debugger.debug("operator", "\tIgnoring due to potion amount " + effects.size() + " being less than required " + operator.getRequirePotionAmount());

						return false;
					}

					for (final PotionEffect effect : potionMeta.getCustomEffects()) {
						if (operator.getRequirePotionDuration() != null && effect.getDuration() < operator.getRequirePotionDuration()) {
							Debugger.debug("operator", "\tIgnoring due to potion effect duration " + effect.getDuration() + " being less than required " + operator.getRequirePotionDuration());

							return false;
						}

						if (operator.getRequirePotionAmplifier() != null && effect.getAmplifier() < operator.getRequirePotionAmplifier()) {
							Debugger.debug("operator", "\tIgnoring due to potion effect amplifier " + effect.getAmplifier() + " being less than required " + operator.getRequirePotionAmplifier());

							return false;
						}
					}

				} else if (this.hasPotionFilters(operator))
					Debugger.debug("operator", "Rule with potion checks can only be used for potion items, skipping.");

				if (operator.getRequireTagLength() != null && this.nbtItem.toString().length() < operator.getRequireTagLength()) {
					Debugger.debug("operator", "\tIgnoring due to NBT tag length " + this.nbtItem.toString().length() + " being less than required " + operator.getRequireTagLength());

					return false;
				}

				if (!operator.getIgnoreTags().isEmpty()) {
					boolean found = false;

					for (final String ignoredTag : operator.getIgnoreTags())
						if (this.nbtItem.hasTag(ignoredTag)) {
							found = true;

							break;
						}

					if (found) {
						Debugger.debug("operator", "\tIgnoring due to having ignored tag: " + operator.getIgnoreTags());

						return false;
					}
				}
			}

			if (!operator.getIgnoreMaterials().isEmpty())
				for (final FastMatcher ignoreMatcher : operator.getIgnoreMaterials())
					if (ignoreMatcher.find(this.materialName)) {
						Debugger.debug("operator", "\tIgnoring material " + this.item.getType());

						return false;
					}

			if (operator.isCheckMaxStackSize() && amount <= this.item.getMaxStackSize()) {
				Debugger.debug("operator", "\tIgnoring due to stack size " + amount + " being less than max stack size " + this.item.getMaxStackSize());

				return false;
			}

			if (this.item.getEnchantments().isEmpty() && this.hasEnchantFilters(operator)) {
				Debugger.debug("operator", "\tIgnoring due to no enchantments while the rule requires some");

				return false;
			}

			final Map<Enchantment, Integer> enchants = this.item.getEnchantments();

			for (final Enchantment enchant : operator.getIgnoreEnchants())
				if (enchants.containsKey(enchant)) {
					Debugger.debug("operator", "\tIgnoring due to having ignored enchantment: " + enchant);

					return false;
				}

			if (operator.getRequireEnchantLevel() != null) {
				boolean found = false;

				for (final Enchantment enchant : enchants.keySet()) {
					if (enchants.get(enchant) >= operator.getRequireEnchantLevel()) {
						found = true;

						break;
					}
				}

				if (!found) {
					Debugger.debug("operator", "\tIgnoring due to no enchantment with minimum required level " + operator.getRequireEnchantLevel());

					return false;
				}
			}

			if (operator.isCheckEnchantNotApplicable()) {
				boolean ignore = true;

				for (final Enchantment enchant : enchants.keySet())
					if (!enchant.canEnchantItem(this.item)) {
						ignore = false;

						break;
					}

				if (ignore) {
					Debugger.debug("operator", "\tIgnoring due to only having enchantments applicable to the item");

					return false;
				}
			}

			if (operator.isCheckEnchantTooHigh()) {
				boolean ignore = true;

				for (final Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
					final Enchantment enchant = entry.getKey();
					final int level = entry.getValue();

					if (level > enchant.getMaxLevel()) {
						if (operator.getIgnoreEnchantLevel() != null && level <= operator.getIgnoreEnchantLevel()) {
							Debugger.debug("operator", "\tIgnoring enchantment " + enchant + " with level " + level + " on " + this.item.getType() +
									(this.getPlayer() != null ? " for " + this.getPlayer().getName() : ""));

							continue;
						}

						ignore = false;

						if (operator.isNerfEnchant()) {
							this.item.removeEnchantment(enchant);
							this.item.addEnchantment(enchant, enchant.getMaxLevel());
						}
					}
				}

				if (ignore) {
					Debugger.debug("operator", "\tIgnoring due to no enchantment with level higher than allowed");

					return false;
				}
			}

			if (this.contents != null) {
				int found = 0;
				int maxAllowedPieces = this.ruleForGroup instanceof ProtectOperator && ((ProtectOperator) this.ruleForGroup).getIgnoreInventoryAmount() != null ? ((ProtectOperator) this.ruleForGroup).getIgnoreInventoryAmount() : -1;

				// Override from group
				if (operator.getIgnoreInventoryAmount() != null)
					maxAllowedPieces = operator.getIgnoreInventoryAmount();

				if (maxAllowedPieces != -1) {
					for (int i = 0; i < this.contents.length; i++) {
						final ItemStack otherItem = this.contents[i];

						if (otherItem != null && otherItem.getType() == this.item.getType())
							found += otherItem.getAmount();
					}

					if (found > maxAllowedPieces) {
						boolean confiscateOverLimit = this.ruleForGroup instanceof ProtectOperator ? ((ProtectOperator) this.ruleForGroup).isConfiscateOverLimit() : false;

						// Override from group
						if (operator.isConfiscateOverLimit())
							confiscateOverLimit = true;

						if (confiscateOverLimit) {
							int removeCount = found - maxAllowedPieces;

							for (int i = 0; i < this.contents.length && removeCount > 0; i++) {
								final ItemStack otherItem = this.contents[i];

								if (otherItem != null && otherItem.getType() == this.item.getType()) {
									final int otherAmount = otherItem.getAmount();

									if (otherAmount > removeCount) {
										otherItem.setAmount(otherAmount - removeCount);

										// add logged item with the amount which was removed
										final ItemStack clone = otherItem.clone();
										clone.setAmount(removeCount);

										this.addLoggedItem(operator, clone);

										this.contents[i] = otherItem;
										break;

									} else {
										this.contents[i] = null;

										removeCount -= otherAmount;
										this.addLoggedItem(operator, otherItem);
									}
								}
							}

							this.setModified(true);

							this.removedExcess = found - maxAllowedPieces;
						}

					} else {
						Debugger.debug("operator", "\tIgnoring due to having less than limit " + maxAllowedPieces + "x of " + this.item.getType() + " in inventory");

						return false;
					}
				}
			}

			return true;
		}

		/*
		 * Add the item to the list of altered items
		 */
		private void addLoggedItem(T operator, ItemStack item) {
			if (!operator.isIgnoreLogging()) {
				final List<ItemStack> items = this.loggedItems.getOrDefault(operator, new ArrayList<>());
				items.add(item.clone());

				this.loggedItems.put((Rule) Common.getOrDefault(this.ruleForGroup, operator), items);
			}
		}

		/**
		 * Log the items if any, and clear the list.
		 */
		protected final void logItems() {
			if (this.getLoggedItems().size() > 0) {

				for (final Map.Entry<Rule, List<ItemStack>> entry : this.getLoggedItems().entrySet()) {
					final Rule rule = entry.getKey();
					final ItemStack[] items = entry.getValue().toArray(new ItemStack[entry.getValue().size()]);

					if (this.hasPlayer)
						Item.log(this.getPlayer(), this.inventorySnapshot, items, rule);
					else
						Item.log(this.cause, this.location, items, rule);
				}

				this.getLoggedItems().clear();
			}
		}

		@Override
		protected void executeExtraOperators(T operator) {
			if (this.removedExcess > 0) {
				this.verbosePush(operator, "&cRemoved " + this.removedExcess + " excess items.");

				this.removedExcess = 0;
			}

			if (operator.isDisenchant()) {
				this.verbosePush(operator, "&cItem disenchanted.");

				this.addLoggedItem(operator, this.item);

				for (final Enchantment enchantment : this.item.getEnchantments().keySet())
					this.item.removeEnchantment(enchantment);

				this.setModified(true);
			}

			if (operator.isClone()) {
				this.addLoggedItem(operator, this.item);

				this.nbtItem.setBoolean(Tags.CLONED, true);
				this.verbosePush(operator, "&cItem cloned silently.");

				throw new CloneItemException();
			}

			if (operator.isConfiscate()) {
				this.addLoggedItem(operator, this.item);
				this.verbosePush(operator, "&cItem confiscated.");

				throw new EventHandledException(true);
			}
		}

		@Override
		protected SimpleComponent replaceExtraVariables(SimpleComponent message, T operator) {
			message = message
					.replaceBracket("cause", this.cause.toString())
					.replaceBracket("removed_amount", this.removedExcess + "")
					.replaceBracket("world", this.location != null ? this.location.getWorld().getName() : "")
					.replaceBracket("x", this.location != null ? this.location.getBlockX() + "" : "")
					.replaceBracket("y", this.location != null ? this.location.getBlockY() + "" : "")
					.replaceBracket("z", this.location != null ? this.location.getBlockZ() + "" : "")
					.replaceBracket("location", SerializeUtil.serializeLocation(this.location));

			return super.replaceExtraVariables(message, operator);
		}

		/*
		 * Show the message if rules are set to verbose
		 */
		protected final void verbose(Operator operator, String... messages) {
			if (!operator.isIgnoreVerbose() && Settings.Rules.VERBOSE)
				for (final String message : messages)
					this.verboseMessage.add(message);
		}

		/*
		 * Push the verbose message
		 */
		protected final void verbosePush(Operator operator, String... messages) {
			if (!operator.isIgnoreVerbose() && Settings.Rules.VERBOSE) {
				for (final String message : this.verboseMessage)
					Common.log(message);

				Common.log(messages);

				this.verboseMessage.clear();
			}
		}

		protected final void setModified(boolean modified) {
			this.modified = modified;
			this.ruleModified = modified;
		}

		protected final void setRuleModified(boolean ruleModified) {
			this.ruleModified = ruleModified;
		}

		protected final void clearVerboseMessages() {
			this.verboseMessage.clear();
		}
	}

	/**
	 * A helper class used to clone items upstream
	 */
	public static final class CloneItemException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
