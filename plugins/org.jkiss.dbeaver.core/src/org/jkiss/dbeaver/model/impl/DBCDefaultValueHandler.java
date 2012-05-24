/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.Clipboard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

/**
 * Default value handler
 */
public class DBCDefaultValueHandler implements DBDValueHandler {

    public static final DBCDefaultValueHandler INSTANCE = new DBCDefaultValueHandler();

    public int getFeatures()
    {
        return FEATURE_VIEWER;
    }

    public Class getValueObjectType()
    {
        return Object.class;
    }

    public Object getValueObject(
        DBCExecutionContext context,
        DBCResultSet resultSet,
        DBSTypedObject column,
        int columnIndex) throws DBCException
    {
        return resultSet.getColumnValue(columnIndex + 1);
    }

    public void bindValueObject(
        DBCExecutionContext context,
        DBCStatement statement,
        DBSTypedObject columnType,
        int paramIndex,
        Object value) throws DBCException
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

    @Override
    public Object getValueFromClipboard(DBSTypedObject column, Clipboard clipboard) throws DBException
    {
        return null;
    }

    public void releaseValueObject(Object value) {
        if (value instanceof DBDValue) {
            ((DBDValue) value).release();
        }
    }

    public String getValueDisplayString(DBSTypedObject column, Object value) {
        return DBUtils.getDefaultValueDisplayString(value);
    }

    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column) throws DBCException {
        return null;
    }

    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller) throws DBCException {
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller) {
    }

    public boolean editValue(DBDValueController controller) throws DBException {
        TextViewDialog dialog = new TextViewDialog(controller);
        dialog.open();
        return true;
    }

}
