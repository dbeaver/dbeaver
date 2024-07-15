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
package org.jkiss.dbeaver.model.sql.semantics.model.select;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.ArrayList;
import java.util.List;


/**
 * Describes table definition
 */
public class SQLQueryRowsTableDataModel extends SQLQueryRowsSourceModel implements SQLQuerySymbolDefinition {

    private static final Log log = Log.getLog(SQLQueryRowsTableDataModel.class);
    @NotNull
    private final SQLQueryQualifiedName name;
    @Nullable
    private DBSEntity table = null;

    public SQLQueryRowsTableDataModel(SQLQueryModelContext context, @NotNull STMTreeNode syntaxNode, @NotNull SQLQueryQualifiedName name) {
        super(context, syntaxNode);
        this.name = name;
    }

    @NotNull
    public SQLQueryQualifiedName getName() {
        return this.name;
    }

    @Nullable
    public DBSEntity getTable() {
        return this.table;
    }

    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return this.table != null ? SQLQuerySymbolClass.TABLE : SQLQuerySymbolClass.ERROR;
    }

    @NotNull
    private SQLQuerySymbol prepareColumnSymbol(@NotNull SQLQueryDataContext context, @NotNull DBSEntityAttribute attr) {
        String name = SQLUtils.identifierToCanonicalForm(context.getDialect(), attr.getName(), false, true);
        SQLQuerySymbol symbol = new SQLQuerySymbol(name);
        symbol.setDefinition(new SQLQuerySymbolByDbObjectDefinition(attr, SQLQuerySymbolClass.COLUMN));
        return symbol;

    }

    @NotNull
    protected List<SQLQueryResultColumn> prepareResultColumnsList(
        @NotNull SQLQuerySymbolEntry cause,
        @NotNull SQLQueryDataContext attrsContext,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull List<? extends DBSEntityAttribute> attributes
    ) {
        List<SQLQueryResultColumn> columns = new ArrayList<>(attributes.size());
        for (DBSEntityAttribute attr : attributes) {
            if (!DBUtils.isHiddenObject(attr)) {
                columns.add(new SQLQueryResultColumn(
                    columns.size(),
                    this.prepareColumnSymbol(attrsContext, attr),
                    this, this.table, attr,
                    obtainColumnType(cause, statistics, attr)
                ));
            }
        }
        return columns;
    }

    @NotNull
    private SQLQueryExprType obtainColumnType(
        @NotNull SQLQuerySymbolEntry reason,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull DBSAttributeBase attr
    ) {
        SQLQueryExprType type;
        try {
            type = SQLQueryExprType.forTypedObject(statistics.getMonitor(), attr, SQLQuerySymbolClass.COLUMN);
        } catch (DBException e) {
            log.debug(e);
            statistics.appendError(reason, "Failed to resolve column type", e);
            type = SQLQueryExprType.UNKNOWN;
        }
        return type;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.name.isNotClassified()) {
            List<String> nameStrings = this.name.toListOfStrings();
            this.table = context.findRealTable(statistics.getMonitor(), nameStrings);

            if (this.table != null) {
                this.name.setDefinition(table);
                context = context.extendWithRealTable(this.table, this);
                try {
                    List<? extends DBSEntityAttribute> attributes = this.table.getAttributes(statistics.getMonitor());
                    if (attributes != null) {
                        List<SQLQueryResultColumn> columns = this.prepareResultColumnsList(
                            this.name.entityName,
                            context,
                            statistics,
                            attributes
                        );
                        context = context.overrideResultTuple(columns);
                    }
                } catch (DBException ex) {
                    statistics.appendError(this.name.entityName, "Failed to resolve table", ex);
                }
            } else {
                SourceResolutionResult rr = context.resolveSource(statistics.getMonitor(), nameStrings);
                if (rr != null && rr.tableOrNull == null && rr.source != null && rr.aliasOrNull != null && nameStrings.size() == 1) {
                    // seems cte reference resolved
                    this.name.entityName.setDefinition(rr.aliasOrNull.getDefinition());
                    context = context.overrideResultTuple(rr.source.getResultDataContext().getColumnsList());
                } else {
                    this.name.setSymbolClass(SQLQuerySymbolClass.ERROR);
                    statistics.appendError(this.name.entityName, "Table not found");
                }
            }
        }
        return context;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsTableData(this, arg);
    }
}