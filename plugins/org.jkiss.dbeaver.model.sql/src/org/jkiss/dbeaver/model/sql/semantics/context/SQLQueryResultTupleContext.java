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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Redefines result tuple leaving the aggregated sources from the parent context
 */
public class SQLQueryResultTupleContext extends SQLQuerySyntaxContext {

    private static final Log log = Log.getLog(SQLQueryResultTupleContext.class);
    @NotNull
    private final List<SQLQueryResultColumn> columns;
    @NotNull
    private final Set<DBSEntity> realSources;
    @NotNull
    private final List<SQLQueryResultPseudoColumn> pseudoColumns;

    public SQLQueryResultTupleContext(@NotNull SQLQueryDataContext parent, @NotNull List<SQLQueryResultColumn> columns, @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns) {
        super(parent);
        this.columns = columns;
        this.realSources = columns.stream().map(c -> c.realSource).filter(Objects::nonNull).collect(Collectors.toSet());
        this.pseudoColumns = pseudoColumns;
    }

    @NotNull
    @Override
    public List<SQLQueryResultColumn> getColumnsList() {
        return this.columns;
    }

    @NotNull
    @Override
    public List<SQLQueryResultPseudoColumn> getPseudoColumnsList() {
        return this.pseudoColumns;
    }

    @Nullable
    @Override
    public SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String columnName) {  // TODO consider reporting ambiguity
        SQLQueryResultColumn result = this.columns.stream()
            .filter(c -> c.symbol.getName().equals(columnName))
            .findFirst()
            .orElse(null);
        
        if (result != null) {
            return result;
        }
        
        String unquoted = this.getDialect().getUnquotedIdentifier(columnName);
        for (DBSEntity source : this.realSources) {
            try {
                DBSEntityAttribute attr = source.getAttribute(monitor, unquoted);
                if (attr != null) {
                    result = this.columns.stream()
                        .filter(c -> c.realAttr == attr)
                        .findFirst()
                        .orElse(null);
                }
            } catch (DBException e) {
                log.debug("Failed to resolve column", e);
                result = null;
            }
            
            if (result != null) {
                return result;
            }
        }
        
        return null;
    }

    @Override
    @Nullable
    public SQLQueryResultPseudoColumn resolvePseudoColumn(DBRProgressMonitor monitor, @NotNull String name) {
        SQLQueryResultPseudoColumn result = this.pseudoColumns.stream()
            .filter(c -> c.symbol.getName().equals(name))
            .findFirst()
            .orElse(null);

        return result;
    }
}

