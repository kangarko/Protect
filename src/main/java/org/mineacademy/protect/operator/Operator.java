package org.mineacademy.protect.operator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.exception.UnrecognizedRuleOperatorException;
import org.mineacademy.fo.model.BossBarMessage;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.JavaScriptExecutor;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.TitleMessage;
import org.mineacademy.fo.model.ToastMessage;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTItem;
import org.mineacademy.protect.PlayerCache;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Operator implements org.mineacademy.fo.model.Rule {

	/**
	 * Permission required for the rule to apply,
	 * message sent to player if he lacks it.
	 */
	@Nullable
	private Tuple<String, SimpleComponent> requirePermission;

	/**
	 * JavaScript boolean output required to be true for the rule to apply
	 */
	@Nullable
	private String requireScript;

	/**
	 * Gamemodes to require
	 */
	private final Set<GameMode> requireGamemodes = new HashSet<>();

	/**
	 * World names to require
	 */
	private final Set<String> requireWorlds = new HashSet<>();

	/**
	 * Region names to require
	 */
	private final Set<String> requireRegions = new HashSet<>();

	/**
	 * Map of keys and JS expressions to match certain player data to require
	 */
	private final Map<String, String> requireData = new HashMap<>();

	/**
	 * The minimum play time required for the rule to apply
	 */
	private SimpleTime requirePlayTime;

	/**
	 * Permission to bypass the rule
	 */
	@Nullable
	private String ignorePermission;

	/**
	 * JavaScript boolean output when true for the rule to bypass
	 */
	@Nullable
	private String ignoreScript;

	/**
	 * Gamemodes to ignore
	 */
	private final Set<GameMode> ignoreGamemodes = new HashSet<>();

	/**
	 * World names to ignore
	 */
	private final Set<String> ignoreWorlds = new HashSet<>();

	/**
	 * Region names to ignore
	 */
	private final Set<String> ignoreRegions = new HashSet<>();

	/**
	 * Map of keys and JS expressions to match certain player data to ignore
	 */
	private final Map<String, String> ignoreData = new HashMap<>();

	/**
	 * The limit of play time to ignore the rule
	 */
	private SimpleTime ignorePlayTime;

	/**
	 * The time since when this broadcast should run
	 */
	@Getter
	private String begins;

	/**
	 * The time in the future when this broadcast no longer runs
	 */
	@Getter
	private String expires;

	/**
	 * The delay between the next time this rule can be fired up, with optional warning message
	 */
	private Tuple<SimpleTime, SimpleComponent> delay;

	/**
	 * The delay between the next time this rule can be fired up for the given sender, with optional warning message
	 */
	private Tuple<SimpleTime, SimpleComponent> playerDelay;

	/**
	 * List of commands to run as player when rule matches
	 */
	private final List<String> playerCommands = new ArrayList<>();

	/**
	 * List of commands to run as console when rule matches
	 */
	private final List<String> consoleCommands = new ArrayList<>();

	/**
	 * List of commands to send to proxy to run when rule matches
	 */
	//private final List<String> proxyCommands = new ArrayList<>();

	/**
	 * List of messages to log
	 */
	private final List<String> consoleMessages = new ArrayList<>();

	/**
	 * Map of keys and JS expressions to match certain player data to add
	 */
	private final Map<String, String> saveData = new HashMap<>();

	/**
	 * Kick message that when set, rule will kick player
	 */
	@Nullable
	private SimpleComponent kickMessage;

	/**
	 * The message that, if set, will show as a toast notification
	 */
	@Nullable
	private ToastMessage toast;

	/**
	 * Permission:Message map to send to other players having such permission
	 */
	private final Map<String, SimpleComponent> notifyMessages = new HashMap<>();

	/**
	 * Channel:Message map to send to Discord
	 */
	private final Map<String, List<String>> discordMessages = new HashMap<>();

	/**
	 * File:Message messages to log
	 */
	private final Map<String, String> writeMessages = new HashMap<>();

	/**
	 * Map of messages to send back to player when rule matches
	 * They have unique ID assigned to prevent duplication
	 */
	private final Map<UUID, List<SimpleComponent>> warnMessages = new LinkedHashMap<>();

	/**
	 * How much money to take from player? Uses Vault.
	 */
	private double fine = 0D;

	/**
	 * Lists of sounds to send to player
	 */
	private final List<SimpleSound> sounds = new ArrayList<>();

	/**
	 * Title and subtitle to send
	 */
	@Nullable
	private TitleMessage title;

	/**
	 * The message on the action bar
	 */
	@Nullable
	private SimpleComponent actionBar;

	/**
	 * The Boss bar message
	 */
	@Nullable
	private BossBarMessage bossBar;

	/**
	 * Should we abort checking more rules below this one?
	 */
	private boolean abort = false;

	/**
	 * Should we exempt the rule from being logged?
	 */
	private boolean ignoreLogging = false;

	/**
	 * Prevent console catch information coming up?
	 */
	private boolean ignoreVerbose = false;

	/**
	 * Is this class (all operators here) temporary disabled?
	 */
	private boolean disabled;

	/*
	 * The time the operator was last executed
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private long lastExecuted = -1;

	/*
	 * The time the operator was last executed for the given player name(s)
	 */
	private final Map<String, Long> lastExecutedForPlayers = new HashMap<>();

	/**
	 * @see org.mineacademy.fo.model.Rule#onOperatorParse(java.lang.String[])
	 */
	@Override
	public boolean onOperatorParse(String[] args) {
		final String firstTwo = Common.joinRange(0, 2, args);
		final String theRestTwo = Common.joinRange(args.length >= 2 ? 2 : 1, args);

		final List<String> theRestSplit = splitVertically(theRestTwo);

		if ("require perm".equals(firstTwo) || "require permission".equals(firstTwo)) {
			this.checkNotSet(this.requirePermission, "require perm");
			final String[] split = theRestTwo.split(" ");

			this.requirePermission = new Tuple<>(split[0], split.length > 1 ? SimpleComponent.fromMiniAmpersand(Common.joinRange(1, split)) : null);
		}

		else if ("require script".equals(firstTwo)) {
			this.checkNotSet(this.requireScript, "require script");

			this.requireScript = theRestTwo;
		}

		else if ("require gamemode".equals(firstTwo) || "require gamemodes".equals(firstTwo))
			for (final String modeName : theRestSplit) {
				final GameMode gameMode = ReflectionUtil.lookupEnum(GameMode.class, modeName);

				this.requireGamemodes.add(gameMode);
			}

		else if ("require world".equals(firstTwo) || "require worlds".equals(firstTwo))
			this.requireWorlds.addAll(theRestSplit);

		else if ("require region".equals(firstTwo) || "require regions".equals(firstTwo))
			this.requireRegions.addAll(theRestSplit);

		else if ("require playtime".equals(firstTwo)) {
			this.checkStatSaving();

			this.requirePlayTime = SimpleTime.fromString(theRestTwo);

		} else if ("ignore perm".equals(firstTwo) || "ignore permission".equals(firstTwo)) {
			this.checkNotSet(this.ignorePermission, "ignore perm");

			this.ignorePermission = theRestTwo;
		}

		else if ("ignore script".equals(firstTwo)) {
			this.checkNotSet(this.ignoreScript, "ignore script");

			this.ignoreScript = theRestTwo;
		}

		else if ("ignore gamemode".equals(firstTwo) || "ignore gamemodes".equals(firstTwo))
			for (final String modeName : theRestSplit) {
				final GameMode gameMode = ReflectionUtil.lookupEnum(GameMode.class, modeName);

				this.ignoreGamemodes.add(gameMode);
			}

		else if ("ignore world".equals(firstTwo) || "ignore worlds".equals(firstTwo))
			this.ignoreWorlds.addAll(theRestSplit);

		else if ("ignore region".equals(firstTwo) || "ignore regions".equals(firstTwo))
			this.ignoreRegions.addAll(theRestSplit);

		else if ("ignore playtime".equals(firstTwo)) {
			this.checkStatSaving();

			this.ignorePlayTime = SimpleTime.fromString(theRestTwo);

		} else if ("require key".equals(firstTwo) || "ignore key".equals(firstTwo) || "save key".equals(firstTwo)) {
			final String[] split = theRestTwo.split(" ");
			Valid.checkBoolean(split.length > 0, "Wrong operator syntax! Usage: <keyName> <JavaScript condition with 'value' as the value object>");

			final String key = split[0];
			final String script = split.length > 1 ? Common.joinRange(1, split) : "";

			if ("require key".equals(firstTwo)) {
				Valid.checkBoolean(!this.requireData.containsKey(key), "The 'require key' operator already contains key: " + key);

				this.requireData.put(key, script);

			} else if ("ignore key".equals(firstTwo)) {
				Valid.checkBoolean(!this.ignoreData.containsKey(key), "The 'ignore key' operator already contains key: " + key);

				this.ignoreData.put(key, script);

			} else if ("save key".equals(firstTwo)) {
				Valid.checkBoolean(!this.saveData.containsKey(key), "The 'save key' operator already contains key: " + key);

				this.saveData.put(key, script);
			}
		}

		else if ("expires".equals(args[0])) {
			Valid.checkBoolean(this.expires == null, "Operator 'expires' already defined on " + this);
			final String date = Common.joinRange(1, args);

			this.expires = date;
		}

		else if ("begins".equals(args[0])) {
			Valid.checkBoolean(this.begins == null, "Operator 'begins' already defined on " + this);
			final String date = Common.joinRange(1, args);

			this.begins = date;
		}

		else if ("delay".equals(args[0]) || "player delay".equals(firstTwo)) {
			final int offset = "player delay".equals(firstTwo) ? 1 : 0;

			try {
				final SimpleTime time = SimpleTime.fromString(Common.joinRange(1 + offset, 3 + offset, args));
				final SimpleComponent message = args.length > 2 ? SimpleComponent.fromMiniAmpersand(Common.joinRange(3 + offset, args)) : null;

				final Tuple<SimpleTime, SimpleComponent> tuple = new Tuple<>(time, message);

				if ("delay".equals(args[0])) {
					this.checkNotSet(this.delay, args[0]);

					this.delay = tuple;

				} else {
					this.checkNotSet(this.playerDelay, firstTwo);

					this.playerDelay = tuple;
				}

			} catch (final Throwable ex) {
				Common.throwError(ex, "Syntax error in 'delay' operator. Valid: <amount> <unit> (1 second, 2 minutes). Got: " + String.join(" ", args));
			}
		}

		else if ("then command".equals(firstTwo) || "then commands".equals(firstTwo))
			this.playerCommands.addAll(theRestSplit);

		else if ("then console".equals(firstTwo))
			this.consoleCommands.addAll(theRestSplit);

		else if ("then bungeeconsole".equals(firstTwo) || "then bungee".equals(firstTwo))
			Common.warning(firstTwo + " operator has been temporarily disabled.");

		else if ("then log".equals(firstTwo))
			this.consoleMessages.addAll(theRestSplit);

		else if ("then kick".equals(firstTwo)) {
			this.checkNotSet(this.kickMessage, "then kick");

			this.kickMessage = SimpleComponent.fromMiniAmpersand(theRestTwo);
		}

		else if ("then toast".equals(firstTwo)) {
			this.checkNotSet(this.toast, "then toast");

			final String[] split = theRestTwo.split(" ");
			Valid.checkBoolean(split.length >= 3, "Invalid 'then toast' syntax. Usage: <material> <style> <message>");

			final CompMaterial icon = ReflectionUtil.lookupEnumSilent(CompMaterial.class, split[0].toUpperCase());
			final CompToastStyle style = ReflectionUtil.lookupEnumSilent(CompToastStyle.class, split[1].toUpperCase());
			final String message = Common.joinRange(2, split);

			this.toast = new ToastMessage(icon, style, message);
		}

		else if ("then notify".equals(firstTwo)) {
			final String[] split = theRestTwo.split(" ");
			Valid.checkBoolean(split.length > 1, "wrong then notify syntax! Usage: <permission> <message>");

			final String permission = split[0];
			final SimpleComponent message = SimpleComponent.fromMiniAmpersand(Common.joinRange(1, split));

			this.notifyMessages.put(permission, message);
		}

		else if ("then discord".equals(firstTwo)) {
			final String[] split = theRestTwo.split(" ");
			Valid.checkBoolean(split.length > 1, "wrong 'then discord' syntax! Use: 'then discord <channelName> <message>' (must have two words at least, found: " + split.length + " words: " + theRestTwo);

			final String channelName = split[0];
			final String message = Common.joinRange(1, split);

			final List<String> previousMessages = this.discordMessages.getOrDefault(channelName, new ArrayList<>());
			previousMessages.add(message);

			this.discordMessages.put(channelName, previousMessages);
		}

		else if ("then write".equals(firstTwo)) {
			final String[] split = theRestTwo.split(" ");
			Valid.checkBoolean(split.length > 1, "wrong 'then log' syntax! Usage: <file (without spaces)> <message>");

			final String file = split[0];
			final String message = Common.joinRange(1, split);

			this.writeMessages.put(file, message);
		}

		else if ("then fine".equals(firstTwo)) {
			Valid.checkBoolean(this.fine == 0D, "everything is fine except you specifying 'then fine' twice (dont do that) for rule: " + this);

			try {
				this.fine = Double.parseDouble(theRestTwo);

			} catch (final NumberFormatException ex) {
				Common.warning("Invalid whole number in 'then fine' (msut be a whole number, got: " + theRestTwo + ") in rule: " + this);
			}
		}

		else if ("then sound".equals(firstTwo)) {
			final SimpleSound sound = SimpleSound.fromString(theRestTwo);

			this.sounds.add(sound);
		}

		else if ("then title".equals(firstTwo)) {
			this.checkNotSet(this.title, "then title");

			final List<String> split = splitVertically(theRestTwo);
			final String title = split.get(0);
			final String subtitle = split.size() > 1 ? split.get(1) : "";
			final int fadeIn = Integer.parseInt(split.size() > 2 ? split.get(2) : "10");
			final int stay = Integer.parseInt(split.size() > 3 ? split.get(3) : "30");
			final int fadeOut = Integer.parseInt(split.size() > 4 ? split.get(4) : "10");

			this.title = new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
		}

		else if ("then actionbar".equals(firstTwo)) {
			this.checkNotSet(this.actionBar, "then actionbar");

			this.actionBar = SimpleComponent.fromMiniAmpersand(theRestTwo);
		}

		else if ("then bossbar".equals(firstTwo)) {
			this.checkNotSet(this.bossBar, "then bossbar");

			final String[] split = theRestTwo.split(" ");
			Valid.checkBoolean(split.length >= 4, "Invalid 'then bossbar' syntax. Usage: <color> <style> <secondsToShow> <message>");

			final BossBar.Color color = ReflectionUtil.lookupEnum(BossBar.Color.class, split[0]);
			final BossBar.Overlay overlay = ReflectionUtil.lookupEnum(BossBar.Overlay.class, split[1]);

			try {
				this.bossBar = new BossBarMessage(color, overlay, Integer.parseInt(split[2]), 1F, Common.joinRange(3, split));

			} catch (final NumberFormatException ex) {
				Common.warning("Invalid seconds to show in 'then bossbar', expected a whole number, got: " + split[2] + " in rule: " + this);
			}
		}

		else if ("then warn".equals(firstTwo) || "then alert".equals(firstTwo) || "then message".equals(firstTwo)) {
			this.warnMessages.put(UUID.randomUUID(), Common.convertList(splitVertically(theRestTwo), SimpleComponent::fromMiniAmpersand));

		} else if ("then abort".equals(firstTwo)) {
			Valid.checkBoolean(!this.abort, "then abort already used on " + this);

			this.abort = true;
		}

		else if ("dont log".equals(firstTwo)) {
			Valid.checkBoolean(!this.ignoreLogging, "dont log already used on " + this);

			this.ignoreLogging = true;
		}

		else if ("dont verbose".equals(firstTwo)) {
			Valid.checkBoolean(!this.ignoreVerbose, "dont verbose already used on " + this);

			this.ignoreVerbose = true;
		}

		else if ("disabled".equals(args[0])) {
			Valid.checkBoolean(!this.disabled, "'disabled' already used on " + this);

			this.disabled = true;

		} else {
			final boolean parsed = this.onParse(firstTwo, theRestTwo, args);

			if (!parsed)
				throw new UnrecognizedRuleOperatorException(args, this, "ignore".equals(args[0]) ? "ignore material " + Common.joinRange(1, args) : null);
		}

		return true;
	}

	/*
	 * Helper to check if we can use playtime operators
	 */
	private void checkStatSaving() {
		if (Remain.isStatSavingDisabled())
			throw new FoException("To use 'require/ignore playtime, you need to enable stats in spigot.yml (set stat.disable-saving to false)", false);
	}

	/**
	 * Parse the given operator, return true if parsed
	 *
	 * @param firstTwo
	 * @param theRest
	 * @param args
	 * @return
	 */
	public boolean onParse(String firstTwo, String theRest, String[] args) {
		return false;
	}

	/**
	 * Check if the value is null or complains that the operator of the given type is already defined
	 *
	 * @param value
	 * @param type
	 */
	protected final void checkNotSet(Object value, String type) {
		Valid.checkBoolean(value == null, "Operator '" + type + "' already defined on " + this);
	}

	/**
	 * A helper method to split a message by |
	 * but ignore \| and replace it with | only.
	 *
	 * @param message
	 * @return
	 */
	protected static final List<String> splitVertically(String message) {
		final List<String> split = Arrays.asList(message.split("(?<!\\\\)\\|"));

		for (int i = 0; i < split.size(); i++)
			split.set(i, split.get(i).replace("\\|", "|"));

		return split;
	}

	/**
	 * Collect all options we have to debug
	 *
	 * @return
	 */
	protected SerializedMap collectOptions() {
		return SerializedMap.fromArray(
				"Require Permission", this.requirePermission,
				"Require Script", this.requireScript,
				"Require Gamemodes", this.requireGamemodes,
				"Require Worlds", this.requireWorlds,
				"Require Regions", this.requireRegions,
				"Require Keys", this.requireData,
				"Require Playtime", this.requirePlayTime,
				"Ignore Permission", this.ignorePermission,
				"Ignore Script", this.ignoreScript,
				"Ignore Gamemodes", this.ignoreGamemodes,
				"Ignore Worlds", this.ignoreWorlds,
				"Ignore Regions", this.ignoreRegions,
				"Ignore Keys", this.ignoreData,
				"Ignore Playtime", this.ignorePlayTime,
				"Expires", this.expires,
				"Begins", this.begins,
				"Delay", this.delay != null ? this.delay.getKey() + (this.delay.getValue() != null ? ", warnmessage=" + this.delay.getValue().toMini(null) : "") : "",
				"Player Delay", this.playerDelay != null ? this.playerDelay.getKey() + (this.playerDelay.getValue() != null ? ", warnmessage=" + this.playerDelay.getValue().toMini(null) : "") : "",
				"Player Commands", this.playerCommands,
				"Console Commands", this.consoleCommands,
				//"Proxy Commands", this.proxyCommands,
				"Save Keys", this.saveData,
				"Console Messages", this.consoleMessages,
				"Kick Message", this.kickMessage,
				"Toast Message", this.toast,
				"Notify Messages", this.notifyMessages != null ? this.notifyMessages.keySet() : "",
				"Discord Message", this.discordMessages,
				"Log To File", this.writeMessages,
				"Fine", this.fine,
				"Sounds", this.sounds,
				"Title", this.title,
				"Action Bar", this.actionBar,
				"Boss Bar", this.bossBar,
				"Warn Messages", Common.convertList(this.warnMessages.values(), listOfComponents -> Common.convertList(listOfComponents, element -> element.toLegacySection(null))),
				"Abort", this.abort,
				"Ignore Logging", this.ignoreLogging,
				"Ignore Verbose", this.ignoreVerbose,
				"Disabled", this.disabled);
	}

	/*
	 * Get the time in milliseconds since the last execution for the given player
	 */
	protected final long getLastExecutedFor(CommandSender sender) {
		return this.lastExecutedForPlayers.getOrDefault(sender.getName(), -1L);
	}

	/*
	 * Set the execution time for the given player
	 */
	protected final void setLastExecutedFor(CommandSender sender) {
		this.lastExecutedForPlayers.put(sender.getName(), System.currentTimeMillis());
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof Operator) {
			final Operator other = (Operator) obj;

			return this.collectOptions().equals(other.collectOptions()) && this.getUniqueName().equals(other.getUniqueName());
		}

		return false;
	}

	@Override
	public String toString() {
		throw new RuntimeException("Please implement toString() in " + this.getClass());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	@Getter(value = AccessLevel.PROTECTED)
	public static abstract class OperatorCheck<T extends Operator> {

		@Nullable
		private final Player player;

		@Nullable
		private final FoundationPlayer audience;

		@Nullable
		private final PlayerCache cache;

		protected final boolean hasPlayer;
		protected NBTItem nbtItem;

		/**
		 * Stores sent notify messages to prevent duplication
		 */
		private final Set<String> alreadySentMessages = new HashSet<>();

		protected OperatorCheck(@Nullable Player player) {
			this.player = player;
			this.audience = player != null ? Platform.toPlayer(player) : null;
			this.cache = player != null ? PlayerCache.from(this.player) : null;
			this.hasPlayer = player != null;
		}

		/*
		 * Return true if the given operator can be applied for the given message
		 */
		protected boolean canFilter(T operator) {
			if (operator.getBegins() != null)
				if (!TimeUtil.isInTimeframe(operator.getBegins(), true)) {
					Debugger.debug("operator", "\tIgnoring due to 'begins " + operator.getBegins() + "' not started yet.");

					return false;
				}

			if (operator.getExpires() != null)
				if (!TimeUtil.isInTimeframe(operator.getExpires(), false)) {
					Debugger.debug("operator", "\tIgnoring due to 'expires " + operator.getExpires() + "' expired.");

					return false;
				}

			if (this.hasPlayer && operator.getRequirePermission() != null) {
				final String permission = operator.getRequirePermission().getKey();
				final SimpleComponent noPermissionMessage = operator.getRequirePermission().getValue();

				if (!this.player.hasPermission(permission)) {
					Debugger.debug("operator", "\tIgnoring due to missing permission: " + permission);

					if (noPermissionMessage != null) {
						Common.tell(this.player, this.replaceVariables(noPermissionMessage, operator).replaceBracket("permission", permission));

						throw new EventHandledException(false);
					}

					return false;
				}
			}

			if (operator.getRequireScript() != null) {
				Object result;

				try {
					result = JavaScriptExecutor.run(this.replaceVariablesLegacy(operator.getRequireScript(), operator), this.audience, Common.newHashMap("nbt", this.nbtItem));

				} catch (final FoScriptException ex) {
					Common.logFramed(
							"Error parsing 'require script' in rule!",
							"Rule " + operator.getUniqueName() + " in " + operator.getFile(),
							"",
							"Raw script: " + operator.getRequireScript(),
							"Evaluated script with variables replaced: '" + this.replaceVariablesLegacy(operator.getRequireScript(), operator) + "'",
							"Player: " + this.player,
							"Error: " + ex.getMessage(),
							"",
							"Check that the evaluated script",
							"above is a valid JavaScript!");

					throw ex;
				}

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "require script condition must return boolean not " + (result == null ? "null" : result.getClass()) + " for rule " + operator);

					if (!((boolean) result)) {
						Debugger.debug("operator", "\tIgnoring due to require script returning false");

						return false;
					}
				}
			}

			if (this.hasPlayer) {
				if (!operator.getRequireGamemodes().isEmpty() && !operator.getRequireGamemodes().contains(this.player.getGameMode())) {
					Debugger.debug("operator", "\tIgnoring due to required gamemodes (" + operator.getRequireGamemodes() + ") not matching player's = " + this.player.getGameMode());

					return false;
				}

				if (!operator.getRequireWorlds().isEmpty() && !Valid.isInList(this.player.getWorld().getName(), operator.getRequireWorlds())) {
					Debugger.debug("operator", "\tIgnoring due to required worlds (" + operator.getRequireWorlds() + ") not matching player's = " + this.player.getWorld().getName());

					return false;
				}

				if (!operator.getRequireRegions().isEmpty()) {
					final List<String> regions = Common.convertList(DiskRegion.findRegions(this.player.getLocation()), DiskRegion::getFileName);
					boolean found = false;

					for (final String requireRegionName : operator.getRequireRegions())
						if (regions.contains(requireRegionName)) {
							found = true;

							break;
						}

					if (!found) {
						Debugger.debug("operator", "\tIgnoring due to required regions (" + operator.getRequireRegions() + ") not matching player's = " + regions);

						return false;
					}
				}

				for (final Map.Entry<String, String> entry : operator.getRequireData().entrySet()) {
					final String key = entry.getKey();

					if (!this.cache.hasRuleData(key)) {
						Debugger.debug("operator", "\tIgnoring due to missing rule data key: " + key);

						return false;
					}

					if (entry.getValue() != null && !"".equals(entry.getValue())) {
						final String script = this.replaceVariablesLegacy(entry.getValue(), operator);

						final Object value = this.cache.getRuleData(key);
						final Object result;

						try {
							result = JavaScriptExecutor.run(script, Common.newHashMap("player", this.player, "value", value));

						} catch (final FoScriptException ex) {
							Common.logFramed(
									"Error parsing 'require key'!",
									"",
									"Operator: " + operator,
									"",
									"Evaluated script with variables replaced: '" + script + "'",
									"Player: " + this.player.getName(),
									"Error: " + ex.getMessage(),
									"",
									"Check that the evaluated script",
									"above is a valid JavaScript!");

							throw ex;
						}

						Valid.checkBoolean(result instanceof Boolean, "'require key' expected boolean, got " + result.getClass() + ": " + result + " for rule: " + this);

						if (!((boolean) result)) {
							Debugger.debug("operator", "\tIgnoring due to require data key " + key + " return false");

							return false;
						}
					}
				}

				if (operator.getRequirePlayTime() != null || operator.getIgnorePlayTime() != null) {
					final long playtimeSeconds = Remain.getPlaytimeSeconds(this.player);

					if (operator.getRequirePlayTime() != null && playtimeSeconds < operator.getRequirePlayTime().getTimeSeconds()) {
						Debugger.debug("operator", "\tIgnoring due to required playtime (" + operator.getRequirePlayTime() + ") under player's " + playtimeSeconds + " seconds");

						return false;
					}

					if (operator.getIgnorePlayTime() != null && playtimeSeconds > operator.getIgnorePlayTime().getTimeSeconds()) {
						Debugger.debug("operator", "\tIgnoring due to ignored playtime (" + operator.getIgnorePlayTime() + ") over player's " + playtimeSeconds + " seconds");

						return false;
					}
				}

				if (operator.getIgnorePermission() != null && this.player.hasPermission(operator.getIgnorePermission())) {
					Debugger.debug("operator", "\tIgnoring due to playing having ignored permission: " + operator.getIgnorePermission());

					return false;
				}
			}

			if (operator.getIgnoreScript() != null) {
				final Object result;
				final String script = this.replaceVariablesLegacy(operator.getIgnoreScript(), operator);

				try {
					result = JavaScriptExecutor.run(script, this.audience, Common.newHashMap("nbt", this.nbtItem));

				} catch (final FoScriptException ex) {
					Common.logFramed(
							"Error parsing 'ignore script' in rule!",
							"Rule " + operator.getUniqueName() + " in " + operator.getFile(),
							"",
							"Raw script: " + operator.getIgnoreScript(),
							"Evaluated script with variables replaced: '" + script + "'",
							"Player: " + this.player,
							"Error: " + ex.getMessage(),
							"",
							"Check that the evaluated script",
							"above is a valid JavaScript!");

					throw ex;
				}

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "ignore script condition must return boolean not " + (result == null ? "null" : result.getClass()) + " for rule " + operator);

					if (((boolean) result)) {
						Debugger.debug("operator", "\tIgnoring due to ignore script returning true");

						return false;
					}
				}
			}

			if (this.hasPlayer) {
				if (operator.getIgnoreGamemodes().contains(this.player.getGameMode())) {
					Debugger.debug("operator", "\tIgnoring due to ignored gamemodes (" + operator.getIgnoreGamemodes() + ") matching player's = " + this.player.getGameMode());

					return false;
				}

				if (operator.getIgnoreWorlds().contains(this.player.getWorld().getName())) {
					Debugger.debug("operator", "\tIgnoring due to ignored worlds (" + operator.getIgnoreWorlds() + ") matching player's = " + this.player.getWorld().getName());

					return false;
				}

				for (final String playersRegion : Common.convertList(DiskRegion.findRegions(this.player.getLocation()), DiskRegion::getFileName))
					if (operator.getIgnoreRegions().contains(playersRegion)) {
						Debugger.debug("operator", "\tIgnoring due to ignored regions (" + operator.getIgnoreRegions() + ") matching player's = " + playersRegion);

						return false;
					}

				for (final Map.Entry<String, String> entry : operator.getIgnoreData().entrySet()) {
					final String key = entry.getKey();
					final Object value = this.cache.getRuleData(key);

					if ((entry.getValue() == null || "".equals(entry.getValue())) && value != null) {
						Debugger.debug("operator", "\tIgnoring due to ignore data key " + key + " not null");

						return false;
					}

					final String script = this.replaceVariablesLegacy(entry.getValue(), operator);

					if (value != null) {
						final Object result;

						try {
							result = JavaScriptExecutor.run(script, Common.newHashMap("player", this.player, "value", value));

						} catch (final FoScriptException ex) {
							Common.logFramed(
									"Error parsing 'ignore key'!",
									"",
									"Operator: " + operator,
									"",
									"Evaluated script with variables replaced: '" + script + "'",
									"Player: " + this.player.getName(),
									"Error: " + ex.getMessage(),
									"",
									"Check that the evaluated script",
									"above is a valid JavaScript!");

							throw ex;
						}

						Valid.checkBoolean(result instanceof Boolean, "'ignore key' expected boolean, got " + result.getClass() + ": " + result + " for rule: " + this);

						if (((boolean) result)) {
							Debugger.debug("operator", "\tIgnoring due to ignore data key " + key + " returning true");

							return false;
						}
					}
				}
			}

			return true;
		}

		/*
		 * Run given operators for the given message and return the updated message
		 */
		protected final void executeOperators(T operator) throws EventHandledException {
			// Delay
			if (operator.getDelay() != null) {
				final SimpleTime time = operator.getDelay().getKey();
				final SimpleComponent warnComponent = operator.getDelay().getValue();

				final long now = System.currentTimeMillis();

				// Round the number due to Bukkit scheduler lags
				final long delay = Math.round((now - operator.getLastExecuted()) / 1000D);

				if (delay < time.getTimeSeconds()) {
					Debugger.debug("operator", "\tbefore delay: " + delay + " threshold: " + time.getTimeSeconds());

					this.cancel(false, warnComponent == null ? null : this.replaceVariables(warnComponent, operator).replaceBracket("delay", "" + (time.getTimeSeconds() - delay)));
				}

				operator.setLastExecuted(now);
			}

			if (this.hasPlayer) {

				// Player Delay
				if (operator.getPlayerDelay() != null) {
					final SimpleTime time = operator.getPlayerDelay().getKey();
					final SimpleComponent warnComponent = operator.getPlayerDelay().getValue();

					final long now = System.currentTimeMillis();

					// Round the number due to Bukkit scheduler lags
					final long delay = Math.round((now - operator.getLastExecutedFor(this.player)) / 1000D);

					if (delay < time.getTimeSeconds()) {
						Debugger.debug("operator", "\tbefore player delay: " + delay + " threshold: " + time.getTimeSeconds());

						this.cancel(false, warnComponent == null ? null
								: this.replaceVariables(warnComponent
										.replaceBracket("delay", time.getTimeSeconds() - delay + "")
										.replaceBracket("player_delay", time.getTimeSeconds() - delay + ""), operator));
					}

					operator.setLastExecutedFor(this.player);
				}

				for (final String command : operator.getPlayerCommands())
					this.audience.dispatchCommand(this.replaceVariablesLegacy(command, operator));
			}

			for (final String command : operator.getConsoleCommands())
				Platform.dispatchConsoleCommand(null, this.replaceVariablesLegacy(command, operator).replace("{player}", this.hasPlayer ? this.audience.getName() : ""));

			for (final String message : operator.getConsoleMessages())
				Common.log(this.replaceVariablesLegacy(message, operator));

			for (final Map.Entry<String, SimpleComponent> entry : operator.getNotifyMessages().entrySet()) {
				final String permission = entry.getKey();
				final SimpleComponent notifyComponent = this.replaceVariables(entry.getValue(), operator);
				final String plain = notifyComponent.toPlain(null);

				if (!this.alreadySentMessages.contains(plain)) {
					this.alreadySentMessages.add(plain);

					for (final FoundationPlayer online : Platform.getOnlinePlayers())
						if (online.hasPermission(permission))
							Messenger.warn(online, notifyComponent);
				}
			}

			if (HookManager.isDiscordSRVLoaded())
				for (final Entry<String, List<String>> entry : operator.getDiscordMessages().entrySet()) {
					final String channelName = entry.getKey();
					final List<String> discordMessages = entry.getValue();

					for (final String discordMessage : discordMessages)
						HookManager.sendDiscordMessage(channelName, this.replaceVariablesLegacy(discordMessage, operator));
				}

			for (final Map.Entry<String, String> entry : operator.getWriteMessages().entrySet()) {
				final String file = this.replaceVariablesLegacy(entry.getKey(), operator);
				final String message = this.replaceVariablesLegacy(entry.getValue(), operator);

				// Run async for best performance
				Platform.runTaskAsync(() -> FileUtil.writeFormatted(file, message));
			}

			if (this.hasPlayer) {
				if (operator.getFine() > 0)
					HookManager.withdraw(this.player, operator.getFine());

				for (final SimpleSound sound : operator.getSounds())
					sound.play(this.player);

				if (operator.getToast() != null)
					operator.getToast().displayTo(this.player, toastMessage -> this.replaceVariables(SimpleComponent.fromMiniAmpersand(toastMessage), operator).toLegacySection(null));

				if (operator.getTitle() != null)
					operator.getTitle().displayTo(this.audience, messageToReplace -> this.replaceVariables(messageToReplace, operator));

				if (operator.getActionBar() != null)
					this.audience.sendActionBar(this.replaceVariables(operator.getActionBar(), operator));

				if (operator.getBossBar() != null)
					operator.getBossBar().displayTo(this.audience, bossMessage -> this.replaceVariables(bossMessage, operator));

				for (final Map.Entry<String, String> entry : operator.getSaveData().entrySet()) {
					final String key = entry.getKey();
					final String script = this.replaceVariables(SimpleComponent.fromMiniAmpersand(entry.getValue()), operator).toLegacySection(null);
					final Object result;

					try {
						result = script.trim().isEmpty() ? null : JavaScriptExecutor.run(script, Common.newHashMap("player", this.player));

						Platform.runTask(() -> this.cache.setRuleData(key, result));

					} catch (final FoScriptException ex) {
						Common.logFramed(
								"Error saving data in operator!",
								"",
								"Operator: " + operator,
								"",
								"Evaluated script with variables replaced: '" + script + "'",
								"Player: " + this.player.getName(),
								"Error: " + ex.getMessage(),
								"",
								"Check that the evaluated script",
								"above is a valid JavaScript!");

						throw ex;
					}
				}

				if (operator.getKickMessage() != null)
					this.audience.kick(this.replaceVariables(operator.getKickMessage(), operator));

				// Dirty: Run later including when EventHandledException is thrown
				Platform.runTask(1, () -> {
					if (!operator.getWarnMessages().isEmpty())
						for (final Entry<UUID, List<SimpleComponent>> entry : operator.getWarnMessages().entrySet()) {
							final UUID uniqueId = entry.getKey();
							final SimpleComponent warnMessage = this.replaceVariables(RandomUtil.nextItem(entry.getValue()), operator);

							final long now = System.currentTimeMillis();
							final long lastTimeShown = this.cache.getRecentWarningMessages().getOrDefault(uniqueId, -1L);

							// Prevent duplicate messages in the last 0.5 seconds
							if (lastTimeShown == -1L || now - lastTimeShown > 500) {
								Common.tell(this.player, warnMessage);

								this.cache.getRecentWarningMessages().put(uniqueId, now);
							}
						}
				});
			}

			this.executeExtraOperators(operator);

			if (operator.isAbort())
				throw new OperatorAbortException();
		}

		protected abstract void executeExtraOperators(T operator);

		/*
		 * Replace variables in the given message
		 */
		private String replaceVariablesLegacy(String message, T operator) {
			return this.replaceVariables(SimpleComponent.fromMiniAmpersand(message), operator).toLegacySection(null);
		}

		/*
		 * Replace variables in the given message
		 */
		private SimpleComponent replaceVariables(SimpleComponent message, T operator) {
			final Variables variables = Variables.builder(this.audience);

			if (this.hasPlayer)
				for (final Map.Entry<String, Object> data : this.cache.getRuleData().entrySet())
					variables.placeholder("data_" + data.getKey(), SerializeUtil.serialize(Language.YAML, data.getValue()).toString());

			return variables.replaceComponent(this.replaceExtraVariables(message, operator));
		}

		protected SimpleComponent replaceExtraVariables(SimpleComponent message, T operator) {
			return message;
		}

		/*
		 * Cancels the pipeline by throwing a {@link EventHandledException}
		 * and send an error message to the player
		 */
		private void cancel(boolean takenItems, @Nullable SimpleComponent errorMessage) {
			if (errorMessage != null && !errorMessage.toPlain().isEmpty() && this.hasPlayer)
				Messenger.error(this.player, Variables.builder(this.audience).replaceComponent(errorMessage));

			throw new EventHandledException(takenItems);
		}
	}

	/**
	 * Represents an indication that further rule processing should be aborted
	 */
	@Getter
	@RequiredArgsConstructor
	public final static class OperatorAbortException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
