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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSVisibilityScopeProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SQLQueryConnectionRealContext extends SQLQueryConnectionContext {
    private static final Log log = Log.getLog(SQLQueryConnectionRealContext.class);

    private final boolean validateFunctions;

    @NotNull
    private final SQLIdentifierDetector identifierDetector;
    @NotNull
    private final DBCExecutionContext executionContext;
    @NotNull
    private final Map<String, SQLQueryResultPseudoColumn> globalPseudoColumnsByCanonicalName;
    @NotNull
    private final Function<SQLQueryRowsSourceModel, List<SQLQueryResultPseudoColumn>> rowsetPseudoColumnsProvider;

    public SQLQueryConnectionRealContext(
        @NotNull SQLDialect dialect,
        @NotNull SQLIdentifierDetector identifierDetector,
        @NotNull DBCExecutionContext executionContext,
        boolean validateFunctions,
        @NotNull Map<String, SQLQueryResultPseudoColumn> globalPseudoColumns,
        @NotNull Function<SQLQueryRowsSourceModel, List<SQLQueryResultPseudoColumn>> rowsetPseudoColumnsProvider
    ) {
        super(dialect);
        this.validateFunctions = validateFunctions;
        this.identifierDetector = identifierDetector;
        this.executionContext = executionContext;
        this.globalPseudoColumnsByCanonicalName = globalPseudoColumns;
        this.rowsetPseudoColumnsProvider = rowsetPseudoColumnsProvider;
    }

    @NotNull
    public List<SQLQueryResultPseudoColumn> obtainRowsetPseudoColumns(@Nullable SQLQueryRowsSourceModel rowsSource) {
        return this.rowsetPseudoColumnsProvider.apply(rowsSource);
    }

    @Override
    public boolean isDummy() {
        return false;
    }

    @NotNull
    @Override
    protected List<? extends DBSObject> findRealObjectsImpl(@NotNull DBRProgressMonitor monitor, @NotNull List<String> objectName) {
        if (this.executionContext.getDataSource() instanceof DBSObjectContainer container) {
            List<? extends DBSObject> objs = SQLSearchUtils.findObjectsByFQN(
                monitor,
                container,
                this.executionContext,
                objectName,
                false,
                this.identifierDetector,
                this.validateFunctions
            );
            if (objs.isEmpty()) {
                DBSVisibilityScopeProvider scopeProvider =
                    DBUtils.getSelectedObject(this.executionContext) instanceof DBSVisibilityScopeProvider currentScope
                        ? currentScope
                        : (this.executionContext.getDataSource() instanceof DBSVisibilityScopeProvider contextScope
                            ? contextScope : null);
                if (scopeProvider != null) {
                    try {
                        for (DBSObjectContainer scope : scopeProvider.getPublicScopes(monitor)) {
                            objs = SQLSearchUtils.findObjectsByFQN(
                                monitor,
                                scope,
                                this.executionContext,
                                objectName,
                                false,
                                this.identifierDetector,
                                this.validateFunctions
                            );
                            if (!objs.isEmpty()) {
                                break;
                            }
                        }
                    } catch (DBException e) {
                        String name = String.join(Character.toString(this.executionContext.getDataSource().getSQLDialect()
                            .getStructSeparator()), objectName);
                        log.error("Failed to resolve real database object " + name, e);
                    }
                }
            }
            return objs;
        } else {
            // Semantic analyser should never be used for databases, which doesn't support table lookup
            // It's managed by LSMDialectRegistry (see org.jkiss.dbeaver.lsm.dialectSyntax extension point)
            // so that analysers could be created only for supported dialects.
            throw new UnsupportedOperationException(
                "Semantic analyser should never be used for databases, which doesn't support table lookup");
        }
    }

    @Nullable
    @Override
    public SQLQueryResultPseudoColumn resolveGlobalPseudoColumn(@NotNull String name) {
        return this.globalPseudoColumnsByCanonicalName.get(name);
    }
}
