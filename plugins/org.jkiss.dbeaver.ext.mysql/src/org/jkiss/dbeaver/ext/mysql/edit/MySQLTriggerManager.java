/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;

import java.util.List;

/**
 * MySQLTriggerManager
 */
public class MySQLTriggerManager extends SQLTriggerManager<MySQLTrigger, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MySQLTrigger> getObjectsCache(MySQLTrigger object)
    {
        return object.getCatalog().getTriggerCache();
    }

    @Override
    protected MySQLTrigger createDatabaseObject(DBECommandContext context, MySQLTable parent, Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(DBeaverUI.getActiveWorkbenchShell(), parent.getDataSource(), "Create trigger");
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        MySQLTrigger newTrigger = new MySQLTrigger(parent.getContainer(), parent, dialog.getEntityName());
        newTrigger.setObjectDefinitionText(""); //$NON-NLS-1$
        return newTrigger;
    }

    protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, MySQLTrigger trigger) {
        String ddl =
            "CREATE TRIGGER " + trigger.getFullQualifiedName() + "\n" +
                trigger.getActionTiming() + " " + trigger.getManipulationType() + "\n" +
            "ON " + trigger.getTable().getFullQualifiedName() + " FOR EACH ROW\n" +
            "BEGIN\n" +
            trigger.getBody() + "\n" +
            "END";

        actions.add(new SQLDatabasePersistAction("Create trigger", ddl)); //$NON-NLS-2$
    }

}

