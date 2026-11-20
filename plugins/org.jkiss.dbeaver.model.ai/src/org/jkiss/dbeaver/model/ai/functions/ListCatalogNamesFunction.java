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
package org.jkiss.dbeaver.model.ai.functions;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class ListCatalogNamesFunction extends ListStructNamesFunction {
    public static final Log log = Log.getLog(ListCatalogNamesFunction.class);

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull Map<String, Object> parameters
    ) throws DBException {
        AIDatabaseContext aiDatabaseContext = context.getContext();
        if (aiDatabaseContext == null) {
            return new AIFunctionResult(AIFunctionType.INFORMATION, "No database connection");
        }

        DBCExecutionContext executionContext = aiDatabaseContext.getExecutionContext();
        DBPDataSource dataSource = executionContext.getDataSource();
        DBPDataSourceInfo info = dataSource.getInfo();
        if (info.getCatalogTerm() == null || (!(dataSource instanceof DBSObjectContainer objectContainer))) {
            return new AIFunctionResult(
                AIFunctionType.INFORMATION,
                "Catalogs are not supported by driver"
            );
        }

        List<String> catalogIdentifiers = CommonUtils.safeCollection(objectContainer.getChildren(context.getMonitor())).stream()
            .filter(c -> c instanceof DBSCatalog catalog &&
                AIUtils.isCatalogInScope(aiDatabaseContext, executionContext, catalog))
            .map(c -> DBUtils.getObjectFullName(c, DBPEvaluationContext.DDL)).toList();

        if (catalogIdentifiers.isEmpty()) {
            return new AIFunctionResult(
                AIFunctionType.INFORMATION,
                "No catalogs in the database or catalogs are not supported"
            );
        }

        return new AIFunctionResult(
            AIFunctionType.INFORMATION,
            String.join(",", catalogIdentifiers)
        );
    }

    @NotNull
    @Override
    public FunctionState getFunctionState(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor function
    ) {
        return isChildTypeSupported(context, DBSCatalog.class) ?
            FunctionState.APPLICABLE : FunctionState.NOT_APPLICABLE;
    }

}
