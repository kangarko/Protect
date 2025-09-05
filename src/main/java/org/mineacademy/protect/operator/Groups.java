package org.mineacademy.protect.operator;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mineacademy.fo.model.RuleSetReader;

import lombok.Getter;

/**
 * Represents a way to store/load rule groups
 */
public final class Groups extends RuleSetReader<Group> {

	@Getter
	private static final Groups instance = new Groups();

	/**
	 * The loaded rule groups sorted by name
	 */
	private final Map<String, Group> groups = new HashMap<>();

	private Groups() {
		super("group");
	}

	/**
	 * Reloads rules and handlers.
	 */
	@Override
	public void load() {
		this.groups.clear();

		for (final Group group : this.loadFromFile("rules/group.rs")) {
			//Common.log("Loading group " + group.getUniqueName());

			this.groups.put(group.getUniqueName(), group);
		}
	}

	/**
	 * Return group by name, or null if such group not exist
	 *
	 * @param groupName
	 * @return
	 */
	public Group findGroup(String groupName) {
		return this.groups.get(groupName);
	}

	/**
	 * Return group name list
	 *
	 * @return
	 */
	public Set<String> getGroupNames() {
		return Collections.unmodifiableSet(this.groups.keySet());
	}

	/**
	 * @see org.mineacademy.fo.model.RuleSetReader#createRule(java.io.File, java.lang.String)
	 */
	@Override
	protected Group createRule(File file, String name) {
		return new Group(file, name);
	}
}
