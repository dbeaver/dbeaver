/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.edit;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.wmi.model.WMINamespace;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;

/**
 * OracleSchemaManager
 */
public class WMINamespaceManager extends AbstractObjectManager<WMINamespace> implements DBEObjectEditor<WMINamespace> {

    @Override
    public DBEPropertyHandler<WMINamespace> makePropertyHandler(WMINamespace object, IPropertyDescriptor property)
    {
        return null;
    }

    @Override
    public void executePersistAction(DBCExecutionContext context, DBECommand<WMINamespace> wmiNamespaceDBECommand, IDatabasePersistAction action) throws DBException
    {

    }
}

