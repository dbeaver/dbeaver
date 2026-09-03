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
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class ListSchemaNamesFunction extends ListStructNamesFunction {

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull Map<String, Object> parameters
    ) throws DBException {
        AIDatabaseContext aiDatabaseContext = context.getContext();
        if (aiDatabaseContext == null) {
            throw new DBException("No database connection");
        }

        DBCExecutionContext executionContext = aiDatabaseContext.getExecutionContext();
        DBPDataSource dataSource = executionContext.getDataSource();
        DBPDataSourceInfo info = dataSource.getInfo();
        if (info.getSchemaTerm() == null || (!(dataSource instanceof DBSObjectContainer objectContainer))) {
            throw new DBException("Schemas are not supported by driver");
        }

        DBRProgressMonitor monitor = context.getMonitor();

        String catalogName = CommonUtils.toString(parameters.get(AIFunctionParams.CATALOG_PARAM), null);
        if (!CommonUtils.isEmpty(catalogName)) {
            DBSObject catalog = DBUtils.getObjectByPath(
                monitor,
                executionContext,
                objectContainer,
                catalogName,
                null,
                null,
                true
            );
            if (!(catalog instanceof DBSObjectContainer coc)) {
                throw new DBException("Catalog '" + catalogName + "' is not an object container");
            }
            objectContainer = coc;
        } else {
            DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
            if (contextDefaults != null) {
                DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                if (defaultCatalog instanceof DBSObjectContainer oc) {
                    objectContainer = oc;
                }
            }
        }
        if (!DBSSchema.class.isAssignableFrom(objectContainer.getPrimaryChildType(monitor))) {
            throw new DBException("Driver doesn't support schemas");
        }

        if (!AIUtils.isObjectInScope(aiDatabaseContext, executionContext, objectContainer)) {
            throw new DBException("Container '" + objectContainer.getName() + "' is not in scope. Access restricted.");
        }

        List<String> schemaIdentifiers = CommonUtils.safeCollection(objectContainer.getChildren(monitor)).stream()
            .filter(c -> c instanceof DBSSchema schema &&
                AIUtils.isSchemaInScope(aiDatabaseContext, executionContext, schema))
            .map(c -> DBUtils.getObjectFullName(c, DBPEvaluationContext.DDL))
            .toList();

        if (schemaIdentifiers.isEmpty()) {
            return new AIFunctionResult(
                AIFunctionType.INFORMATION,
                "There are no schemas in " + objectContainer.getName()
            );
        }
        return new AIFunctionResult(
            AIFunctionType.INFORMATION,
            String.join(",", schemaIdentifiers)
        );
    }

    @NotNull
    @Override
    public FunctionState getFunctionState(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor function
    ) {
        AIDatabaseContext dbContext = context.getContext();
        DBCExecutionContext executionContext = dbContext == null ? null : dbContext.getExecutionContext();
        if (executionContext != null) {
            DBPDataSource dataSource = executionContext.getDataSource();
            if (dataSource instanceof DBSObjectContainer oc) {
                try {
                    Class<? extends DBSObject> catType = oc.getPrimaryChildType(context.getMonitor());
                    if (DBSSchema.class.isAssignableFrom(catType)) {
                        return FunctionState.APPLICABLE;
                    } else if (DBSCatalog.class.isAssignableFrom(catType)) {
                        DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
                        if (contextDefaults != null) {
                            DBSCatalog catalog = contextDefaults.getDefaultCatalog();
                            if (catalog != null) {
                                Class<? extends DBSObject> cct = catalog.getPrimaryChildType(context.getMonitor());
                                if (DBSSchema.class.isAssignableFrom(cct)) {
                                    return FunctionState.APPLICABLE;
                                }
                            }
                        }
                    }
                } catch (DBException e) {
                    log.debug(e);
                }
            }
        }
        return FunctionState.NOT_APPLICABLE;
    }

}
