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
package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.edit.OracleDataTypeManager;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataType;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * YashanDBDataTypeManager
 */
public class YashanDBDataTypeManager extends OracleDataTypeManager {

    @Override
    protected YashanDBDataType createDatabaseObject(@NotNull DBRProgressMonitor monitor,
                                                    @NotNull DBECommandContext context, final Object container, Object copyFrom,
                                                    @NotNull Map<String, Object> options) {
        YashanDBSchema schema = (YashanDBSchema) container;
        YashanDBDataType dataType = new YashanDBDataType(schema, "DataType", false);
        dataType.setObjectDefinitionText("TYPE " + dataType.getName() + " AS OBJECT\n" + //$NON-NLS-1$ //$NON-NLS-2$
            "(\n" + //$NON-NLS-1$
            ")"); //$NON-NLS-1$
        return dataType;
    }

    @Override
    protected void createOrReplaceProcedureQuery(@NotNull DBRProgressMonitor monitor,
                                                 @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList,
                                                 @NotNull OracleDataType dataType) {
        String header = OracleUtils.normalizeSourceName(monitor, dataType, false);
        if (!CommonUtils.isEmpty(header)) {
            // YashanDB header has "create or replace".
            actionList.add(new SQLDatabasePersistAction("Create type header", header)); // $NON-NLS-2$
        }
        String body = OracleUtils.normalizeSourceName(monitor, dataType, true);
        if (!CommonUtils.isEmpty(body)) {
            actionList.add(new SQLDatabasePersistAction("Create type body", "CREATE OR REPLACE " + body)); //$NON-NLS-2$
        }
        OracleUtils.addSchemaChangeActions(executionContext, actionList, dataType);
    }
}
