/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.data.DBDDataTypeProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Oracle data types provider
 */
public class OracleDataTypeProvider implements DBDDataTypeProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return JDBCUtils.getDataIcon(type).getImage();
    }

    public DBDValueHandler getHandler(DBCExecutionContext context, DBSTypedObject type)
    {
/*
        String typeName = type.getTypeName();
        if (OracleConstants.TYPE_NAME_ENUM.equalsIgnoreCase(typeName)) {
            return OracleEnumValueHandler.INSTANCE;
        } else if (OracleConstants.TYPE_NAME_SET.equalsIgnoreCase(typeName)) {
            return OracleSetValueHandler.INSTANCE;
        } else {
            return null;
        }
*/
        return null;
    }

}