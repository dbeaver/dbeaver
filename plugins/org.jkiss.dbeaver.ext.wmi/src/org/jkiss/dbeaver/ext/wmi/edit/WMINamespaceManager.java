/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.wmi.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
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
    public DBEPropertyHandler<WMINamespace> makePropertyHandler(WMINamespace object, DBPPropertyDescriptor property)
    {
        return null;
    }

    @Override
    public void executePersistAction(DBCSession session, DBECommand<WMINamespace> wmiNamespaceDBECommand, DBEPersistAction action) throws DBException
    {

    }
}

