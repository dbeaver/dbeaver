/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides information about query sources (tables, subqueries, etc.) and columns used in the query
 */
public class SQLQueryRowsDataContext {

    // TODO introduce class ResultColumnInfo extends SQLQueryResultColumn having reference for KnownRowsSourceInfo

    private static final Log log = Log.getLog(SQLQueryResultTupleContext.class);

    @NotNull
    private final SQLQueryRowsSourceContext rowsSources;
    @NotNull
    private final List<SQLQueryResultColumn> columns;
    @NotNull
    private final Set<DBSEntity> realSources;
    @NotNull
    private final List<SQLQueryResultPseudoColumn> pseudoColumns;

    public SQLQueryRowsDataContext(
        @NotNull SQLQueryRowsSourceContext rowsSources,
        @NotNull List<SQLQueryResultColumn> columns,
        @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns
    ) {
        this.rowsSources = rowsSources;
        this.columns = columns;
        this.realSources = columns.stream().map(c -> c.realSource).filter(Objects::nonNull).collect(Collectors.toSet());
        this.pseudoColumns = pseudoColumns;
    }

    @NotNull
    public SQLQueryConnectionContext getConnection() {
        return this.rowsSources.getConnectionInfo();
    }

    @NotNull
    public SQLQueryRowsSourceContext getRowsSources() {
        return this.rowsSources;
    }

    @NotNull
    public List<SQLQueryResultColumn> getColumnsList() {
        return this.columns;
    }

    @NotNull
    public List<SQLQueryResultPseudoColumn> getPseudoColumnsList() {
        return this.pseudoColumns;
    }

    /**
     * Returns information about resolved column searching for specified column name in metadata
     */
    @Nullable
    public SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String columnName) {
        // TODO consider reporting ambiguity
        SQLQueryResultColumn result = this.columns.stream()
            .filter(c -> c.symbol.getName().equals(columnName))
            .findFirst()
            .orElse(null);

        if (result != null) {
            return result;
        }

        String unquoted = this.getConnection().dialect.getUnquotedIdentifier(columnName);
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
            }

            if (result != null) {
                return result;
            }
        }

        return null;
    }


    /**
     * Return information about the pseudo column used in the query by the specified name
     */
    @Nullable
    public SQLQueryResultPseudoColumn resolvePseudoColumn(@NotNull String name) {
        SQLQueryResultPseudoColumn result = this.pseudoColumns.stream()
            .filter(c -> c.symbol.getName().equals(name))
            .findFirst()
            .orElse(null);

        return result;
    }

    /**
     * Combine information about query sources (tables, subqueries, etc.) and columns used in the query
     */
    public SQLQueryRowsDataContext combine(SQLQueryRowsDataContext other) {
        SQLQueryRowsSourceContext combinedSources = this.rowsSources.combine(other.rowsSources);
        return combinedSources.makeTuple(
            STMUtils.combineLists(this.getColumnsList(), other.getColumnsList()),
            // TODO consider ambiguity and/or propagation policy of pseudo-columns here
            STMUtils.combineLists(this.getPseudoColumnsList(), other.getPseudoColumnsList())
        );
    }
}
