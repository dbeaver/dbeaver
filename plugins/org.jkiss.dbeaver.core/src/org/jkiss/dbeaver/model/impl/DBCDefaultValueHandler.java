/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.Clipboard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
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
    public Object getValueFromClipboard(DBSTypedObject column, Clipboard clipboard)
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
    public DBDValueAnnotation[] getValueAnnotations(DBCAttributeMetaData attribute) throws DBCException {
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
