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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.impl.AISchemaGeneratorImpl;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class GetTableDetailsFunction implements AIFunction {

    private static final Log log = Log.getLog(GetTableDetailsFunction.class);

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
        if (!(dataSource instanceof DBSObjectContainer)) {
            throw new DBException("Tables are not supported by driver");
        }

        String tableNamesString = CommonUtils.toString(parameters.get(AIFunctionParams.TABLE_NAMES_PARAM), null);

        Set<String> tableNames = tableNamesString == null ?
            Set.of() :
            Arrays.stream(tableNamesString.split(","))
                .map(n -> DBUtils.getUnQuotedIdentifier(dataSource, n.trim()))
                .collect(Collectors.toSet());

        Map<String, Object> tableMap = new LinkedHashMap<>();
        for (String tableName : tableNames) {
            try {
                DBSObject table = AIUtils.findTableInContext(context, tableName);
                if (AIUtils.isObjectInScope(aiDatabaseContext, executionContext, table)) {
                    tableMap.put(tableName, table);
                } else {
                    tableMap.put(tableName, "Access was restricted");
                }
            } catch (DBException e) {
                tableMap.put(tableName, e.getMessage());
            }
        }

        String ddl = generateTableDDL(context, parameters, tableMap);
        return new AIFunctionResult(AIFunctionType.INFORMATION, ddl);
    }

    @NotNull
    private static String generateTableDDL(
        @NotNull AIFunctionContext context,
        @NotNull Map<String, Object> parameters,
        @NotNull Map<String, Object> tableMap
    ) throws DBException {
        // Cache metadata
        Set<DBSObjectContainer> ocList = new LinkedHashSet<>();
        for (Map.Entry<String, Object> tee : tableMap.entrySet()) {
            if (tee.getValue() instanceof DBSEntity entity && entity.getParentObject() instanceof DBSObjectContainer container) {
                ocList.add(container);
            }
        }
        for (DBSObjectContainer oc : ocList) {
            try {
                oc.cacheStructure(context.getMonitor(), DBSObjectContainer.STRUCT_ALL);
            } catch (DBException e) {
                log.debug("Error caching metadata in " + oc, e);
            }
        }

        AISchemaGenerator schemaGenerator = new AISchemaGeneratorImpl();

        boolean sendConstraints = CommonUtils.toBoolean(parameters.get(AIFunctionParams.DDL_CONSTRAINTS_PARAM));
        boolean sendIndexes = CommonUtils.toBoolean(parameters.get(AIFunctionParams.DDL_INDEXES_PARAM));
        boolean sendComments = CommonUtils.toBoolean(parameters.get(AIFunctionParams.DDL_COMMENTS_PARAM));
        boolean sendReferences = CommonUtils.toBoolean(parameters.get(AIFunctionParams.DDL_REFERENCES_PARAM));

        boolean fullDDL = context.getPrompt().hasFeature(AIConstants.AI_PROMPT_FEATURE_FULL_DDL);

        AISchemaGenerationOptions options = AISchemaGenerationOptions.builder()
            .withSendColumnTypes(true)
            .withSendForeignKeys(sendConstraints)
            .withSendConstraints(sendConstraints)
            .withSendObjectComment(sendComments)
            .withSendIndexes(sendIndexes)
            .withSendFullDDL(fullDDL)
            .withSendReferences(sendReferences)
            .withUseFQN(true)
            .build();

        DBCExecutionContext executionContext = context.getContext() == null ? null :
            context.getContext().getExecutionContext();

        StringJoiner c = new StringJoiner("\n");
        for (Map.Entry<String, Object> tee : tableMap.entrySet()) {
            if (tee.getValue() instanceof DBSEntity entity) {
                c.add(schemaGenerator.generateSchema(
                    context.getMonitor(),
                    executionContext,
                    options,
                    entity
                ));
            } else {
                c.add("Table '" + tee.getKey() + "' not resolved: " + tee.getValue());
            }
        }
        return c.toString();
    }
}
