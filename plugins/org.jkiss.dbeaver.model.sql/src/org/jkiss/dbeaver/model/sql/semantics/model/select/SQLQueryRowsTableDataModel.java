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
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Describes table definition
 */
public class SQLQueryRowsTableDataModel extends SQLQueryRowsSourceModel implements SQLQuerySymbolDefinition {

    private static final Log log = Log.getLog(SQLQueryRowsTableDataModel.class);
    @Nullable
    private final SQLQueryQualifiedName name;
    @Nullable
    private DBSEntity table = null;

    private final boolean forDdl;

    public SQLQueryRowsTableDataModel(@NotNull STMTreeNode syntaxNode, @Nullable SQLQueryQualifiedName name, boolean forDdl) {
        super(syntaxNode);
        this.name = name;
        this.forDdl = forDdl;
    }

    @Nullable
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
    private static SQLQuerySymbol prepareColumnSymbol(@NotNull SQLQueryDataContext context, @NotNull DBSEntityAttribute attr) {
        String name = SQLUtils.identifierToCanonicalForm(context.getDialect(), attr.getName(), false, true);
        SQLQuerySymbol symbol = new SQLQuerySymbol(name);
        symbol.setDefinition(new SQLQuerySymbolByDbObjectDefinition(attr, SQLQuerySymbolClass.COLUMN));
        return symbol;

    }

    @NotNull
    protected Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> prepareResultColumnsList(
        @NotNull SQLQuerySymbolEntry cause,
        @NotNull SQLQueryDataContext attrsContext,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull List<? extends DBSEntityAttribute> attributes
    ) {
        return prepareResultColumnsList(cause, this, this.table, attrsContext, statistics, attributes);
    }

    @NotNull
    public static Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> prepareResultColumnsList(
        @NotNull SQLQuerySymbolEntry cause,
        @NotNull SQLQueryRowsSourceModel rowsSourceModel,
        @Nullable DBSEntity table,
        @NotNull SQLQueryDataContext attrsContext,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull List<? extends DBSEntityAttribute> attributes
    ) {
        List<SQLQueryResultColumn> columns = new ArrayList<>(attributes.size());
        List<SQLQueryResultPseudoColumn> pseudoColumns = new ArrayList<>(attributes.size());
        for (DBSEntityAttribute attr : attributes) {
            if (DBUtils.isHiddenObject(attr)) {
                pseudoColumns.add(new SQLQueryResultPseudoColumn(
                    prepareColumnSymbol(attrsContext, attr),
                    rowsSourceModel, table, obtainColumnType(cause, statistics, attr), DBDPseudoAttribute.PropagationPolicy.TABLE_LOCAL, attr.getDescription()
                ));
            } else {
                columns.add(new SQLQueryResultColumn(
                    columns.size(),
                    prepareColumnSymbol(attrsContext, attr),
                    rowsSourceModel, table, attr,
                    obtainColumnType(cause, statistics, attr)
                ));
            }
        }
        return Pair.of(columns, pseudoColumns);
    }

