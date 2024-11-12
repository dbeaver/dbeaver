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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents underlying database context having real tables
 */
public class SQLQueryDataSourceContext extends SQLQueryDataContext {
    private static final Log log = Log.getLog(SQLQueryDataSourceContext.class);

    @NotNull
    private final SQLDialect dialect;
    @NotNull
    private final DBCExecutionContext executionContext;
    @NotNull
    private final SQLIdentifierDetector identifierDetector;
    @NotNull
    private final Map<String, SQLQueryResultPseudoColumn> globalPseudoColumnsByCanonicalName;
    @NotNull
    private final Function<SQLQueryRowsSourceModel, List<SQLQueryResultPseudoColumn>> rowsetPseudoColumnsProvider;

    public SQLQueryDataSourceContext(
        @NotNull SQLDialect dialect,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Map<String, SQLQueryResultPseudoColumn> globalPseudoColumns,
        @NotNull Function<SQLQueryRowsSourceModel, List<SQLQueryResultPseudoColumn>> rowsetPseudoColumnsProvider
    ) {
        this.dialect = dialect;
        this.executionContext = executionContext;
        this.identifierDetector = new SQLIdentifierDetector(dialect);
        this.globalPseudoColumnsByCanonicalName = globalPseudoColumns;
        this.rowsetPseudoColumnsProvider = rowsetPseudoColumnsProvider;
    }

    @NotNull
    @Override
    public List<SQLQueryResultColumn> getColumnsList() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasUndresolvedSource() {
        return false;
    }


    @NotNull
    @Override
    public List<SQLQueryResultPseudoColumn> getPseudoColumnsList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public DBSEntity findRealTable(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        // TODO consider differentiating direct references vs expanded aliases: each alias expansion should be treated as a virtual table
        DBSObject obj = expandAliases(monitor, this.findRealObjectImpl(monitor, tableName));
        return obj instanceof DBSTable table ? table : (obj instanceof DBSView view ? view : null);
    }

    @Nullable
    @Override
    public DBSObject findRealObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectType objectType,
        @NotNull List<String> objectName
    ) {
        DBSObject obj = this.findRealObjectImpl(monitor, objectName);
        return obj != null && objectType.getTypeClass().isInstance(obj) ? obj : null;
    }

    @Nullable
    private DBSObject findRealObjectImpl(@NotNull DBRProgressMonitor monitor, @NotNull List<String> objectName) {
        if (this.executionContext.getDataSource() instanceof DBSObjectContainer container) {
            DBSObject obj = SQLSearchUtils.findObjectByFQN(
                monitor,
                container,
                this.executionContext,
                objectName,
                false,
                identifierDetector
            );
            if (obj == null) {
                DBSVisibilityScopeProvider scopeProvider = DBUtils.getSelectedObject(this.executionContext) instanceof DBSVisibilityScopeProvider currentScope
                    ? currentScope
                    : (this.executionContext.getDataSource() instanceof DBSVisibilityScopeProvider contextScope ? contextScope : null);
                if (scopeProvider != null) {
                    try {
                        for (DBSObjectContainer scope : scopeProvider.getPublicScopes(monitor)) {
                            obj = SQLSearchUtils.findObjectByFQN(monitor, scope, this.executionContext, objectName, false, identifierDetector);
                            if (obj != null) {
                                break;
                            }
                        }
                    } catch (DBException e) {
                        String name = String.join(Character.toString(this.executionContext.getDataSource().getSQLDialect().getStructSeparator()), objectName);
                        log.error("Failed to resolve real database object " + name, e);
                    }
                }
            }
            return obj;
        } else {
            // Semantic analyser should never be used for databases, which doesn't support table lookup
            // It's managed by LSMDialectRegistry (see org.jkiss.dbeaver.lsm.dialectSyntax extension point)
            // so that analyzers could be created only for supported dialects.
            throw new UnsupportedOperationException("Semantic analyser should never be used for databases, which doesn't support table lookup");
        }
    }

    /**
     * Resolve target object for alias
     */
    @Nullable
    public static DBSObject expandAliases(@NotNull DBRProgressMonitor monitor, @Nullable DBSObject obj) {
        while (obj instanceof DBSAlias aliasObject) {
            try {
                obj = aliasObject.getTargetObject(monitor);
            } catch (DBException e) {
                obj = null;
                log.debug("Can't resolve target object for alias '" + aliasObject.getName() + "'", e);
            }
        }
        return obj;
    }

    @Nullable
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String simpleName) {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryResultPseudoColumn resolvePseudoColumn(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryResultPseudoColumn resolveGlobalPseudoColumn(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        return this.globalPseudoColumnsByCanonicalName.get(name);
    }

    @NotNull
    @Override
    public SQLDialect getDialect() {
        return this.dialect;
    }

    @Override
    protected void collectKnownSourcesImpl(@NotNull KnownSourcesInfo result) {
        // no sources have been referenced yet, so nothing to register
    }

    @Override
    protected List<SQLQueryResultPseudoColumn> prepareRowsetPseudoColumns(@NotNull SQLQueryRowsSourceModel source) {
        return this.rowsetPseudoColumnsProvider.apply(source);
    }
}
