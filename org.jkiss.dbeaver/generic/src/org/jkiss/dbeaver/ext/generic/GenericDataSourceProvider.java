package org.jkiss.dbeaver.ext.generic;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

public class GenericDataSourceProvider extends JDBCDataSourceProvider {

    public GenericDataSourceProvider()
    {
    }

    public DBPDataSource openDataSource(
        DBRProgressMonitor monitor,
        DBSDataSourceContainer container)
        throws DBException
    {
        return new GenericDataSource(monitor, container);
    }

}
