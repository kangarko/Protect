package org.mineacademy.protect.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A utility class for caching. A simple and elegant solution used
 * in the offline inventory lookup and tab completion to enhance tab complete.
 *
 * Example reasoning: A bad actor just left the server before you could check his inventory,
 * so we save you time by auto-completing their name when you start typing it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemporaryStorage {

	@Getter
	private static final Set<String> playerNames = new HashSet<>();

	@Getter
	private static final Set<UUID> playerUids = new HashSet<>();

	@Getter
	private static final Set<String> pluginNames = new HashSet<>();

	@Getter
	private static final Set<String> serverNames = new HashSet<>();

	@Getter
	private static final Set<String> worldNames = new HashSet<>();

	/**
	 * Add a player name to the cache
	 *
	 * @param player
	 */
	public static void addPlayer(final Player player) {
		playerNames.add(player.getName());
		playerUids.add(player.getUniqueId());
	}

	/**
	 * Add a player name to the cache
	 *
	 * @param name
	 * @param uid
	 */
	public static void addPlayer(final String name, final UUID uid) {
		playerNames.add(name);
		playerUids.add(uid);
	}

	/**
	 * Remove a player name from the cache
	 *
	 * @param player
	 */
	public static void removePlayer(final Player player) {
		playerNames.remove(player.getName());
		playerUids.remove(player.getUniqueId());
	}

	/**
	 * Add a plugin name to the cache
	 *
	 * @param plugin
	 */
	public static void addPluginName(final String plugin) {
		pluginNames.add(plugin);
	}

	/**
	 * Add a server name to the cache
	 *
	 * @param server
	 */
	public static void addServerName(final String server) {
		serverNames.add(server);
	}

	/**
	 * Add a world name to the cache
	 *
	 * @param world
	 */
	public static void addWorldName(final String world) {
		worldNames.add(world);
	}
}
