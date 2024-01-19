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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsTableValueModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents underlying database context having real tables
 */
public class SQLQueryDataSourceContext extends SQLQueryDataContext {
    private final DBCExecutionContext executionContext;
    private final SQLDialect dialect;
    private final SQLIdentifierDetector identifierDetector;

    public SQLQueryDataSourceContext(@NotNull DBCExecutionContext executionContext, @NotNull SQLDialect dialect) {
        this.executionContext = executionContext;
        this.dialect = dialect;
        this.identifierDetector = new SQLIdentifierDetector(dialect);
    }

    @NotNull
    @Override
    public List<SQLQueryResultColumn> getColumnsList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public DBSEntity findRealTable(@NotNull List<String> tableName) {
        if (this.executionContext.getDataSource() instanceof DBSObjectContainer container) {
            List<String> tableName2 = new ArrayList<>(tableName);
            DBSObject obj = SQLSearchUtils.findObjectByFQN(
                new VoidProgressMonitor(),
                container,
                this.executionContext,
                tableName2,
                false,
                identifierDetector
            );
            return obj instanceof DBSTable table ? table : (obj instanceof DBSView view ? view : null);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Nullable
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return null;
    }

//    @Override
//    public SQLQuerySymbolDefinition resolveColumn(List<String> tableName, String columnName) {
//        if (tableName.size() == 0) {
//            return null;
//        } else {
//            SQLQueryRowsSource table = this.resolveSource(tableName);
//            return table.getDataContext().resolveColumn(columnName);
//        }
//    }

    @Nullable
    @Override
    public SQLQueryResultColumn resolveColumn(@NotNull String simpleName) {
        return null;
    }

    @NotNull
    @Override
    public SQLDialect getDialect() {
        return this.dialect;
    }

    @NotNull
    @Override
    public SQLQueryRowsSourceModel getDefaultTable(@NotNull Interval range) {
        return new SQLQueryRowsTableValueModel(range);
    }
}
