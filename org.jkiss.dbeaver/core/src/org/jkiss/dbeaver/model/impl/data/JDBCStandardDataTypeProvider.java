package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataTypeProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.eclipse.swt.graphics.Image;

/**
 * standard JDBC data types provider
 */
public class JDBCStandardDataTypeProvider implements DBDDataTypeProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return null;
    }

    public DBDValueHandler getHandler(DBPDataSource dataSource, DBSTypedObject type)
    {
        DBSDataKind dataKind = JDBCUtils.getDataKind(type.getValueType());
        switch (dataKind) {
            case BOOLEAN:
                return JDBCBooleanValueHandler.INSTANCE;
            case STRING:
                return JDBCStringValueHandler.INSTANCE;
            case NUMERIC:
                return JDBCNumberValueHandler.INSTANCE;
            case DATETIME:
                return JDBCDateTimeValueHandler.INSTANCE;
            default:
                return null;
        }
    }

}
