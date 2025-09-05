package org.mineacademy.protect.settings;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.DatabaseType;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.protect.model.FastMatcher;
import org.mineacademy.protect.model.Permissions;

/**
 * The main settings.yml configuration file.
 */
@SuppressWarnings("unused")
public final class Settings extends SimpleSettings {

	@Override
	protected List<String> getUncommentedSections() {
		return Arrays.asList("WorldEdit.Total_Limit");
	}

	public static class Scan {
		public static Boolean PLAYER_JOIN;
		public static Boolean PLAYER_DEATH;
		public static Boolean WORLD_CHANGE;
		public static Boolean INVENTORY_OPEN;
		public static List<FastMatcher> ITEM_USE;
		public static Boolean ITEM_SPAWN;
		public static IsInList<String> COMMAND;
		public static SimpleTime PERIODIC;

		private static void init() {
			setPathPrefix("Scan");

			PLAYER_JOIN = getBoolean("Player_Join");
			PLAYER_DEATH = getBoolean("Player_Death");
			WORLD_CHANGE = getBoolean("World_Change");
			INVENTORY_OPEN = getBoolean("Inventory_Open");
			ITEM_USE = FastMatcher.compileFromList(getStringList("Item_Use"));
			ITEM_SPAWN = getBoolean("Item_Spawn");
			COMMAND = getIsInList("Command", String.class);
			PERIODIC = getTime("Periodic");
		}
	}

	public static class Ignore {
		public static Boolean OPS;
		public static IsInList<GameMode> GAMEMODES;
		public static IsInList<String> WORLDS;
		public static List<String> METADATA_PLAYER;
		public static List<String> METADATA_ITEM;
		public static Boolean CUSTOM_DISPLAY_NAME;
		public static Boolean CUSTOM_LORE;
		public static Boolean CUSTOM_PERSISTENT_TAGS;
		public static Boolean CUSTOM_MODEL_DATA;
		public static IsInList<CompMaterial> MATERIALS;
		public static IsInList<InventoryType> INVENTORY_TYPES;
		public static IsInList<String> INVENTORY_TITLES;

		private static void init() {
			setPathPrefix("Ignore");

			OPS = getBoolean("Ops");
			GAMEMODES = getIsInList("Gamemodes", GameMode.class);
			WORLDS = getIsInList("Worlds", String.class);
			METADATA_PLAYER = getStringList("Metadata_Player");
			METADATA_ITEM = getStringList("Metadata_Item");
			CUSTOM_DISPLAY_NAME = getBoolean("Custom_Display_Name");
			CUSTOM_LORE = getBoolean("Custom_Lore");
			CUSTOM_PERSISTENT_TAGS = getBoolean("Custom_Persistent_Tags");
			CUSTOM_MODEL_DATA = getBoolean("Custom_Model_Data");
			MATERIALS = getIsInList("Materials", CompMaterial.class);
			INVENTORY_TYPES = getIsInList("Inventory_Types", InventoryType.class);
			INVENTORY_TITLES = getIsInList("Inventory_Titles", String.class);

			for (final String worldName : WORLDS)
				if (Bukkit.getWorld(worldName) == null)
					throw new FoException("Found invalid world '" + worldName + "' in Ignore.Worlds", false);
		}
	}

	public static class AfterScan {
		public static List<String> CONFISCATE_CONSOLE_COMMANDS;
		public static List<String> CONFISCATE_PLAYER_COMMANDS;
		public static List<String> CLONE_CONSOLE_COMMANDS;
		public static List<String> CLONE_PLAYER_COMMANDS;

		private static void init() {
			setPathPrefix("After_Scan");

			CONFISCATE_CONSOLE_COMMANDS = getStringList("Confiscate_Console_Commands");
			CONFISCATE_PLAYER_COMMANDS = getStringList("Confiscate_Player_Commands");
			CLONE_CONSOLE_COMMANDS = getStringList("Clone_Console_Commands");
			CLONE_PLAYER_COMMANDS = getStringList("Clone_Player_Commands");
		}
	}

	public static class Rules {

