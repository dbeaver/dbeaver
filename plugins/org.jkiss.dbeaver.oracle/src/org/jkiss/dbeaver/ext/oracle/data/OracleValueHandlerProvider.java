/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Oracle data types provider
 */
public class OracleValueHandlerProvider implements DBDValueHandlerProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return JDBCUtils.getDataIcon(type).getImage();
    }

    public DBDValueHandler getHandler(DBCExecutionContext context, DBSTypedObject type)
    {
        String typeName = type.getTypeName();
        if (OracleConstants.TYPE_NAME_XML.equals(typeName) || OracleConstants.TYPE_FQ_XML.equals(typeName)) {
            return OracleXMLValueHandler.INSTANCE;
        }/* else if (OracleConstants.TYPE_NAME_SET.equalsIgnoreCase(typeName)) {
            return OracleSetValueHandler.INSTANCE;
        } else {
            return null;
        }
*/
        return null;
    }

}