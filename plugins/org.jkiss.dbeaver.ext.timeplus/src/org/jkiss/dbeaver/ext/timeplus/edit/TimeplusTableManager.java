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
package org.jkiss.dbeaver.ext.timeplus.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class TimeplusTableManager extends GenericTableManager {

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(TimeplusTableColumn.class);

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    protected String getCreateTableType(GenericTableBase table) {
        return "STREAM";
    }

    @Override
    protected void appendTableModifiers(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table,
        @NotNull NestedObjectCommand tableProps,
        @NotNull StringBuilder ddl,
        boolean alter,
        @NotNull Map<String, Object> options
    ) {
        if (!alter && CommonUtils.isNotEmpty(table.getDescription())) {
            ddl.append(" COMMENT ").append(SQLUtils.quoteString(table, table.getDescription()));
        }
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<GenericTableBase, PropertyHandler> command,
        @NotNull Map<String, Object> options
    ) {
        GenericTableBase table = command.getObject();
        if (table.isPersisted() && command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(new SQLDatabasePersistAction(
                "Comment stream",
                "ALTER STREAM " + table.getFullyQualifiedName(DBPEvaluationContext.DDL)
                    + " MODIFY COMMENT " + SQLUtils.quoteString(table, CommonUtils.notEmpty(table.getDescription()))
            ));
        }
    }
}
