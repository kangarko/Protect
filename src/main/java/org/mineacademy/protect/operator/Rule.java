package org.mineacademy.protect.operator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTItem;
import org.mineacademy.protect.api.InventoryScanEvent;
import org.mineacademy.protect.api.ItemScanEvent;
import org.mineacademy.protect.api.PlayerScanEvent;
import org.mineacademy.protect.api.PreRuleMatchEvent;
import org.mineacademy.protect.hook.McMMOHook;
import org.mineacademy.protect.model.FastMatcher;
import org.mineacademy.protect.model.Permissions;
import org.mineacademy.protect.model.ScanCause;
import org.mineacademy.protect.model.Tags;
import org.mineacademy.protect.model.db.Item;
import org.mineacademy.protect.model.db.Item.InventorySnapshot;
import org.mineacademy.protect.settings.Settings;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a simple chat rule
 */
@Getter
public final class Rule extends ProtectOperator {

	/**
	 * The rule file
	 */
	private final File file;

	/**
	 * The match of the rule
	 */
	private final FastMatcher matcher;

	/**
	 * The name of the rule, empty if not set
	 */
	private String name;

	/**
	 * Apply rules from the given group name
	 */
	@Nullable
	private Group group;

	/**
	 * Create a new rule of the given type and match operator
	 *
	 * @param file
	 * @param match the regex after the match
	 */
	public Rule(File file, String match) {
		this.file = file;
		this.matcher = FastMatcher.compile(match);
	}

	public String getMatch() {
		return this.matcher.getRawPattern();
	}

	@Override
	public String getUniqueName() {
		return this.name;
	}

	@Override
	public boolean onOperatorParse(String[] args) {
		if ("name".equals(args[0])) {
			this.name = Common.joinRange(1, args);

			if (this.name.contains(" "))
				throw new FoException("Rule name cannot contain spaces: " + this, false);

		} else if ("group".equals(args[0])) {
			this.checkNotSet(this.group, "group");

			final String groupName = Common.joinRange(1, args);
			this.group = Groups.getInstance().findGroup(groupName);

			if (this.group == null)
				throw new FoException("Rule referenced a non-existing group '" + groupName + "'! Rule: " + this, false);

		} else
			return super.onOperatorParse(args);

		return true;
	}

