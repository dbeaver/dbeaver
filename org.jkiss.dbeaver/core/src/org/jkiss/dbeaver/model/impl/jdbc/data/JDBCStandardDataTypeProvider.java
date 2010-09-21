/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.data.DBDDataTypeProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
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

    public DBDValueHandler getHandler(DBCExecutionContext context, DBSTypedObject type)
    {
        DBSDataKind dataKind = JDBCUtils.getDataKind(type);
        switch (dataKind) {
            case BOOLEAN:
                return new JDBCBooleanValueHandler();
            case STRING:
                if (type.getValueType() == java.sql.Types.LONGVARCHAR || type.getValueType() == java.sql.Types.LONGNVARCHAR) {
                    // Eval longvarchars as LOBs
                    return new JDBCContentValueHandler();
                } else {
                    return new JDBCStringValueHandler();
                }
            case NUMERIC:
                return new JDBCNumberValueHandler(context.getDataFormatterProfile());
            case DATETIME:
                return new JDBCDateTimeValueHandler(context.getDataFormatterProfile());
            case BINARY:
            case LOB:
                return new JDBCContentValueHandler();
            default:
                return null;
        }
    }

}
