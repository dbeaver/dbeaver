package org.jkiss.dbeaver.ext.dm.model;

public enum DmDataTypeModifier {
	REF, POINTER;

	public static DmDataTypeModifier resolveTypeModifier(String typeMod) {
		if (typeMod == null || typeMod.length() == 0) {
			return null;
		} else if (typeMod.equals("REF")) {
			return REF;
		} else {
			return POINTER;
		}
	}
}
