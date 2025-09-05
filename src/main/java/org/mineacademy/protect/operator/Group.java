package org.mineacademy.protect.operator;

import java.io.File;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a group holding operators that can be reused by many rules
 */
@Getter
@RequiredArgsConstructor
public final class Group extends ProtectOperator {

	/**
	 * The file where the group is stored
	 */
	private final File file;

	/**
	 * The name of the group
	 */
	private final String groupName;

	/**
	 * @see org.mineacademy.fo.model.Rule#getUniqueName()
	 */
	@Override
	public String getUniqueName() {
		return this.groupName;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Rule Group " + super.collectOptions().putArray("Group", this.groupName).toStringFormatted();
	}
}
