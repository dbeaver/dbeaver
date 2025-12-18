package org.jkiss.dbeaver.ext.starrocks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.starrocks.StarRocksDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.code.NotNull;

public class StarRocksDataSourceProvider extends MySQLDataSourceProvider {
    
    @NotNull
    @Override
    public DBPDataSource openDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container)
            throws DBException {
        return new StarRocksDataSource(monitor, container);
    }
}
