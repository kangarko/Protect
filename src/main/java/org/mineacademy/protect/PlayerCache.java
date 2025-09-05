package org.mineacademy.protect;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.Getter;
import lombok.Setter;

/**
 * Stores temporary and permanent player information
 */
@Getter
public final class PlayerCache {

	/**
	 * The player cache map caching data for players online.
	 */
	private static Map<UUID, PlayerCache> cacheMap = new HashMap<>();

	/**
	 * This instance's player's unique id
	 */
	private final UUID uniqueId;

	/**
	 * Data stored from rules
	 */
	private final Map<String, Object> ruleData = new HashMap<>();

	/**
	 * Recent warning messages the sender has received
	 * Used to prevent duplicate warning messages
	 */
	@Getter
	private final Map<UUID, Long> recentWarningMessages = new HashMap<>();

	/**
	 * Is the currently being performed WorldEdit edit session blocked?
	 */
	@Getter
	@Setter
	private boolean editSessionBlocked = false;

	/**
	 * The timestamp when last WorldEdit edit session violation happened
	 */
	@Getter
	@Setter
	private long lastWorldEditViolation = 0;

	/**
	 * Total blocks placed in the last WorldEdit edit session
	 */
	@Getter
	private long totalBlocksPlaced = 0;

	/**
	 * Stores blocks placed in the last WorldEdit edit session by material name (as lowercase string, for performance reasons)
	 */
	@Getter
	private final Map<String, Integer> blocksPlaced = new HashMap<>();

	/**
	 * Represents a region the player is currently creating
	 */
	@Getter
	@Setter
	private VisualizedRegion createdRegion = new VisualizedRegion();

	private PlayerCache(final UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	/* ------------------------------------------------------------------------------- */
	/* Data-related methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return true if player has rule data
	 *
	 * @param key
	 * @return
	 */
	public boolean hasRuleData(String key) {
		return this.ruleData.containsKey(key);
	}

	/**
	 * Get rule data
	 *
	 * @param key
	 * @return
	 */
	public Object getRuleData(String key) {
		return this.ruleData.get(key);
	}

	/**
	 * Save the given rule data pair
	 *
	 * @param key
	 * @param object
	 */
	public void setRuleData(String key, @Nullable Object object) {

		if (object == null || object.toString().trim().equals("") || object.toString().equalsIgnoreCase("null"))
			this.ruleData.remove(key);

		else
			this.ruleData.put(key, object);
	}

	public void resetEditSession() {
		this.editSessionBlocked = false;
		this.totalBlocksPlaced = 0;
		this.blocksPlaced.clear();
	}

	public void increaseTotalBlocksPlaced() {
		this.totalBlocksPlaced++;
	}

	public int getBlocksPlaced(final String materialName) {
		return this.blocksPlaced.getOrDefault(materialName, 0);
	}

	public void setBlocksPlaced(final String materialName, final int blocksPlaced) {
		this.blocksPlaced.put(materialName, blocksPlaced);
	}

	public boolean hasBlocksPlaced() {
		return !this.blocksPlaced.isEmpty();
	}

	/* ------------------------------------------------------------------------------- */
	/* Misc methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Compare if two caches are equal on the basis of {@link UUID} since it is unique.
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof PlayerCache && ((PlayerCache) obj).getUniqueId().equals(this.uniqueId);
	}

	/**
	 * Generate unique hash code from {@link UUID}.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.uniqueId);
	}

	@Override
	public String toString() {
		return "PlayerCache{" + this.uniqueId + "}";
	}

	/* ------------------------------------------------------------------------------- */
	/* Static access */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return or create new player cache for the given player
	 * @param player
	 *
	 * @return
	 */
	public static PlayerCache from(final Player player) {
		final UUID uniqueId = player.getUniqueId();
		PlayerCache cache = cacheMap.get(uniqueId);

		if (cache == null) {
			cache = new PlayerCache(uniqueId);

			cacheMap.put(uniqueId, cache);
		}

		return cache;
	}

	/**
	 * Clear the entire cache map
	 */
	public static void clearCaches() {
		cacheMap.clear();
	}
}
