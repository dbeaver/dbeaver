package org.jkiss.dbeaver.ext.cubrid;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class CubridDataSourceProvider extends GenericDataSourceProvider {
	
	public CubridDataSourceProvider()
	{
	}

	@NotNull
	@Override
	public DBPDataSource openDataSource(
		@NotNull DBRProgressMonitor monitor,
		@NotNull DBPDataSourceContainer container)
		throws DBException {
		return new CubridDataSource(monitor, container, new CubridMetaModel());
	}

}
