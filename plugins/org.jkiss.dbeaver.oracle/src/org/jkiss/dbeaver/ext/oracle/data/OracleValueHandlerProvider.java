/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
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

    public DBDValueHandler getHandler(DBDPreferences preferences, String typeName, int valueType)
    {
        if (OracleConstants.TYPE_NAME_XML.equals(typeName) || OracleConstants.TYPE_FQ_XML.equals(typeName)) {
            return OracleXMLValueHandler.INSTANCE;
        } else if (valueType == java.sql.Types.STRUCT) {
            return OracleObjectValueHandler.INSTANCE;
        } else if (typeName.indexOf("TIMESTAMP") != -1) {
            return new OracleTimestampValueHandler(preferences.getDataFormatterProfile());
        } else {
            return null;
        }
    }

}