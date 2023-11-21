package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Collection;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DPIContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class CubridDataSource extends GenericDataSource {

	private final CubridMetaModel metaModel;
	private CubridObjectContainer structureContainer;
	
	public CubridDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, CubridMetaModel metaModel) throws DBException {
		super(monitor, container, metaModel, new CubridSQLDialect());
		this.metaModel = new CubridMetaModel();
    }
    
	@DPIContainer
	@NotNull
	@Override
	public CubridDataSource getDataSource() {
		return this;
	}

	public Collection<? extends CubridUser> getCubridUsers(DBRProgressMonitor monitor) throws DBException {
		return structureContainer == null ? null : structureContainer.getCubridUsers(monitor);
	}
	
	@NotNull
	public CubridMetaModel getMetaModel() {
		return metaModel;
	}
    
	@Override
	public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
		super.initialize(monitor);
        structureContainer = new CubridObjectContainer(this);
	}

}
