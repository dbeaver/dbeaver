package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public abstract class DmObject<PARENT extends DBSObject> implements DBSObject, DBPSaveableObject {

	private static final Log log = Log.getLog(DmObject.class);

	protected final PARENT parent;
	protected String name;
	private boolean persisted;
	private long objectId;

	protected DmObject(PARENT parent, String name, long objectId, boolean persisted) {
		this.parent = parent;
		this.name = CommonUtils.notEmpty(name);
		this.objectId = objectId;
		this.persisted = persisted;
	}

	protected DmObject(PARENT parent, String name, boolean persisted) {
		this.parent = parent;
		this.name = name;
		this.persisted = persisted;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public PARENT getParentObject() {
		return parent;
	}

	@NotNull
	@Override
	public DmDataSource getDataSource() {
		return (DmDataSource) parent.getDataSource();
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getObjectId() {
		return objectId;
	}

	@Override
	public boolean isPersisted() {
		return persisted;
	}

	@Override
	public void setPersisted(boolean persisted) {
		this.persisted = persisted;
	}
}
