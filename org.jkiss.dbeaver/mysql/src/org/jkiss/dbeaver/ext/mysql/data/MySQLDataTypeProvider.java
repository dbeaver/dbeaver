/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.data.DBDDataTypeProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * MySQL data types provider
 */
public class MySQLDataTypeProvider implements DBDDataTypeProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return JDBCUtils.getDataIcon(type).getImage();
    }

    public DBDValueHandler getHandler(DBCExecutionContext context, DBSTypedObject type)
    {
        String typeName = type.getTypeName();
        if (MySQLConstants.TYPE_NAME_ENUM.equalsIgnoreCase(typeName)) {
            return MySQLEnumValueHandler.INSTANCE;
        } else if (MySQLConstants.TYPE_NAME_SET.equalsIgnoreCase(typeName)) {
            return MySQLSetValueHandler.INSTANCE;
        } else {
            return null;
        }
    }

}