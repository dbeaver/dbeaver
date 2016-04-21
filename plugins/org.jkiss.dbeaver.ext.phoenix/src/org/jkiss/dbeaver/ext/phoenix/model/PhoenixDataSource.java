package org.jkiss.dbeaver.ext.phoenix.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;

public class PhoenixDataSource extends GenericDataSource {

	public PhoenixDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
			throws DBException {
		super(monitor, container, metaModel);
	}
	
    @Override
    protected SQLDialect createSQLDialect(@NotNull JDBCDatabaseMetaData metaData) {
        return new PhoenixSQLDialect(this, metaData);
    }
    
    

}
