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
package org.jkiss.dbeaver.ui.editors.sql.semantics.context;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsCorrelatedSourceModel;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsSourceModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SQLQueryDataContext {
    
    public abstract List<SQLQueryResultColumn> getColumnsList();

    public abstract DBSEntity findRealTable(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName);

    public abstract SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String simpleName);  // TODO consider ambiguous column names
    
    public SourceResolutionResult resolveSource(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) { // TODO consider ambiguous table names
        DBSEntity table = this.findRealTable(monitor, tableName);
        SQLQueryRowsSourceModel source = this.findRealSource(table);
        return source == null ? null : SourceResolutionResult.forRealTableByName(source, table); 
    }
    
    public abstract SQLQueryRowsSourceModel findRealSource(DBSEntity table);

    public final SQLQueryDataContext overrideResultTuple(List<SQLQueryResultColumn> columns) {
        return new SQLQueryResultTupleContext(this, columns);
    }
    
    public final SQLQueryDataContext combine(SQLQueryDataContext other) {
        return new SQLQueryCombinedContext(this, other);
    }
    
    public final SQLQueryDataContext extendWithRealTable(DBSEntity table, SQLQueryRowsSourceModel source) {
        return new SQLQueryTableRowsContext(this, table, source);
    }

    public final SQLQueryDataContext extendWithTableAlias(SQLQuerySymbol alias, SQLQueryRowsSourceModel source) {
        return new SQLQueryAliasedRowsContext(this, alias, source);
    }
    
    public final SQLQueryDataContext hideSources() {
        return new SQLQueryPureResultTupleContext(this);
    }

    public abstract SQLDialect getDialect();

    @NotNull
    public abstract SQLQueryRowsSourceModel getDefaultTable(@NotNull STMTreeNode syntaxNode);
    
    public class KnownSourcesInfo {
        private final Map<SQLQueryRowsSourceModel, SourceResolutionResult> sources = new HashMap<>();

        public void registerTableReference(SQLQueryRowsSourceModel source, DBSEntity table) {
            if (source instanceof SQLQueryRowsCorrelatedSourceModel cc && cc.getCorrelationColumNames().isEmpty()) {
                source = cc.getSource();
            }
            SQLQueryRowsSourceModel ssource = source;
            this.sources.compute(source, (k, v) -> v == null ? SourceResolutionResult.forRealTableByName(ssource, table) : SourceResolutionResult.withRealTable(v, table));
        }

        public void registerAlias(SQLQueryRowsSourceModel source, SQLQuerySymbol alias) {
            if (source instanceof SQLQueryRowsCorrelatedSourceModel cc && cc.getCorrelationColumNames().isEmpty()) {
                source = cc.getSource();
            }
            SQLQueryRowsSourceModel ssource = source;
            this.sources.compute(source, (k, v) -> v == null ? SourceResolutionResult.forSourceByAlias(ssource, alias) : SourceResolutionResult.withAlias(v, alias));
        }

        public Map<SQLQueryRowsSourceModel, SourceResolutionResult> getResolutionResults() {
            return Collections.unmodifiableMap(this.sources);
        }
    }

    public KnownSourcesInfo getKnownSources() {
        KnownSourcesInfo result = new KnownSourcesInfo();
        this.collectKnownSources(result);
        return result;
    }
    
    protected abstract void collectKnownSources(KnownSourcesInfo result);
}
