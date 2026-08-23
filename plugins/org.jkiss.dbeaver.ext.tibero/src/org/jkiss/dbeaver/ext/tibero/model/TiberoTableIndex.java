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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.Map;

/**
 * TiberoTableIndex
 */
public class TiberoTableIndex extends OracleTableIndex {

    public TiberoTableIndex(
        OracleSchema schema,
        OracleTableBase table,
        String indexName,
        java.sql.ResultSet dbResult
    ) {
        super(schema, table, indexName, dbResult);
    }

    public TiberoTableIndex(
        OracleSchema schema,
        OracleTableBase parent,
        String name,
        boolean unique,
        DBSIndexType indexType
    ) {
        super(schema, parent, name, unique, indexType);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Map<String, Object> options
    ) throws DBException {
        String definition = super.getObjectDefinitionText(monitor, options);
        if (definition != null) {
            int end = definition.length() - 1;
            while (end >= 0 && Character.isWhitespace(definition.charAt(end))) {
                end--;
            }
            if (end < 0 || definition.charAt(end) != ';') {
                definition = definition + ";";
            }
        }
        return definition;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return super.getFullyQualifiedName(context);
    }
}
