/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
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
    protected DB2Sequence createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                               final DB2Schema schema,
                                               Object copyFrom)
    {
        return new UITask<DB2Sequence>() {
            @Override
            protected DB2Sequence runTask() {
                EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.SEQUENCE);
                if (!page.edit()) {
                    return null;
                }

                return new DB2Sequence(schema, page.getEntityName());
            }
        }.execute();
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
        String sql = String.format(SQL_DROP, command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
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
        sb.append(sequence.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(SPACE);
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
            return String.format(SQL_COMMENT, sequence.getFullyQualifiedName(DBPEvaluationContext.DDL), sequence.getDescription());
        } else {
            return null;
        }
    }

}
