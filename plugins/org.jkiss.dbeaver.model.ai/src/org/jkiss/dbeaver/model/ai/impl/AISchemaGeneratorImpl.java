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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectWithDescription;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AISchemaGenerationOptions;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class AISchemaGeneratorImpl implements AISchemaGenerator {

    @Override
    public String generateSchema(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext dbContext,
        @NotNull DBSEntity entity,
        boolean useFqn
    ) throws DBException {
        if (entity instanceof DBSTable table) {
            return describeTable(monitor, dbContext, table, useFqn);
        } else {
            return "";
        }
    }

    @NotNull
    public String describeTable(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext dbContext,
        @NotNull DBSTable table,
        boolean useFqn
    ) throws DBException {
        StringBuilder ddl = new StringBuilder();

        if (dbContext.getSchemaGenerationOptions().sendFullDDL()) {
            String tableDDL = DBStructUtils.generateTableDDL(
                monitor,
                table,
                Map.of(DBPScriptObject.OPTION_SKIP_DROPS, true),
                false
            );
            DBStructUtils.addDDLLine(ddl, tableDDL);
        } else {
            generateCustomDDL(monitor, dbContext, table, useFqn, ddl);
        }

        return ddl.toString();
    }

    @Nullable
    private static String generateCustomDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext dbContext,
        @NotNull DBSTable table,
        boolean useFqn,
        @NotNull StringBuilder ddl
    ) throws DBException {
        String name = useFqn
            ? DBUtils.getObjectFullName(dbContext.getExecutionContext().getDataSource(), table, DBPEvaluationContext.DDL)
            : DBUtils.getQuotedIdentifier(table);

        AISchemaGenerationOptions sgOptions = dbContext.getSchemaGenerationOptions();
        if (sgOptions.sendObjectComment()) {
            String tableDescription = describe(table);
            if (!tableDescription.isBlank()) {
                ddl.append(tableDescription).append("\n");
            }
        }

        if (table.isView()) {
            ddl.append("CREATE VIEW ");
        } else {
            ddl.append("CREATE TABLE ");
        }

        ddl.append(name);

        List<? extends DBSEntityAttribute> attributes = table.getAttributes(monitor);

        if (attributes == null || attributes.isEmpty()) {
            return ddl.append(");").toString();
        }

        StringJoiner columnsJoiner = new StringJoiner(",", " (", ") ");
        attributes.forEach(attr -> {
            if (DBUtils.isHiddenObject(attr)) {
                return;
            }

            columnsJoiner.add(
                DBUtils.getQuotedIdentifier(attr)
                    + (sgOptions.sendColumnTypes() ? " " + attr.getTypeName() : "")
                    + (sgOptions.sendObjectComment() && !describe(attr).isBlank() ? describe(attr) : "")
            );
        });

        ddl.append(columnsJoiner);
        return null;
    }

    @NotNull
    private static String describe(@NotNull DBPObjectWithDescription object) {
        String description = object.getDescription();
        if (description == null || description.isBlank()) {
            return "";
        } else {
            return "-- " + description + "\n";
        }
    }
}
