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
package org.jkiss.dbeaver.model.sql.semantics.model.select;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.utils.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Describes table definition
 */
public class SQLQueryRowsTableDataModel extends SQLQueryRowsSourceModel
    implements SQLQuerySymbolDefinition, SQLQueryNodeModel.NodeSubtreeTraverseControl<SQLQueryRowsSourceModel, SQLQueryRowsDataContext> {

    private static final Log log = Log.getLog(SQLQueryRowsTableDataModel.class);

    @Nullable
    private final SQLQueryComplexName name;
    @Nullable
    private DBSEntity table = null;

    private final boolean forDdl;

    @Nullable
    protected SQLQueryRowsSourceModel referencedSource = null;

    public SQLQueryRowsTableDataModel(@NotNull STMTreeNode syntaxNode, @Nullable SQLQueryComplexName name, boolean forDdl) {
        super(syntaxNode);
        this.name = name;
        this.forDdl = forDdl;
    }

    @Nullable
    @Override
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return SQLQuerySemanticUtils.getIdentifierSymbolClass(this.name);
    }

    @Nullable
    public SQLQueryComplexName getName() {
        return this.name;
    }

    @Nullable
    public DBSEntity getTable() {
        return this.table;
    }

    @Nullable
    public SQLQueryRowsSourceModel getReferencedSource() {
        return this.referencedSource;
    }

    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return this.table != null ? SQLQuerySymbolClass.TABLE : SQLQuerySymbolClass.ERROR;
    }

    @NotNull
    protected Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> prepareResultColumnsList(
        @NotNull STMTreeNode cause,
        @NotNull SQLDialect dialect,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull List<? extends DBSEntityAttribute> attributes
    ) {
        return SQLQuerySemanticUtils.prepareResultColumnsList(cause, this, this.table, dialect, statistics, attributes);
    }

    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQuerySymbolOrigin rowsetRefOrigin = new SQLQuerySymbolOrigin.RowsSourceRef(context);
        if (this.name == null) {
            statistics.appendError(this.getSyntaxNode(), "Invalid table reference");
            return context.resetAsUnresolved();
        }
        if (this.name.invalidPartsCount > 0) {
            SQLQuerySemanticUtils.performPartialResolution(
                context,
                statistics,
                this.name,
                rowsetRefOrigin,
                SQLQuerySymbolOrigin.DbObjectFilterMode.ROWSET,
                SQLQuerySymbolClass.ERROR
            );
            statistics.appendError(this.getSyntaxNode(), "Invalid table reference");
            return context.resetAsUnresolved();
        }

        if (this.name.parts.size() == 1) {
            if (this.name.stringParts.getLast().equalsIgnoreCase(context.getDialect().getDualTableName())) {
                this.name.parts.getLast().getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE);
                return context.reset();
            } else {
                SourceResolutionResult dynamicSource = context.findDynamicRowsSource(this.name.parts.getFirst());
                if (dynamicSource != null) {
                    this.referencedSource = dynamicSource.source;
                    SQLQueryComplexName referenceName = dynamicSource.referenceName;
                    if (referenceName != null) {
                        this.name.parts.getFirst().setDefinition(referenceName.parts.getLast());
                        this.name.parts.getFirst().setOrigin(rowsetRefOrigin);
                    }
                    return context.reset().appendSource(this, name, null);
                }
            }
        }

        List<? extends DBSObject> candidates = context.getConnectionInfo().findRealObjects(
            statistics.getMonitor(),
            RelationalObjectType.TYPE_UNKNOWN,
            this.name.stringParts
        );
        DBSObject refTarget = candidates.size() == 1 ? candidates.getFirst() : null;
        DBSObject obj = forDdl ? refTarget : SQLQueryConnectionContext.expandAliases(statistics.getMonitor(), refTarget);

        this.table = obj instanceof DBSEntity e && (obj instanceof DBSTable || obj instanceof DBSView) ? e : null;

        if (this.table != null) {
            SQLQuerySemanticUtils.setNamePartsDefinition(
                context, this.name, refTarget, SQLQuerySymbolClass.TABLE, rowsetRefOrigin, SQLQuerySymbolOrigin.DbObjectFilterMode.ROWSET
            );
            context = context.reset().appendSource(this, name, this.table);
        } else {
            SQLQuerySymbolClass tableSymbolClass = statistics.isTreatErrorsAsWarnings()
                ? SQLQuerySymbolClass.TABLE
                : SQLQuerySymbolClass.ERROR;
            SQLQuerySemanticUtils.performPartialResolution(
                context,
                statistics,
                this.name,
                rowsetRefOrigin,
                SQLQuerySymbolOrigin.DbObjectFilterMode.ROWSET,
                tableSymbolClass
            );
            context = context.resetAsUnresolved();
            if (candidates.isEmpty() || candidates.size() == 1) {
                statistics.appendError(this.name.syntaxNode, "Table " + this.name.getNameString() + " not found");
            }
        }
        return context;
    }

    @Nullable
    @Override
    public List<SQLQueryNodeModel> getChildren() {
        return this.referencedSource == null ? null : List.of(this.referencedSource);
    }

    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsDataContext result;
        if (this.table != null && this.name != null) {
            try {
                List<? extends DBSEntityAttribute> attributes = this.table.getAttributes(statistics.getMonitor());
                if (attributes != null) {
                    Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> columns = prepareResultColumnsList(
                        this.name.syntaxNode,
                        this.getRowsSources().getDialect(),
                        statistics,
                        attributes
                    );
                    List<SQLQueryResultPseudoColumn> inferredPseudoColumns = table instanceof DBDPseudoAttributeContainer pac
                        ? SQLQuerySemanticUtils.prepareResultPseudoColumnsList(
                            this.getRowsSources().getDialect(),
                            this,
                            this.table,
                            Stream.of(pac.getAllPseudoAttributes(statistics.getMonitor()))
                                .filter(a -> a.getPropagationPolicy().providedByTable)
                        )
                        : Collections.emptyList();
                    List<SQLQueryResultPseudoColumn> pseudoColumns = Stream.of(
                        columns.getSecond(), inferredPseudoColumns
                    ).flatMap(Collection::stream).collect(Collectors.toList());
                    result = this.getRowsSources().makeTuple(this, columns.getFirst(), pseudoColumns);
                } else {
                    result = this.getRowsSources().makeEmptyTuple();
                }
            } catch (DBException ex) {
                result = this.getRowsSources().makeEmptyTuple();
                statistics.appendError(
                    this.name.syntaxNode,
                    "Failed to resolve columns of the table " + this.name.getNameString(),
                    ex
                );
            }
        } else if (this.referencedSource != null) {
            if (this.referencedSource.isResolved()) {
                SQLQueryRowsDataContext referencedData = this.referencedSource.getRowsDataContext();
                List<SQLQueryResultColumn> resultColumns = referencedData.getColumnsList().stream()
                    .map(c -> c.withNewSource(this))
                    .toList();
                result = this.getRowsSources().makeTuple(this, resultColumns, Collections.emptyList());
            } else {
                statistics.appendError(this.name.syntaxNode, "Circular dependency detected at " + this.name.getNameString());
                result = this.getRowsSources().makeTuple(this, Collections.emptyList(), Collections.emptyList());
            }
        } else {
            result = this.getRowsSources().makeEmptyTuple();
        }
        return result;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsTableData(this, arg);
    }
}