/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreMaterializedView;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreViewBase;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.util.List;
import java.util.Map;

/**
 * PostgreViewManager
 */
public class PostgreMViewManager extends PostgreViewManager {

    @Override
    protected PostgreMaterializedView createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options)
    {
        PostgreSchema schema = (PostgreSchema)container;
        PostgreMaterializedView newMV = new PostgreMaterializedView(schema);
        newMV.setName("new_mview"); //$NON-NLS-1$
        setNewObjectName(monitor, schema, newMV);
        return newMV;
    }

    @Override
    protected String getBaseObjectName() {
        return SQLTableManager.BASE_MATERIALIZED_VIEW_NAME;
    }

    @Override
    protected void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions, PostgreViewBase view, Map<String, Object> options) throws DBException {
        super.createOrReplaceViewQuery(monitor, actions, view, options);
        // Indexes DDL
    }

    @Override
    public void appendViewDeclarationPrefix(DBRProgressMonitor monitor, StringBuilder sqlBuf, PostgreViewBase view) throws DBException {
        PostgreMaterializedView mview = (PostgreMaterializedView) view;
        String[] relOptions = mview.getRelOptions();
        if (!ArrayUtils.isEmpty(relOptions)) {
            sqlBuf.append("\nWITH(").append(String.join("," , relOptions)).append(")");
        }
        PostgreTablespace tablespace = mview.getTablespace(monitor);
        if (tablespace  != null) {
            sqlBuf.append("\nTABLESPACE ").append(DBUtils.getQuotedIdentifier(tablespace));
        }
    }

    @Override
    public void appendViewDeclarationPostfix(DBRProgressMonitor monitor, StringBuilder sqlBuf, PostgreViewBase view) {
        PostgreMaterializedView mview = (PostgreMaterializedView)view;
        boolean withData = mview.isWithData();
        sqlBuf.append("\n").append(withData ? "WITH DATA" : "WITH NO DATA");
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException {
        final PostgreMaterializedView mView = (PostgreMaterializedView) command.getObject();
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION) || command.getProperties().size() > 1) {
            super.addObjectDeleteActions(monitor, executionContext, actionList, new ObjectDeleteCommand(mView, "Drop view"), options);
            super.addObjectModifyActions(monitor, executionContext, actionList, command, options);
        }
        if (command.hasProperty("tablespace")) {
            final String alterPrefix = "ALTER " + mView.getTableTypeName() + " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + " ";
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "SET TABLESPACE " + mView.getTablespace(monitor).getName()));
        }
    }


}

