package org.mineacademy.protect.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ConfigSerializable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * An extremely limited and simple matcher modeled off of regex, intentionally culled down for
 * maximum performance and efficiency.
 *
 * It matches items in four different modes:
 * 1. Start your pattern with * and we will evaluate if the message starts with it,
 * 2. End it with * and we'll evaluate endings,
 * 3. Start with " and end with " to evaluate equal
 * 4. Otherwise we evaluate if message contains the pattern
 *
 * You can use | to separate multiple matches i.e. DIAMOND_*|GOLDEN_* etc
 * You can use '*'  to match everything
 * You can use #tag to match Minecraft item/block tags i.e. #swords, #enchantable/armor, #anvil
 * You can combine tags with materials i.e. #swords|TRIDENT
 *
 * If you still want/need to use regex you can prefix your message with "* " and then match
 * normally, i.e. "* ^DIAMOND_(SWORD|HOE)"
 *
 * Example: DIAMOND_* will match all DIAMOND_HOE, DIAMOND_SPADE etc. but not SUPERDIAMOND_SPADE
 *
 * Rationale: The Protect plugin evaluates each slot in the inventory (27 + armor) against all rules,
 * and using the complex regex class drags performance down too much.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FastMatcher implements ConfigSerializable {

	/**
	 * The full regex class
	 */
	private final Pattern pattern;

	/**
	 * The raw pattern
	 */
	private final String rawPattern;

	/**
	 * The list of matcher, null if pattern is *
	 */
	private final Matcher[] matchers;

	/**
	 * Material names resolved from Minecraft tags (#tag syntax), null if no tags used
	 */
	private final Set<String> taggedMaterials;

	/**
	 * Return if this matcher matches the given message,
	 * case sensitive
	 *
	 * @param message
	 * @return
	 */
	public boolean find(String message) {

		// Check Minecraft tag membership (resolved at compile time for performance)
		if (this.taggedMaterials != null && this.taggedMaterials.contains(message))
			return true;

		// Indicates we match everything
		if (this.matchers == null)
			return true;

		if (message.isEmpty())
			return false;

		// Indicate regex is used
		if (this.pattern != null)
			return this.pattern.matcher(message).find();

		// Use our matching
		for (final Matcher matcher : this.matchers) {
			ValidCore.checkNotEmpty(matcher.getPattern(), "Matcher pattern cannot be empty! Use * instead to match everything in " + this);

			if (matcher.find(message))
				return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return "FastMatcher{pattern=" + rawPattern + "}";
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.fromArray("Pattern", this.rawPattern);
	}

	/**
	 * Deserialize the matcher
	 *
	 * @param map
	 * @return
	 */
	public static FastMatcher deserialize(SerializedMap map) {
		return compile(map.getString("Pattern"));
	}

	/**
	 * Compiles a matcher from the given pattern
	 *
	 * @param pattern
	 * @return
	 */
	public static FastMatcher compile(String pattern) {
		if ("*".equals(pattern))
			return new FastMatcher(null, pattern, null, null);

		else if (pattern.startsWith("* "))
			return new FastMatcher(Pattern.compile(pattern.substring(2)), pattern, null, null);

		final List<Matcher> matchers = new ArrayList<>();
		Set<String> taggedMaterials = null;

		for (final String part : pattern.split("\\|"))

			if (part.startsWith("#")) {
				final String tagName = part.substring(1);
				final Tag<Material> tag = lookupTag(tagName);

				if (tag == null)
					throw new FoException("Unknown Minecraft tag '#" + tagName + "' in match pattern '" + pattern + "'. "
							+ "Use vanilla tag names such as 'swords', 'anvil', 'enchantable/armor'. "
							+ "See https://minecraft.wiki/w/Tag for a list of all tags.", false);

				if (taggedMaterials == null)
					taggedMaterials = new HashSet<>();

				for (final Material material : tag.getValues())
					taggedMaterials.add(material.name());

			} else
				matchers.add(Matcher.compile(part));

		return new FastMatcher(null, pattern, matchers.toArray(new Matcher[matchers.size()]), taggedMaterials);
	}

	/**
	 * Look up a Minecraft tag by name, trying items registry first, then blocks.
	 */
	private static Tag<Material> lookupTag(String tagName) {
		if (tagName.isEmpty())
			throw new FoException("Empty Minecraft tag name '#' in match pattern. Specify a tag name such as '#swords' or '#enchantable/armor'.", false);

		try {
			final NamespacedKey key = NamespacedKey.minecraft(tagName.toLowerCase());

			Tag<Material> tag = Bukkit.getTag("items", key, Material.class);

			if (tag == null)
				tag = Bukkit.getTag("blocks", key, Material.class);

			return tag;

		} catch (final NoSuchMethodError ex) {
			throw new FoException("Minecraft tag matching (#tag) requires Minecraft 1.13 or newer.", false);
		}
	}

	/**
	 * Compile a list of matchers from the given list
	 *
	 * @param list
	 * @return
	 */
	public static List<FastMatcher> compileFromList(List<String> list) {
		final List<FastMatcher> matchers = new ArrayList<>();

		for (final String pattern : list)
			matchers.add(compile(pattern));

		return matchers;
	}
}

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
class Matcher {

	/**
	 * The pattern, adjusted for mode, see above
	 */
	private final String pattern;

	/**
	 * The matching mode
	 */
	private final int mode;

	/**
	 * Return true if the pattern matches the given message
	 * @param message
	 * @return
	 */
	public boolean find(String message) {
		if (this.mode == 1)
			return message.endsWith(this.pattern);

		else if (this.mode == 2)
			return message.startsWith(this.pattern);

		else if (this.mode == 3)
			return message.equals(this.pattern);

		else
			return message.contains(this.pattern);
	}

	/**
	 * Compiles a matcher from the given pattern, case sensitive.
	 *
	 * @param pattern
	 * @return
	 */
	public static Matcher compile(String pattern) {
		int mode = 4;

		if (pattern.charAt(0) == '*') {
			mode = 1;
			pattern = pattern.substring(1);

		} else if (pattern.endsWith("*")) {
			mode = 2;
			pattern = pattern.substring(0, pattern.length() - 1);

		} else if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
			mode = 3;
			pattern = pattern.substring(1, pattern.length() - 1);
		}

		return new Matcher(pattern, mode);
	}
}