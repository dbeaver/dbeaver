/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * MySQL data types provider
 */
public class MySQLValueHandlerProvider implements DBDValueHandlerProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return JDBCUtils.getDataIcon(type).getImage();
    }

    public DBDValueHandler getHandler(DBDPreferences preferences, String typeName, int valueType)
    {
        if (MySQLConstants.TYPE_NAME_ENUM.equalsIgnoreCase(typeName)) {
            return MySQLEnumValueHandler.INSTANCE;
        } else if (MySQLConstants.TYPE_NAME_SET.equalsIgnoreCase(typeName)) {
            return MySQLSetValueHandler.INSTANCE;
        } else if (JDBCUtils.getDataKind(typeName, valueType) == DBSDataKind.DATETIME) {
            return new MySQLDateTimeValueHandler(preferences.getDataFormatterProfile());
        } else {
            return null;
        }
    }

}