package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridUser implements DBSObject, CubridScriptObject{

	private String name;
	private String comment;
	private CubridStructContainer container;

	public CubridUser(String name) {
		this.name = name;
	}
	
	public CubridUser(CubridStructContainer container, String name, String comment) {
		this.name = name;
		this.comment = comment;
		this.container = container;
	}

	@Property(viewable = true, order = 1)
	public String getName() {
		return name;
	}

	@Nullable
	@Property(viewable = true, order = 2)
	public String getComment() {
		return comment;
	}

	@Override
	public String getDescription() {
		return null;
	}
	@Override
	public boolean isPersisted() {
		return true;
	}
	@Override
	public CubridStructContainer getParentObject() {
		return this.container;
	}
	@Override
	public CubridDataSource getDataSource() {
		return this.container.getDataSource();
	}
    
	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public List<? extends CubridTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
		return this.container.getDataSource().getPhysicalTables(monitor, name);
	}
	
    public List<? extends CubridTable> getPhysicalSystemTables(DBRProgressMonitor monitor) throws DBException {
		return this.container.getDataSource().getPhysicalSystemTables(monitor, name);
    }
    
    public List<? extends CubridTableBase> getTables(DBRProgressMonitor monitor) throws DBException {
		return this.container.getDataSource().getTables(monitor);
    }
    
    public CubridTableBase getTable(DBRProgressMonitor monitor, String name) throws DBException {
		return this.container.getDataSource().getTable(monitor, name);
    }

}