    @NotNull
    private static SQLQueryExprType obtainColumnType(
        @NotNull SQLQuerySymbolEntry reason,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull DBSAttributeBase attr
    ) {
        SQLQueryExprType type;
        try {
            type = SQLQueryExprType.forTypedObject(statistics.getMonitor(), attr, SQLQuerySymbolClass.COLUMN);
        } catch (DBException e) {
            log.debug(e);
            statistics.appendError(reason, "Failed to resolve column type for column " + attr.getName(), e);
            type = SQLQueryExprType.UNKNOWN;
        }
        return type;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.name != null) {
            SQLQuerySymbolOrigin rowsetRefOrigin = new SQLQuerySymbolOrigin.RowsetRefFromContext(context);
            if (this.name.invalidPartsCount == 0) {
                if (this.name.isNotClassified()) {
                    List<String> nameStrings = this.name.toListOfStrings();
                    if (nameStrings.size() == 1 && this.name.entityName.getName().equalsIgnoreCase(context.getDialect().getDualTableName())) {
                        this.name.setSymbolClass(SQLQuerySymbolClass.TABLE);
                        // TODO consider pseudocolumns, for example: dual in Oracle has them ?
                        return context.overrideResultTuple(this, Collections.emptyList(), Collections.emptyList());
                    }

                    DBSObject refTarget = context.findRealObject(statistics.getMonitor(), RelationalObjectType.TYPE_UNKNOWN, nameStrings);
                    DBSObject obj = forDdl
                        ? refTarget
                        : SQLQueryDataSourceContext.expandAliases(statistics.getMonitor(), refTarget);

                    this.table = obj instanceof DBSEntity e && (obj instanceof DBSTable || obj instanceof DBSView) ? e : null;

                    if (this.table != null) {
                        this.name.setDefinition(refTarget, rowsetRefOrigin);
                        context = context.extendWithRealTable(this.table, this);

                        try {
                            List<? extends DBSEntityAttribute> attributes = this.table.getAttributes(statistics.getMonitor());
                            if (attributes != null) {
                                Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> columns = prepareResultColumnsList(
                                    this.name.entityName,
                                    context,
                                    statistics,
                                    attributes
                                );
                                List<SQLQueryResultPseudoColumn> inferredPseudoColumns = table instanceof DBDPseudoAttributeContainer pac
                                    ? prepareResultPseudoColumnsList(
                                    context.getDialect(),
                                    this,
                                    this.table,
                                    Stream.of(pac.getAllPseudoAttributes(statistics.getMonitor()))
                                        .filter(a -> a.getPropagationPolicy().providedByTable)
                                ) : Collections.emptyList();
                                List<SQLQueryResultPseudoColumn> pseudoColumns = Stream.of(
                                    columns.getSecond(), inferredPseudoColumns
                                ).flatMap(Collection::stream).collect(Collectors.toList());
                                context = context.overrideResultTuple(this, columns.getFirst(), pseudoColumns);
                            }
                        } catch (DBException ex) {
                            statistics.appendError(
                                this.name.entityName,
                                "Failed to resolve columns of the table " + this.name.toIdentifierString(),
                                ex
                            );
                        }
                    } else {
                        // TODO consider this.name's origin for error cases
                        if (refTarget != null) {
                            String typeName = obj instanceof DBSObjectWithType to
                                ? to.getObjectType().getTypeName()
                                : obj.getClass().getSimpleName();
                            statistics.appendError(this.name.entityName, "Expected table name while given " + typeName);
                        } else {
                            context = context.markHasUnresolvedSource();
                            SourceResolutionResult rr = context.resolveSource(statistics.getMonitor(), nameStrings);
                            if (rr != null && rr.tableOrNull == null && rr.source != null && rr.aliasOrNull != null && nameStrings.size() == 1) {
                                // seems cte reference resolved
                                this.name.entityName.setDefinition(rr.aliasOrNull.getDefinition());
                                context = context.overrideResultTuple(this, rr.source.getResultDataContext().getColumnsList(), Collections.emptyList());
                            } else {
                                SQLQuerySymbolClass tableSymbolClass = statistics.isTreatErrorsAsWarnings()
                                    ? SQLQuerySymbolClass.TABLE
                                    : SQLQuerySymbolClass.ERROR;
                                this.name.setSymbolClass(tableSymbolClass);
                                statistics.appendError(this.name.entityName, "Table " + this.name.toIdentifierString() + " not found");
                            }
                        }
                    }
                }
            } else {
                context = context.overrideResultTuple(this, Collections.emptyList(), Collections.emptyList()).markHasUnresolvedSource();
                SQLQueryQualifiedName.performPartialResolution(context, statistics, this.name, rowsetRefOrigin);
                statistics.appendError(this.getSyntaxNode(), "Invalid table reference");
            }
        } else {
            context = context.overrideResultTuple(this, Collections.emptyList(), Collections.emptyList()).markHasUnresolvedSource();
            statistics.appendError(this.getSyntaxNode(), "Table reference expected");
        }

        return context;
    }

    public static List<SQLQueryResultPseudoColumn> prepareResultPseudoColumnsList(
        @NotNull SQLDialect dialect,
        @Nullable SQLQueryRowsSourceModel source,
        @Nullable DBSEntity table,
        @NotNull Stream<DBDPseudoAttribute> pseudoAttributes
    ) {
        return pseudoAttributes.map(a -> new SQLQueryResultPseudoColumn(
            new SQLQuerySymbol(SQLUtils.identifierToCanonicalForm(dialect, a.getName(), false, false)),
            source, table, SQLQueryExprType.UNKNOWN, a.getPropagationPolicy(), a.getDescription()
        )).collect(Collectors.toList());
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsTableData(this, arg);
    }
}