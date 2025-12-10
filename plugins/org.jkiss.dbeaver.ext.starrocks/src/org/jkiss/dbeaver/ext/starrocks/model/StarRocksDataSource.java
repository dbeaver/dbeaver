package org.jkiss.dbeaver.ext.starrocks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.code.NotNull;

public class StarRocksDataSource extends MySQLDataSource {

    public StarRocksDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
            throws DBException {
        super(monitor, container);
    }

    @Override
    public String getName() {
        return "StarRocks";
    }

    // This is where you'll add catalog-aware functionality later
}