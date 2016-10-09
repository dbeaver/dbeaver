package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolGlobalObject implements DBSObject, DBPSaveableObject {

	private final ExasolDataSource dataSource;
	private boolean persisted;
	
    protected ExasolGlobalObject(ExasolDataSource dataSource, boolean persisted)
    {
        this.dataSource = dataSource;
        this.persisted = persisted;
    }
	
    
	@Nullable
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPersisted() {
		// TODO Auto-generated method stub
		return persisted;
	}

	@Override
	public void setPersisted(boolean persisted) {
		
		this.persisted = persisted;
		
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DBSObject getParentObject() {
		// TODO Auto-generated method stub
		return dataSource.getContainer();
	}

	@Override
	public DBPDataSource getDataSource() {
		// TODO Auto-generated method stub
		return dataSource;
	}

}