	@Override
	public void onLoadFinish() {
		if (this.name == null)
			throw new FoException("Please set 'name' for rule " + this, false);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Rule " + super.collectOptions().put(SerializedMap.fromArray(
				"Name", this.name,
				"File", this.file.getName(),
				"Match", this.matcher.getRawPattern(),
				"Group", this.group != null ? this.group.toString() : null)).toStringFormatted();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	public static void filterCause(ScanCause cause, Location location, org.bukkit.entity.Item item) {
		if (Platform.callEvent(new ItemScanEvent(cause, item)))
			new RuleCheck(cause, location).checkSingleItem(item.getItemStack());
	}

	public static void filterOpenContainer(ScanCause cause, Player player, @NonNull Inventory openContainer) {
		filter(cause, player, openContainer, Remain.invokeOpenInventoryMethod(player, "getTitle"));
	}

	public static void filterOpenContainer(ScanCause cause, Player player, @NonNull Inventory openContainer, @NonNull String inventoryTitle) {
		filter(cause, player, openContainer, inventoryTitle);
	}

	public static void filterPlayer(ScanCause cause, Player player) {
		filter(cause, player, null, null);
	}

	private static void filter(ScanCause cause, @NonNull Player player, @Nullable Inventory openContainer, @Nullable String inventoryTitle) {
		if (!player.isOnline()) {
			Debugger.debug("scan", "Ignoring disconnected player " + player.getName());

			return;
		}

		if (Settings.Ignore.OPS && player.isOp()) {
			Debugger.debug("scan", "Ignoring OP player " + player.getName());

			return;
		}

		if (player.hasPermission(Permissions.Bypass.SCAN)) {
			Debugger.debug("scan", "Ignoring player " + player.getName() + " with permission " + Permissions.Bypass.SCAN);

			return;
		}

		if (Settings.Ignore.GAMEMODES.contains(player.getGameMode())) {
			Debugger.debug("scan", "Ignoring gamemode " + player.getGameMode() + " for " + player.getName());

			return;
		}

		if (Settings.Ignore.WORLDS.contains(player.getWorld().getName())) {
			Debugger.debug("scan", "Ignoring world " + player.getWorld().getName() + " for " + player.getName());

			return;
		}

		if (McMMOHook.isUsingAbility(player)) {
			Debugger.debug("scan", "Ignoring in-progress McMMO ability for " + player.getName());

			return;
		}

		for (final String metadataKey : Settings.Ignore.METADATA_PLAYER)
			if (player.hasMetadata(metadataKey)) {
				Debugger.debug("scan", "Ignoring metadata " + metadataKey + " for " + player.getName());

				return;
			}

		if (Platform.callEvent(openContainer != null ? new InventoryScanEvent(player, cause, openContainer) : new PlayerScanEvent(player, cause)))
			new RuleCheck(cause, player).checkPlayerInventory(openContainer, inventoryTitle);
	}

	public static final class RuleCheck extends ProtectCheck<ProtectOperator> {

		/**
		 * Currently checked items
		 */
		protected FastMatcher matcher;

		protected RuleCheck(@NonNull ScanCause cause, @NonNull Location location) {
			super(null);

			this.cause = cause;
			this.location = location;
		}

		protected RuleCheck(@NonNull ScanCause cause, @Nullable Player player) {
			super(player);

			this.cause = cause;
			this.location = player.getLocation();
		}

		public void checkPlayerInventory(@Nullable Inventory extraContainer, @Nullable String inventoryTitle) {
			Valid.checkBoolean(this.hasPlayer, "Cannot check inventory without a player");

			final List<ItemStack> combinedContent = new ArrayList<>();
			boolean legacy = false;

			int invSlot = 0;

			for (final ItemStack item : this.getPlayer().getInventory().getContents()) {

				// Newest MC versions can apparently allow more than 41 slots
				if (invSlot++ >= 41)
					continue;

				combinedContent.add(item);
			}

			// Legacy Minecraft stores armor content separately
			if (combinedContent.size() == PlayerUtil.PLAYER_INV_SIZE) {
				for (final ItemStack armor : this.getPlayer().getInventory().getArmorContents())
					combinedContent.add(armor);

				combinedContent.add(null); // No offhand
				legacy = true;
			}

			Valid.checkBoolean(combinedContent.size() == InventorySnapshot.TOTAL_PLAYER_SLOTS,
					"Expected player inventory to have " + InventorySnapshot.TOTAL_PLAYER_SLOTS + " slots, got " + combinedContent.size());

			if (extraContainer != null)
				for (final ItemStack item : extraContainer.getContents())
					combinedContent.add(item);

			this.contents = combinedContent.toArray(new ItemStack[combinedContent.size()]);
			this.inventorySnapshot = Item.InventorySnapshot.fromCombinedClone(this.contents);
			this.inventoryTitle = inventoryTitle;

			Debugger.debug("scan", "Scanning " + this.getPlayer().getName() + "'s inventory");

			for (int slot = 0; slot < this.contents.length; slot++) {
				final ItemStack item = this.contents[slot];

				if (item != null) {
					try {
						this.check0(item);

					} catch (final CloneItemException | EventHandledException ex) {
						final boolean itemsModified = ex instanceof CloneItemException || ex instanceof EventHandledException && ((EventHandledException) ex).isCancelled();

						if (itemsModified) {
							final boolean isClone = ex instanceof CloneItemException;

							this.contents[slot] = isClone ? this.nbtItem.getItem() : null;

							this.setModified(true);

							// Run commands
							final String date = TimeUtil.getFormattedDateShort();
							final String location = this.hasPlayer ? SerializeUtil.serializeLocation(this.getPlayer().getLocation()) : "";

							for (final String consoleCommand : isClone ? Settings.AfterScan.CLONE_CONSOLE_COMMANDS : Settings.AfterScan.CONFISCATE_CONSOLE_COMMANDS)
								Platform.dispatchConsoleCommand(null, consoleCommand
										.replace("{player}", this.getPlayer().getName())
										.replace("{date}", date)
										.replace("{material}", this.materialName)
										.replace("{location}", location));

							if (this.hasPlayer)
								for (final String playerCommand : isClone ? Settings.AfterScan.CLONE_PLAYER_COMMANDS : Settings.AfterScan.CONFISCATE_PLAYER_COMMANDS)
									Platform.toPlayer(this.getPlayer()).dispatchCommand(playerCommand
											.replace("{date}", date)
											.replace("{material}", this.materialName)
											.replace("{location}", location));
						}
					}
				}
			}

			this.logItems();

			// Modify back
			if (this.isModified()) {
				int offset = 0;

				// Set main inventory content. In modern, this includes armor
				final ItemStack[] survivalContent = this.getPlayer().getInventory().getContents();

				for (int i = 0; i < survivalContent.length; i++) {
					survivalContent[i] = i < this.contents.length ? this.contents[i] : null;

					offset++;
				}

				this.getPlayer().getInventory().setContents(survivalContent);

				// Set armor content manually for legacy
				if (legacy) {
					final ItemStack[] armorContent = this.getPlayer().getInventory().getArmorContents();

					for (int i = 0; i < armorContent.length; i++) {
						armorContent[i] = this.contents[i + PlayerUtil.PLAYER_INV_SIZE];

						offset++;
					}

					this.getPlayer().getInventory().setArmorContents(armorContent);
				}

				// Set extra container content if present
				if (extraContainer != null) {
					final ItemStack[] extraContents = extraContainer.getContents();

					for (int i = 0; i < extraContents.length; i++)
						extraContents[i] = this.contents[offset + i];

					extraContainer.setContents(extraContents);
				}
			}
		}

		/**
		 * Check the given item
		 *
		 * @param item
		 * @throws CloneItemException
		 * @throws EventHandledException
		 */
		public void checkSingleItem(ItemStack item) throws CloneItemException, EventHandledException {
			try {
				this.check0(item);

			} finally {
				this.logItems();
			}
		}

		/**
		 * Check the given item
		 */
		private void check0(@NonNull ItemStack item) throws CloneItemException, EventHandledException {
			this.item = item;
			this.material = CompMaterial.fromItem(item);
			this.materialName = this.material.name();

			if (CompMaterial.isAir(this.materialName) || item.getAmount() == 0)
				return;

			Debugger.debug("scan", "Scanning item " + item);

			if (item.getAmount() < 0) {
				Common.warning("Removing item " + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : "") + " with negative amount: " + item.getAmount());

				throw new EventHandledException(true);
			}

			if (Settings.Ignore.MATERIALS.contains(this.material)) {
				Debugger.debug("scan", "Ignoring material '" + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : ""));

				return;
			}

			final boolean hasMeta = item.hasItemMeta();

			if (hasMeta && item.getItemMeta().hasDisplayName() && Settings.Ignore.CUSTOM_DISPLAY_NAME) {
				Debugger.debug("scan", "Ignoring item " + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : "") + " with custom display name: " + item.getItemMeta().getDisplayName() + " due to settings.yml Ignore.Custom_Display_Name set to true");

				return;
			}

			if (hasMeta && item.getItemMeta().hasLore() && Settings.Ignore.CUSTOM_LORE) {
				Debugger.debug("scan", "Ignoring item " + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : "") + " with custom lore: " + item.getItemMeta().getLore());

				return;
			}

			if (hasMeta)
				try {
					final Set<NamespacedKey> keys = item.getItemMeta().getPersistentDataContainer().getKeys();

					if (!keys.isEmpty() && Settings.Ignore.CUSTOM_PERSISTENT_TAGS) {
						Debugger.debug("scan", "Ignoring item " + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : "") + " with custom persistent tags: " + item.getItemMeta().getPersistentDataContainer().getKeys());

						return;
					}

					for (final String ignoredTag : Settings.Ignore.METADATA_ITEM)
						for (final NamespacedKey key : keys) {
							final String stringKey = key.getNamespace() + ":" + key.getKey();

							if (stringKey.equalsIgnoreCase(ignoredTag)) {
								Debugger.debug("scan", "Ignoring item " + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : "") + " with ignored persistent metadata tag: " + key);

								return;
							}
						}

				} catch (final NoSuchMethodError err) {
					// Legacy MC
				}

			try {
				if (hasMeta && item.getItemMeta().hasCustomModelData() && Settings.Ignore.CUSTOM_MODEL_DATA) {
					Debugger.debug("scan", "Ignoring item " + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : "") + " with custom model data");

					return;
				}

			} catch (final IllegalStateException | NoSuchMethodError err) {
				// Legacy MC or a Paper bug
			}

			// Put below to prevent air from being checked
			this.nbtItem = new NBTItem(item);

			if (this.nbtItem.hasTag(Tags.CLONED)) {
				Debugger.debug("scan", "Ignoring already cloned item '" + this.material + (this.hasPlayer ? "' for " + this.getPlayer().getName() : ""));

				return;
			}

			for (final Rule rule : Rules.getInstance().getRules())
				try {
					// Moved here for performance reasons
					if (rule.isDisabled())
						continue;

					this.ruleOrGroupEvaluated = rule;
					this.ruleForGroup = null;
					this.matcher = rule.getMatcher();

					this.setRuleModified(false);
					this.clearVerboseMessages();

					if (this.matcher.find(this.materialName)) {
						Debugger.debug("scan", "\tMatched rule " + rule.getUniqueName());

						if (!this.canFilter(rule))
							continue;

						if (!Platform.callEvent(new PreRuleMatchEvent(this.getPlayer(), this.item, rule))) {
							Debugger.debug("scan", "\tIgnoring as it was cancelled by PreRuleMatchEvent");

							continue;
						}

						this.verbose(rule,
								"&f*--------- Rule match " + (!rule.getName().isEmpty() ? "(" + rule.getName() + ") " : "") + "from " + this.cause + " " + (this.hasPlayer ? "for " + this.getPlayer().getName() : "") + " --------- ",
								"&fMATCH&b: &r" + rule.getMatch(),
								"&fCATCH&b: &r" + this.item.getType());

						this.executeOperators(rule);

						if (rule.getGroup() != null) {
							final Group group = rule.getGroup();
							this.ruleForGroup = rule;

							if (this.canFilter(group))
								this.executeOperators(group);

							if (group.isAbort())
								throw new OperatorAbortException();
						}

						if (rule.isAbort())
							throw new OperatorAbortException();
					}

				} catch (final OperatorAbortException ex) {
					if (this.isRuleModified())
						this.verbosePush(rule, "&cStopping further operator check.");

					break;

				} catch (final CloneItemException | EventHandledException ex) {
					throw ex; // send upstream

				} catch (final Throwable t) {
					Common.throwError(t,
							"Failed to parse rule: " + rule,
							"Error: {error}");
				}
		}

		@Override
		protected SimpleComponent replaceExtraVariables(SimpleComponent message, ProtectOperator operator) {
			message = message
					.replaceBracket("rule_fine", this.ruleOrGroupEvaluated.getFine() + "")
					.replaceBracket("item_type", this.materialName)
					.replaceBracket("item_amount", this.item.getAmount() + "")
					.replaceBracket("item_type_formatted", ChatUtil.capitalizeFully(this.materialName));

			if (this.ruleOrGroupEvaluated instanceof Rule) {
				final Rule rule = (Rule) this.ruleOrGroupEvaluated;

				message = message
						.replaceBracket("rule_name", Common.getOrDefaultStrict(rule.getName(), ""))
						.replaceBracket("rule_group", rule.getGroup() != null ? rule.getGroup().getGroupName() : "")
						.replaceBracket("rule_match", rule.getMatch());
			}

			if (this.ruleOrGroupEvaluated instanceof Group) {
				final Group group = (Group) this.ruleOrGroupEvaluated;

				message = message
						.replaceBracket("rule_group", Common.getOrDefault(group.getUniqueName(), ""))
						.replaceBracket("rule_name", Common.getOrDefault(this.ruleForGroup != null ? ((Rule) this.ruleForGroup).getName() : null, ""));
			}

			return super.replaceExtraVariables(message, operator);
		}
	}
}