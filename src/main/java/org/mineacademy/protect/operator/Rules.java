package org.mineacademy.protect.operator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.model.RuleSetReader;

import lombok.Getter;

/**
 * Represents the core engine for rules
 */
@Getter
public final class Rules extends RuleSetReader<Rule> {

	/**
	 * The singleton instance of this class
	 */
	@Getter
	private static final Rules instance = new Rules();

	/**
	 * The rules list
	 */
	private final List<Rule> rules = new ArrayList<>();

	private Rules() {
		super("match");
	}

	/**
	 * Reloads rules and handlers.
	 */
	@Override
	public void load() {
		this.rules.clear();

		for (final File file : FileUtil.getFiles("rules", ".rs")) {
			if (file.getName().equals("group.rs"))
				continue;

			Common.log("Loading rules from " + file.getName());
			this.rules.addAll(this.loadFromFile(file));
		}
	}

	/**
	 * @see org.mineacademy.fo.model.RuleSetReader#createRule(java.io.File, java.lang.String)
	 */
	@Override
	protected Rule createRule(File file, String value) {
		return new Rule(file, value);
	}

	/**
	 * Return the list of rule names
	 *
	 * @return
	 */
	public List<String> getRuleNames() {
		final List<String> names = new ArrayList<>();

		for (final Rule rule : this.rules)
			names.add(rule.getName());

		return names;
	}
}
