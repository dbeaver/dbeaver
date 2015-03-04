/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ext.wmi.edit;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.wmi.model.WMINamespace;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;

/**
 * OracleSchemaManager
 */
public class WMINamespaceManager extends AbstractObjectManager<WMINamespace> implements DBEObjectEditor<WMINamespace> {

    @Override
    public boolean canEditObject(WMINamespace object)
    {
        return false;
    }

    @Override
    public DBEPropertyHandler<WMINamespace> makePropertyHandler(WMINamespace object, IPropertyDescriptor property)
    {
        return null;
    }

    @Override
    public void executePersistAction(DBCSession session, DBECommand<WMINamespace> wmiNamespaceDBECommand, DBEPersistAction action) throws DBException
    {

    }
}

