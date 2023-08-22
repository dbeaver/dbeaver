package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

public abstract class DmProcedureBase<PARENT extends DBSObjectContainer> extends DmObject<PARENT>
		implements DBSProcedure {

	private static final Log log = Log.getLog(DmProcedureBase.class);

	private DBSProcedureType procedureType;

	public DmProcedureBase(PARENT parent, String name, long objectId, DBSProcedureType procedureType) {
		super(parent, name, objectId, true);
		this.procedureType = procedureType;
	}

	@Override
	@Property(viewable = true, editable = true, order = 3)
	public DBSProcedureType getProcedureType() {
		return procedureType;
	}

	public void setProcedureType(DBSProcedureType procedureType) {
		this.procedureType = procedureType;
	}

	@Override
	public DBSObjectContainer getContainer() {
		return getParentObject();
	}

	public abstract DmSchema getSchema();

	public abstract Integer getOverloadNumber();
}
