package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;

/**
 * Abstract DM schema object 
 * @author caosw
 *
 */
public abstract class DmSchemaObject extends DmObject<DmSchema> implements DBPQualifiedObject {

	protected DmSchemaObject(DmSchema parent, String name, boolean persisted) {
		super(parent, name, persisted);
	}
	
	public DmSchema getSchema() {
		return getParentObject();
	}
	
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getParentObject(),this);
	}
}
