package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

public class MySQLDataSourceProvider extends JDBCDataSourceProvider {

    public MySQLDataSourceProvider()
    {
    }

    public DBPDataSource openDataSource(
        DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        return new MySQLDataSource(monitor, container);
    }

}
