/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

/**
 * Default value handler
 */
public class DBCDefaultValueHandler implements DBDValueHandler {

    public static final DBCDefaultValueHandler INSTANCE = new DBCDefaultValueHandler();


    public Class getValueObjectType()
    {
        return Object.class;
    }

    public Object getValueObject(DBCExecutionContext context, DBCResultSet resultSet, DBSTypedObject column,
                                 int columnIndex) throws DBCException
    {
        Object value = resultSet.getColumnValue(columnIndex + 1);
        return value;
    }

    public void bindValueObject(DBCExecutionContext context, DBCStatement statement, DBSTypedObject columnType,
                                int paramIndex, Object value) throws DBCException
    {
        
    }

    public Object createValueObject(DBCExecutionContext context, DBSTypedObject column) throws DBCException
    {
        return null;
    }

    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
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
        String className = value.getClass().getName();
        if (className.startsWith("java.lang") || className.startsWith("java.util")) {
            // Standard types just use toString
            return value.toString();
        }
        // Unknown types prinyt their class name
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
