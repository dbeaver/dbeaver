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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ListTableNamesFunction implements AIFunction {

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
        if (!(dataSource instanceof DBSObjectContainer objectContainer)) {
            throw new DBException("Schemas are not supported by driver");
        }

        String catalogName = CommonUtils.toString(parameters.get(AIFunctionParams.CATALOG_PARAM), null);
        String schemaName = CommonUtils.toString(parameters.get(AIFunctionParams.SCHEMA_PARAM), null);
        Class<? extends DBSObject> dsChildType = objectContainer.getPrimaryChildType(context.getMonitor());
        if (DBSEntity.class.isAssignableFrom(dsChildType)) {
            // Ignore catalog and schema. LLM may hallucinate
            catalogName = null;
            schemaName = null;
        } else if (DBSSchema.class.isAssignableFrom(dsChildType)) {
            // Ignore schema. LLM may hallucinate
            catalogName = null;
        }

        DBSObject container = DBUtils.getObjectByPath(
            context.getMonitor(),
            executionContext,
            objectContainer,
            catalogName,
            schemaName,
            null
        );
        if (container == null) {
            container = objectContainer;
        }
        if (!(container instanceof DBSObjectContainer targetContainer)) {
            throw new DBException("Table container not found in the database");
        }
        if (!DBSEntity.class.isAssignableFrom(targetContainer.getPrimaryChildType(context.getMonitor()))) {
            throw new DBException("Container '" + targetContainer.getName() + "' doesn't support tables");
        }
        if (!AIUtils.isObjectInScope(aiDatabaseContext, executionContext, targetContainer)) {
            throw new DBException("Container '" + targetContainer.getName() + "' is not in scope. Access restricted.");
        }

        Map<DBSEntityType, List<DBSEntity>> entities = new LinkedHashMap<>();
        for (DBSObject child : CommonUtils.safeCollection(targetContainer.getChildren(context.getMonitor()))) {
            if (!(child instanceof DBSEntity entity)) {
                continue;
            }
            if (!AIUtils.isObjectInScope(aiDatabaseContext, executionContext, child)) {
                continue;
            }
            entities.computeIfAbsent(entity.getEntityType(), dbsEntityType -> new ArrayList<>()).add(entity);
        }

        if (entities.isEmpty()) {
            return new AIFunctionResult(
                AIFunctionType.INFORMATION,
                "There are no tables in " + objectContainer.getName()
            );
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<DBSEntityType, List<DBSEntity>> e : entities.entrySet()) {
            result.append(e.getKey().getName()).append(" names:");
            List<DBSEntity> value = e.getValue();
            for (int i = 0; i < value.size(); i++) {
                if (i > 0) {
                    result.append(',');
                }
                result.append(DBUtils.getQuotedIdentifier(value.get(i)));
            }
            result.append("\n");
        }
        return new AIFunctionResult(AIFunctionType.INFORMATION, result.toString());
    }
}