		public static Boolean VERBOSE;
		public static Boolean BROADCAST;
		public static String BROADCAST_FORMAT;
		public static String DISCORD_CHANNEL;
		public static String DISCORD_FORMAT;

		private static void init() {
			setPathPrefix("Rules");

			VERBOSE = getBoolean("Verbose");
			BROADCAST = getBoolean("Broadcast");
			BROADCAST_FORMAT = getString("Broadcast_Format");
			DISCORD_CHANNEL = getString("Discord_Channel");
			DISCORD_FORMAT = getString("Discord_Format");
		}
	}

	public static class CommandLog {
		public static Boolean ENABLED;
		public static Boolean BLOCK;
		public static String BLOCK_FALLBACK_MESSAGE;
		public static Boolean BROADCAST;
		public static String BROADCAST_FORMAT;
		public static String DISCORD_CHANNEL;
		public static String DISCORD_FORMAT;
		public static IsInList<String> COMMAND_LIST;

		private static void init() {
			setPathPrefix("Command_Log");

			ENABLED = getBoolean("Enabled");
			BLOCK = getBoolean("Block");
			BLOCK_FALLBACK_MESSAGE = getString("Block_Fallback_Message");
			BROADCAST = getBoolean("Broadcast");
			BROADCAST_FORMAT = getString("Broadcast_Format");
			DISCORD_CHANNEL = getString("Discord_Channel");
			DISCORD_FORMAT = getString("Discord_Format");
			COMMAND_LIST = getIsInList("Command_List", String.class);
		}
	}

	public static class TransactionLog {

		public static Boolean ENABLED;
		public static Boolean BROADCAST;
		public static String BROADCAST_FORMAT_BUY;
		public static String BROADCAST_FORMAT_SELL;
		public static String DISCORD_CHANNEL;
		public static String DISCORD_FORMAT_BUY;
		public static String DISCORD_FORMAT_SELL;

		private static void init() {
			setPathPrefix("Transaction_Log");

			ENABLED = getBoolean("Enabled");
			BROADCAST = getBoolean("Broadcast");
			BROADCAST_FORMAT_BUY = getString("Broadcast_Format_Buy");
			BROADCAST_FORMAT_SELL = getString("Broadcast_Format_Sell");
			DISCORD_CHANNEL = getString("Discord_Channel");
			DISCORD_FORMAT_BUY = getString("Discord_Format_Buy");
			DISCORD_FORMAT_SELL = getString("Discord_Format_Sell");
		}
	}

	public static class WorldEdit {
		public static Boolean ENABLED;
		public static Map<String, Integer> TOTAL_GROUP_LIMIT;
		public static Map<String, Integer> BLOCK_LIMIT; // block ID, lowercase for performance reasons - max limit or 0 to block alltogether
		public static SimpleTime WAIT_THRESHOLD;

		private static void init() {
			setPathPrefix("WorldEdit");

			ENABLED = getBoolean("Enabled");
			TOTAL_GROUP_LIMIT = Common.sortByValue(getMap("Total_Limit", String.class, Integer.class));
			BLOCK_LIMIT = getBlockLimit("Block_Limit");
			WAIT_THRESHOLD = getTime("Wait_Threshold");
		}

