/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.CreateEntityDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * DB2 Sequence Manager
 *
 * @author Denis Forveille
 */
public class DB2SequenceManager extends SQLObjectEditor<DB2Sequence, DB2Schema> {

    private static final String SQL_CREATE = "CREATE SEQUENCE ";
    private static final String SQL_ALTER = "ALTER SEQUENCE ";
    private static final String SQL_DROP = "DROP SEQUENCE %s";
    private static final String SQL_COMMENT = "COMMENT ON SEQUENCE %s IS '%s'";

    private static final String SPACE = "\n   ";

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Sequence name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2Sequence> getObjectsCache(DB2Sequence object)
    {
        return object.getSchema().getSequenceCache();
    }

    @Override
    protected DB2Sequence createDatabaseObject(DBECommandContext context,
                                               DB2Schema db2Schema,
                                               Object copyFrom)
    {
        CreateEntityDialog dialog = new CreateEntityDialog(DBeaverUI.getActiveWorkbenchShell(),
            db2Schema.getDataSource(),
            DB2Messages.edit_db2_sequence_manager_dialog_title);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        return new DB2Sequence(db2Schema, dialog.getEntityName());
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        String sql = buildStatement(command.getObject(), false);
        actions.add(new SQLDatabasePersistAction("Create Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
            actions.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        String sql = buildStatement(command.getObject(), true);
        actionList.add(new SQLDatabasePersistAction("Alter Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
            actionList.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        String sql = String.format(SQL_DROP, command.getObject().getFullQualifiedName());
        DBEPersistAction action = new SQLDatabasePersistAction("Drop Sequence", sql);
        actions.add(action);
    }

    // -------
    // Helpers
    // -------
    private String buildStatement(DB2Sequence sequence, Boolean forUpdate)
    {

        StringBuilder sb = new StringBuilder(256);
        if (forUpdate) {
            sb.append(SQL_ALTER);
        } else {
            sb.append(SQL_CREATE);
        }
        sb.append(sequence.getFullQualifiedName()).append(SPACE);
        if (!(forUpdate)) {
            sb.append("AS ");
            sb.append(sequence.getPrecision().getSqlKeyword()).append(SPACE);
        }

        if (sequence.getStart() != null) {
            if (forUpdate) {
                sb.append("RESTART WITH ").append(sequence.getStart()).append(SPACE);
            } else {
                sb.append("START WITH ").append(sequence.getStart()).append(SPACE);
            }
        }

        if (sequence.getIncrementBy() != null) {
            sb.append("INCREMENT BY ").append(sequence.getIncrementBy()).append(SPACE);
        }
        if (sequence.getMinValue() != null) {
            sb.append("MINVALUE ").append(sequence.getMinValue()).append(SPACE);
        }
        if (sequence.getMaxValue() != null) {
            sb.append("MAXVALUE ").append(sequence.getMaxValue()).append(SPACE);
        }
        if (sequence.getCycle()) {
            sb.append("CYCLE ").append(SPACE);
        } else {
            sb.append("NO CYCLE ").append(SPACE);
        }
        if (sequence.getCache() != null) {
            sb.append("CACHE ").append(sequence.getCache()).append(SPACE);
        } else {
            sb.append("NO CACHE ").append(SPACE);
        }
        if (sequence.getOrder()) {
            sb.append("ORDER ").append(SPACE);
        } else {
            sb.append("NO ORDER ").append(SPACE);
        }

        return sb.toString();
    }

    private String buildComment(DB2Sequence sequence)
    {
        if ((sequence.getDescription() != null) && (sequence.getDescription().length() > 0)) {
            return String.format(SQL_COMMENT, sequence.getFullQualifiedName(), sequence.getDescription());
        } else {
            return null;
        }
    }

}
