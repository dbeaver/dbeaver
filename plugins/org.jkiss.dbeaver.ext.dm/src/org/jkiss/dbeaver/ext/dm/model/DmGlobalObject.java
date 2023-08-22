package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract DM schema object
 * 
 * @author caosw
 *
 */
public abstract class DmGlobalObject implements DBSObject, DBPSaveableObject {

	private static final Log log = Log.getLog(DmGlobalObject.class);

	private final DmDataSource dataSource;
	private boolean persisted;

	protected DmGlobalObject(DmDataSource dataSource, boolean persisted) {
		this.dataSource = dataSource;
		this.persisted = persisted;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public DBSObject getParentObject() {
		return dataSource.getContainer();
	}

	@NotNull
	@Override
	public DmDataSource getDataSource() {
		return dataSource;
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
