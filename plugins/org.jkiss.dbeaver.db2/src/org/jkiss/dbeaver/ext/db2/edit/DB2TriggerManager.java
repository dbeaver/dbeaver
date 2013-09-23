/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TriggerCache;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Trigger Manager
 * 
 * @author Denis Forveille
 */
public class DB2TriggerManager extends JDBCObjectEditor<DB2Trigger, DB2Table> {

    private static String SQL_CREATE_TRIGGER;
    private static final String SQL_DROP_TRIGGER = "DROP TRIGGER %S ";

    static {
        StringBuilder sb = new StringBuilder(128);
        sb.append("CREATE TRIGGER %s.%s\n");
        sb.append("   BEFORE INSERT\n");
        sb.append("   ON %s.<table name>\n");
        sb.append("   REFERENCING NEW AS NEW_ROW\n");
        sb.append("   FOR EACH ROW\n");
        sb.append("   MODE DB2SQL\n");
        sb.append("BEGIN ATOMIC\n");
        sb.append("\n");
        sb.append("END;\n");
        SQL_CREATE_TRIGGER = sb.toString();
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Trigger name cannot be empty");
        }
    }

    @Override
    public DB2TriggerCache getObjectsCache(DB2Trigger object)
    {
        return object.getSchema().getTriggerCache();
    }

    @Override
    protected DB2Trigger createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, DB2Table parent,
        Object copyFrom)
    {
        CreateEntityDialog dialog =
            new CreateEntityDialog(workbenchWindow.getShell(), parent.getDataSource(),
                DB2Messages.edit_db2_trigger_manager_dialog_title);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        String triggerName = dialog.getEntityName();
        DB2Schema triggerSchema = parent.getContainer();
        String triggerSchemaName = triggerSchema.getName();

        DB2Trigger newTrigger = new DB2Trigger(triggerSchema, parent, triggerName);
        newTrigger.setSourceDeclaration(String.format(SQL_CREATE_TRIGGER, triggerSchemaName, triggerName, triggerSchemaName));

        return newTrigger;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        return createOrReplaceViewQuery(command.getObject());
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        String triggerName = command.getObject().getFullQualifiedName();
        IDatabasePersistAction action =
            new AbstractDatabasePersistAction("Drop trigger", String.format(SQL_DROP_TRIGGER, triggerName));
        return new IDatabasePersistAction[] { action };
    }

    private IDatabasePersistAction[] createOrReplaceViewQuery(DB2Trigger trigger)
    {
        String source = null;
        try {
            source = trigger.getSourceDeclaration(null);
            if (source == null) {
                return null;
            }
        } catch (DBException e) {
            log.error(e);
            return null;
        }

        IDatabasePersistAction action = new AbstractDatabasePersistAction("Create or replace Trigger", source);
        return new IDatabasePersistAction[] { action };
    }
}
