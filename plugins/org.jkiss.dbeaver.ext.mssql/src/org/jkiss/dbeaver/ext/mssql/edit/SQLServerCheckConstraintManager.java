/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableCheckConstraint;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableUniqueKey;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

import java.util.List;
import java.util.Map;

/**
 * SQL server unique constraint manager
 */
public class SQLServerCheckConstraintManager extends SQLObjectEditor<SQLServerTableCheckConstraint, SQLServerTable> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableCheckConstraint> getObjectsCache(SQLServerTableCheckConstraint object) {
        return object.getParentObject().getCheckConstraintCache();
    }

    @Override
    protected SQLServerTableCheckConstraint createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final SQLServerTable parent,
        Object from)
    {
        return new UITask<SQLServerTableCheckConstraint>() {
            @Override
            protected SQLServerTableCheckConstraint runTask() {
                EditConstraintPage editPage = new EditConstraintPage(
                    "Create CHECK constraint",
                    parent,
                    new DBSEntityConstraintType[] {DBSEntityConstraintType.CHECK} );
                if (!editPage.edit()) {
                    return null;
                }

                return null;
/*
                final SQLServerTableUniqueKey primaryKey = new SQLServerTableUniqueKey(
                    parent,
                    null,
                    null,
                    editPage.getConstraintType(),
                    false);
                primaryKey.setName(editPage.getConstraintName());
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    primaryKey.addColumn(
                        new SQLServerTableConstraintColumn(
                            primaryKey,
                            (SQLServerTableColumn) tableColumn,
                            colIndex++));
                }
                return primaryKey;
*/
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        final SQLServerTableCheckConstraint constraint = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_constraint,
                "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " WITH NOCHECK" +
                    " ADD CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint) + " CHECK " + constraint.getDefinition()
            ));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        final SQLServerTableCheckConstraint constraint = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_constraint,
                "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " DROP CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint)
            ));
    }

}
