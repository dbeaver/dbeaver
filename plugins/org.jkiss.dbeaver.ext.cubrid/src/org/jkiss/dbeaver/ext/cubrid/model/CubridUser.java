package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Collection;
import java.util.List;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridUser implements DBSObject{
	private String name;
	private String comment;
	private CubridObjectContainer container;

	public CubridUser(String name) {
		this.name = name;
	}

	public CubridUser(CubridObjectContainer container, String name, String comment) {
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
	public CubridObjectContainer getParentObject() {
		return this.container;
	}

	public CubridDataSource getDataSource() {
		return this.container.getDataSource();
	}

	public Collection<? extends CubridTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
		return this.container.getPhysicalTables(monitor, name);
	}

	public Collection<? extends CubridTable> getPhysicalSystemTables(DBRProgressMonitor monitor) throws DBException {
		return this.container.getPhysicalSystemTables(monitor, name);
	}

	public Collection<? extends CubridView> getViews(DBRProgressMonitor monitor) throws DBException {
		return this.container.getViews(monitor, name);
    }

	public Collection<? extends CubridView> getSystemViews(DBRProgressMonitor monitor) throws DBException {
		return this.container.getSystemViews(monitor, name);
    }

	public Collection<? extends GenericTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return this.container.getIndexes(monitor);
    }

	public Collection<? extends GenericProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
		return this.container.getProcedures(monitor);
    }

}