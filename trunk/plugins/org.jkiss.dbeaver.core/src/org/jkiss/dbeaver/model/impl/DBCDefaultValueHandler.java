/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
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
        return FEATURE_EDITOR;
    }

    @Override
    public Class getValueObjectType()
    {
        return Object.class;
    }

    @Override
    public Object fetchValueObject(
        DBCSession session,
        DBCResultSet resultSet,
        DBSTypedObject type,
        int index) throws DBCException
    {
        return resultSet.getColumnValue(index + 1);
    }

    @Override
    public void bindValueObject(
        DBCSession session,
        DBCStatement statement,
        DBSTypedObject type,
        int index,
        Object value) throws DBCException
    {
        
    }

    @Override
    public Object getValueFromObject(DBCSession session, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        return object;
    }

    @Override
    public void releaseValueObject(Object value) {
        if (value instanceof DBDValue) {
            ((DBDValue) value).release();
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format) {
        return DBUtils.getDefaultValueDisplayString(value, format);
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller) throws DBCException {
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller) {
    }

    @Override
    public DBDValueEditor createEditor(DBDValueController controller) throws DBException {
        return new TextViewDialog(controller);
    }

}
