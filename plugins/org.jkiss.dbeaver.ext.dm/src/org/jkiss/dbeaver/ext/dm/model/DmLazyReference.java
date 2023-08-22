package org.jkiss.dbeaver.ext.dm.model;

public class DmLazyReference {

	final String schemaName;

	final String objectName;

	public DmLazyReference(String schemaName, String objectName) {
		this.schemaName = schemaName;
		this.objectName = objectName;
	}
}