		public static int getGroupLimit(final Player player) {
			for (final Entry<String, Integer> entry : Settings.WorldEdit.TOTAL_GROUP_LIMIT.entrySet())
				if (player.hasPermission(Permissions.GROUP.replace("{group_name}", entry.getKey())))
					return entry.getValue();

			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Settings for MySQL
	 *
	 * For security reasons, no sensitive information is stored here.
	 */
	public static class Database {

		public static DatabaseType TYPE;

		private static void init() {
			final File databaseYml = FileUtil.extract("database.yml");
			final YamlConfig databaseConfig = YamlConfig.fromFile(databaseYml);

			databaseConfig.setDefaults(YamlConfig.fromInternalPath("database.yml"));

			setPathPrefix("Database");

			boolean save = false;

			if (isSet("Type")) {
				databaseConfig.set("Type", get("Type", DatabaseType.class));

				save = true;
			}

			if (isSet("Host"))
				databaseConfig.set("Host", getString("Host"));

			if (isSet("Database"))
				databaseConfig.set("Database", getString("Database"));

			if (isSet("User"))
				databaseConfig.set("User", getString("User"));

			if (isSet("Password"))
				databaseConfig.set("Password", getString("Password"));

			if (isSet("Line"))
				databaseConfig.set("Line", getString("Line"));

			if (save) {
				Common.log("Migrated 'Database' section from settings.yml to database.yml. Please check.");

				databaseConfig.save();
			}

			TYPE = databaseConfig.get("Type", DatabaseType.class);
			final String HOST = databaseConfig.getString("Host");
			final String DATABASE = databaseConfig.getString("Database");
			final String USER = databaseConfig.getString("User");
			final String PASSWORD = databaseConfig.getString("Password");
			final String LINE = databaseConfig.getString("Line");

			boolean remoteFailed = false;

			if (TYPE == DatabaseType.REMOTE) {
				if (!Platform.hasCustomServerName() || Platform.getCustomServerName().equals("server"))
					CommonCore.logFramed(true,
							"&fERROR: &cRemote database requires the",
							"Server_Name key in proxy.yml to be set!");
				else {
					CommonCore.log("", "Connecting to remote " + TYPE.getDriver() + " database...");
					final String address = LINE.replace("{driver}", TYPE.getDriver()).replace("{host}", HOST).replace("{database}", DATABASE);

					try {
						org.mineacademy.protect.model.db.Database.getInstance().connect(address, USER, PASSWORD);

					} catch (final Throwable t) {
						if (t instanceof SQLException && t.getMessage() != null && t.getMessage().contains("invalid database address")) {
							Common.warning("Invalid database address: " + address + ", falling back to local.");

							t.printStackTrace();

						} else
							CommonCore.error(t, "Error connecting to remote database, falling back to local.");

						remoteFailed = true;
					}
				}
			}

			if (TYPE == DatabaseType.LOCAL || remoteFailed)
				org.mineacademy.protect.model.db.Database.getInstance().connect("jdbc:sqlite:" + FileUtil.getFile("sqlite.db").getPath());
		}

		public static boolean isRemote() {
			return TYPE.isRemote();
		}
	}

	public static SimpleTime REMOVE_ENTRIES_OLDER_THAN;

	private static void init() {
		setPathPrefix(null);

		REMOVE_ENTRIES_OLDER_THAN = getTime("Remove_Entries_Older_Than");

		Common.log("Using timezone " + TIMEZONE + " for logging entries.");
	}

	// ------------------------------------------------------------------------------------------
	// Private helper methods
	// ------------------------------------------------------------------------------------------

	private static Map<String, Integer> getBlockLimit(final String path) {
		final Map<String, Integer> blockList = new HashMap<>();

		for (final String line : getStringList(path)) {
			String materialName;
			Integer maxAmount = 0;

			if (line.contains("-")) {
				final String[] parts = line.split("-");
				Valid.checkBoolean(parts.length == 2, "Invalid block limit syntax. Use '<material>-<limit>' or '<material>'. Line: " + line);

				materialName = parts[0];

				try {
					maxAmount = Integer.parseInt(parts[1]);

				} catch (final NumberFormatException ex) {
					Common.error(ex, "Invalid block limit syntax. Use '<material>-<limit>' where limit must be a number. Line: " + line);
				}

				Valid.checkBoolean(maxAmount >= 0, "Invalid block limit syntax. Use '<material>-<limit>' where limit must be equals or greater than 0. Line: " + line);

			} else
				materialName = line;

			final CompMaterial material = CompMaterial.fromString(materialName);

			if (material == null)
				Common.logFramed(true,
						"Invalid material " + materialName + " in " + path + ". Check your syntax. Valid syntax: '<material>-<limit>' or '<material>'.",
						"Line: " + line);
			else
				blockList.put(material.toString().toLowerCase(), maxAmount);
		}

		return blockList;
	}
}