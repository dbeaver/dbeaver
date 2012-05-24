/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * standard JDBC data types provider
 */
public class JDBCStandardValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public Image getTypeImage(DBSTypedObject type)
    {
        return JDBCUtils.getDataIcon(type).getImage();
    }

    @Override
    public DBDValueHandler getHandler(DBDPreferences preferences, String typeName, int valueType)
    {
        DBSDataKind dataKind = JDBCUtils.getDataKind(typeName, valueType);
        switch (dataKind) {
            case BOOLEAN:
                return new JDBCBooleanValueHandler();
            case STRING:
                if (valueType == java.sql.Types.LONGVARCHAR || valueType == java.sql.Types.LONGNVARCHAR) {
                    // Eval longvarchars as LOBs
                    return new JDBCContentValueHandler();
                } else {
                    return new JDBCStringValueHandler();
                }
            case NUMERIC:
                return new JDBCNumberValueHandler(preferences.getDataFormatterProfile());
            case DATETIME:
                return new JDBCDateTimeValueHandler(preferences.getDataFormatterProfile());
            case BINARY:
            case LOB:
                return new JDBCContentValueHandler();
            case ARRAY:
                return new JDBCArrayValueHandler();
            case STRUCT:
                return new JDBCStructValueHandler();
            default:
                return null;
        }
    }

}
