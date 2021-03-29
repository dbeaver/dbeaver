/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableCheckConstraint;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

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

    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableCheckConstraint> getObjectsCache(SQLServerTableCheckConstraint object) {
        return object.getParentObject().getCheckConstraintCache();
    }

    @Override
    protected SQLServerTableCheckConstraint createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        return new SQLServerTableCheckConstraint((SQLServerTable) container);
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        SQLServerTableCheckConstraint object = command.getObject();
        if (object.getConstraintType() == DBSEntityConstraintType.CHECK && CommonUtils.isEmpty(object.getCheckConstraintDefinition())) {
            throw new DBException("CHECK constraint definition is empty");
        }
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        final SQLServerTableCheckConstraint constraint = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_create_new_constraint,
                "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " WITH NOCHECK" +
                    " ADD CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint) +
                    " CHECK " + constraint.getCheckConstraintDefinition()
            ));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        final SQLServerTableCheckConstraint constraint = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_constraint,
                "ALTER TABLE " + constraint.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " DROP CONSTRAINT " + DBUtils.getQuotedIdentifier(constraint)
            ));
    }

}
