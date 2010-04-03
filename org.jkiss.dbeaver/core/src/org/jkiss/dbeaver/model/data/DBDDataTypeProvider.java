package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * DBPDataTypeProvider
 */
public interface DBDDataTypeProvider
{
    Image getTypeImage(DBSTypedObject type);

    DBDValueHandler createHandler(
        DBPDataSource dataSource,
        DBSTypedObject type);

}