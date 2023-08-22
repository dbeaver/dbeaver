package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

/**
 * DM Persist action
 * 
 * @author caosw
 *
 */
public class DmObjectPersistAction extends SQLDatabasePersistAction {

	private final DmObjectType objectType;

	public DmObjectPersistAction(DmObjectType objectType, String title, String script) {
		super(title, script);
		this.objectType = objectType;
	}

	public DmObjectPersistAction(DmObjectType objectType, String script) {
		super(script);
		this.objectType = objectType;
	}

	public DmObjectType getObjectType() {
		return objectType;
	}
}
