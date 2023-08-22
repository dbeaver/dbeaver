package org.jkiss.dbeaver.ext.dm.model;

/**
 * Stored code interface
 * @author caosw
 *
 */
public enum DmSourceType {
	TYPE(false), PROCEDURE(false), FUNCTION(false), PACKAGE(false), TRIGGER(false), VIEW(true), MATERIALIZED_VIEW(true);

	private final boolean isCustom;

	DmSourceType(boolean custom) {
		isCustom = custom;
	}

	public boolean isCustom() {
		return isCustom;
	}
}
