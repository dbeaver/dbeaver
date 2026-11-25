/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTableIndex;
import org.jkiss.dbeaver.ext.generic.edit.GenericIndexManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Firebird index manager.
 * Firebird sorts the whole index ascending or descending (CREATE DESC INDEX), not single columns,
 * and supports expression indexes (COMPUTED BY), partial indexes (WHERE, Firebird 5+) and inactive indexes.
 */
public class FireBirdIndexManager extends GenericIndexManager {

    private static final String PROP_INACTIVE = "inactive";

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        FireBirdTableIndex index = (FireBirdTableIndex) command.getObject();
        String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
        index.setName(indexName);

        StringBuilder ddl = new StringBuilder("CREATE");
        if (index.isUnique()) {
            ddl.append(" UNIQUE");
        }
        if (index.isDescending()) {
            ddl.append(" DESCENDING");
        }
        ddl.append(" INDEX ").append(indexName)
            .append(" ON ").append(DBUtils.getEntityScriptName(index.getTable(), options));
        if (!CommonUtils.isEmpty(index.getExpression())) {
            ddl.append(" COMPUTED BY (").append(index.getExpression()).append(')');
        } else {
            List<GenericTableIndexColumn> columns = index.getAttributeReferences(monitor);
            ddl.append(" (").append(CommonUtils.safeCollection(columns).stream()
                .map(DBUtils::getQuotedIdentifier)
                .collect(Collectors.joining(","))).append(')');
        }
        if (!CommonUtils.isEmpty(index.getCondition())) {
            ddl.append(" WHERE ").append(index.getCondition());
        }
        actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, ddl.toString()));

        if (index.isInactive()) {
            actions.add(getActivationAction(index));
        }
        if (!CommonUtils.isEmpty(index.getDescription())) {
            actions.add(getCommentAction(index));
        }
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        FireBirdTableIndex index = (FireBirdTableIndex) command.getObject();
        if (command.getProperty(PROP_INACTIVE) != null) {
            actionList.add(getActivationAction(index));
        }
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actionList.add(getCommentAction(index));
        }
    }

    @NotNull
    private static DBEPersistAction getActivationAction(@NotNull FireBirdTableIndex index) {
        return new SQLDatabasePersistAction(
            "Set index state",
            "ALTER INDEX " + DBUtils.getQuotedIdentifier(index) + (index.isInactive() ? " INACTIVE" : " ACTIVE"));
    }

    @NotNull
    private static DBEPersistAction getCommentAction(@NotNull GenericTableIndex index) {
        String description = index.getDescription();
        return new SQLDatabasePersistAction(
            "Comment index",
            "COMMENT ON INDEX " + DBUtils.getQuotedIdentifier(index) + " IS "
                + (CommonUtils.isEmpty(description) ? "NULL" : SQLUtils.quoteString(index, description)));
    }
}
