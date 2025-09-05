package org.mineacademy.protect.api;

import org.bukkit.event.Cancellable;
import org.mineacademy.fo.event.SimpleEvent;
import org.mineacademy.protect.model.ScanCause;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * An event that is executed when a scan is triggered.
 */
@Getter
@Setter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractScanEvent extends SimpleEvent implements Cancellable {

	/**
	 * What triggered the scan.
	 */
	private final ScanCause cause;

	/**
	 * Is the event cancelled?
	 */
	private boolean cancelled;
}