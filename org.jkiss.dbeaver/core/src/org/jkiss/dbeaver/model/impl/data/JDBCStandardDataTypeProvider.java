package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataTypeProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.eclipse.swt.graphics.Image;

/**
 * standard JDBC data types provider
 */
public class JDBCStandardDataTypeProvider implements DBDDataTypeProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return null;
    }

    public DBDValueHandler createHandler(DBPDataSource dataSource, DBSTypedObject type)
    {
        return null;
    }

}
