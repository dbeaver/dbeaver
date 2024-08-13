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
package org.jkiss.dbeaver.ext.altibase.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseMaterializedView;
import org.jkiss.dbeaver.ext.generic.edit.GenericViewManager;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.Map;

public class AltibaseMaterializedViewManager extends GenericViewManager {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }
    
    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }
    
    protected String getDropViewType(GenericTableBase table) {
        return AltibaseConstants.OBJ_TYPE_MATERIALIZED_VIEW;
    }

    @Override
    protected GenericTableBase createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context,
                                                    Object container, Object copyFrom, @NotNull Map<String, Object> options) {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        String tableName = getNewChildName(monitor, structContainer, SQLTableManager.BASE_MATERIALIZED_VIEW_NAME);
        GenericTableBase viewImpl = structContainer.getDataSource().getMetaModel().createTableOrViewImpl(
                structContainer, tableName,
                AltibaseConstants.OBJ_TYPE_MATERIALIZED_VIEW,
                null);
        if (viewImpl instanceof AltibaseMaterializedView) {
            ((AltibaseMaterializedView) viewImpl).setObjectDefinitionText(
                    "CREATE MATERIALIZED VIEW " 
                            + viewImpl.getFullyQualifiedName(DBPEvaluationContext.DDL) 
                            + " AS SELECT 1 as A\n");
        }
        return viewImpl;
    }
    
    @Override
    protected String getBaseObjectName() {
        return SQLTableManager.BASE_MATERIALIZED_VIEW_NAME;
    }
    
    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        actions.add(
            new SQLDatabasePersistAction("Drop view", 
                    "DROP " + AltibaseConstants.OBJ_TYPE_MATERIALIZED_VIEW + " " 
                            + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL))
        );
    }
    
    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        final AltibaseMaterializedView view = (AltibaseMaterializedView) command.getObject();
        actions.add(new SQLDatabasePersistAction(
                "Create " + AltibaseConstants.OBJ_TYPE_MATERIALIZED_VIEW.toLowerCase(), view.getDDL()));
    }
}
