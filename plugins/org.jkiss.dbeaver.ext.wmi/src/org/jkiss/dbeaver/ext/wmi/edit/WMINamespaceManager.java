/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.wmi.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
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

