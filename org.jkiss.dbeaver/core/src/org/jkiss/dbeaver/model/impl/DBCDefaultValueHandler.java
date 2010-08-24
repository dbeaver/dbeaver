/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.DBException;
import org.eclipse.jface.action.IMenuManager;

/**
 * Default value handler
 */
public class DBCDefaultValueHandler implements DBDValueHandler {

    public static final DBCDefaultValueHandler INSTANCE = new DBCDefaultValueHandler();


    public Object getValueObject(DBRProgressMonitor monitor, DBCResultSet resultSet, DBSColumnBase column,
                                 int columnIndex) throws DBCException
    {
        Object value = resultSet.getColumnValue(columnIndex + 1);
        return value;
    }

    public void bindValueObject(DBRProgressMonitor monitor, DBCStatement statement, DBSTypedObject columnType,
                                int paramIndex, Object value) throws DBCException
    {
        
    }

    public Object copyValueObject(DBRProgressMonitor monitor, Object value)
        throws DBCException
    {
        return value;
    }

    public void releaseValueObject(Object value) {

    }

    public String getValueDisplayString(DBSTypedObject column, Object value) {
        if (value == null) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        return "[" + value.getClass().getSimpleName() + "]"; 
    }

    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column) throws DBCException {
        return null;
    }

    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller) throws DBCException {
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller) {
    }

    public boolean editValue(DBDValueController controller) throws DBException {
        return false;
    }
}
