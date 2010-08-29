/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataTypeProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * standard JDBC data types provider
 */
public class JDBCStandardDataTypeProvider implements DBDDataTypeProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return JDBCUtils.getDataIcon(type).getImage();
    }

    public DBDValueHandler getHandler(DBPDataSource dataSource, DBSTypedObject type)
    {
        DBSDataKind dataKind = JDBCUtils.getDataKind(type);
        switch (dataKind) {
            case BOOLEAN:
                return JDBCBooleanValueHandler.INSTANCE;
            case STRING:
                if (type.getValueType() == java.sql.Types.LONGVARCHAR || type.getValueType() == java.sql.Types.LONGNVARCHAR) {
                    // Eval longvarchars as LOBs
                    return JDBCContentValueHandler.INSTANCE;
                } else {
                    return JDBCStringValueHandler.INSTANCE;
                }
            case NUMERIC:
                return JDBCNumberValueHandler.INSTANCE;
            case DATETIME:
                return JDBCDateTimeValueHandler.INSTANCE;
            case BINARY:
            case LOB:
                return JDBCContentValueHandler.INSTANCE;
            default:
                return null;
        }
    }

}
