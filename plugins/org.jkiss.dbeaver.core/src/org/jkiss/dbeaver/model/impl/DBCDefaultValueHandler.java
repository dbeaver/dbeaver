/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER;
    }

    @Override
    public Class getValueObjectType()
    {
        return Object.class;
    }

    @Override
    public Object getValueObject(
        DBCExecutionContext context,
        DBCResultSet resultSet,
        DBSTypedObject column,
        int columnIndex) throws DBCException
    {
        return resultSet.getColumnValue(columnIndex + 1);
    }

    @Override
    public void bindValueObject(
        DBCExecutionContext context,
        DBCStatement statement,
        DBSTypedObject columnType,
        int paramIndex,
        Object value) throws DBCException
    {
        
    }

    @Override
    public Object createValueObject(DBCExecutionContext context, DBSTypedObject column) throws DBCException
    {
        return null;
    }

    @Override
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

    @Override
    public void releaseValueObject(Object value) {
        if (value instanceof DBDValue) {
            ((DBDValue) value).release();
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value) {
        return DBUtils.getDefaultValueDisplayString(value);
    }

    @Override
    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column) throws DBCException {
        return null;
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller) throws DBCException {
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller) {
    }

    @Override
    public boolean editValue(DBDValueController controller) throws DBException {
        TextViewDialog dialog = new TextViewDialog(controller);
        dialog.open();
        return true;
    }

}
