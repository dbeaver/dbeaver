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
package org.jkiss.dbeaver.model.sql.semantics.model.expressions;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Describes a certain value in the query value expression represented with a compound name
 */
public class SQLQueryValueReferenceExpression extends SQLQueryValueExpression {

    // change to Map<DBSObjectType, Function<DBSObject, SQLQueryExprType>> in need of multiple allowed object types
    private static final DBSObjectType ALLOWED_OBJECT_TYPE = RelationalObjectType.TYPE_SEQUENCE;
    private static final SQLQueryExprType ALLOWED_OBJECT_EXPR_TYPE = SQLQueryExprType.NUMERIC;

    private static final Log log = Log.getLog(SQLQueryValueReferenceExpression.class);

    private final boolean rowRefAllowed;

    @Nullable
    private final SQLQueryComplexName name;

    @Nullable
    private SQLQueryResultColumn column = null;

    public SQLQueryValueReferenceExpression(
        @NotNull STMTreeNode syntaxNode,
        boolean rowRefAllowed,
        @Nullable SQLQueryComplexName name
    ) {
        super(syntaxNode);
        this.rowRefAllowed = rowRefAllowed;
        this.name = name;
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

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueReferenceExpr(this, arg);
    }

    @Nullable
    public SQLQuerySymbolEntry getColumnName() {
        return this.name == null || this.name.parts.isEmpty() || this.name.parts.getLast() == null ? null : this.name.parts.getLast();
    }

    @Nullable
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.name == null || this.name.parts.isEmpty() || this.name.parts.getLast() == null
            ? null
            : this.name.parts.getLast().getSymbol();
    }

    @Nullable
    @Override
    public SQLQueryResultColumn getColumnIfTrivialExpression() {
        return this.column;
    }

    @Override
    protected void resolveRowSourcesImpl(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
    }

    @NotNull
    @Override
    protected SQLQueryExprType resolveValueTypeImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryExprType type;
        SQLQueryResultColumn resultColumn;
        SQLQueryResultPseudoColumn resultPseudoColumn;
        SQLQuerySymbolEntry columnRefEntry;
        SQLQueryRowsDataContext columnContext;
        SQLQueryRowsSourceContext.SourceResolutionInfo tableRef;

        if (this.name != null) {
            SQLQuerySymbolOrigin columnRefOrigin = new SQLQuerySymbolOrigin.RowsDataRef(context);
            List<SQLQuerySymbolEntry> restParts = this.name.parts;
            if (this.name.invalidPartsCount > 0) {
                int invalidPartIndex = restParts.indexOf(null);
                restParts = restParts.subList(0, invalidPartIndex);
            }

            // 1: match for suitable reference target
            // 1.1: resolve as prefixless column
            if (restParts.size() > 0) {
                columnContext = context;
                columnRefEntry = restParts.getFirst();
                resultPseudoColumn = context.getConnection().resolveGlobalPseudoColumn(columnRefEntry.getName());
                if (resultPseudoColumn == null) {
                    resultPseudoColumn = context.resolvePseudoColumn(columnRefEntry.getName());
                }
                if (resultPseudoColumn == null) {
                    resultColumn = context.resolveColumn(statistics.getMonitor(), columnRefEntry.getName());
                } else {
                    resultColumn = null;
                }
            } else {
                columnContext = null;
                columnRefEntry = null;
                resultPseudoColumn = null;
                resultColumn = null;
            }

            if ((resultColumn == null && resultPseudoColumn == null) || this.name.parts.size() > 1) {
                // 1.2: try to resolve rowset reference prefix and classify it if resolved
                tableRef = context.getRowsSources().findReferencedSource(this.name);
                if (tableRef != null) {
                    columnContext = tableRef.target().source.getRowsDataContext();
                    restParts = this.name.parts.subList(tableRef.key().parts.size(), restParts.size());
                    SQLQuerySemanticUtils.setNamePartsDefinition(tableRef.key(), tableRef.target(), columnRefOrigin);
                    columnRefOrigin = new SQLQuerySymbolOrigin.ColumnRefFromReferencedContext(tableRef.target());
                    // resolve rowset's column
                    if (!restParts.isEmpty()) {
                        columnRefEntry = restParts.getFirst();
                        restParts = restParts.subList(1, restParts.size());
                        if (resultPseudoColumn == null) {
                            resultPseudoColumn = columnContext.resolvePseudoColumn(columnRefEntry.getName());
                        }
                        if (resultPseudoColumn == null) {
                            resultColumn = columnContext.resolveColumn(statistics.getMonitor(), columnRefEntry.getName());
                        }
                    } else {
                        columnRefEntry = null;
                        resultColumn = null;
                    }
                }
            } else {
                tableRef = null;
            }

            List<? extends DBSObject> dbObjects;
            DBSObject dbObject;
            SQLQuerySymbolClass forcedClass;
            if (resultColumn == null && resultPseudoColumn == null && tableRef == null) {
                // 1.3: no columns and no rowsets, so try for db objects
                if (context.getConnection().isDummy()) {
                    // no real database - no point to treat any random name as object
                    dbObjects = Collections.emptyList();
                } else if (this.name.invalidPartsCount == 0) {
                    dbObjects = context.getConnection().findRealObjects(
                        statistics.getMonitor(),
                        ALLOWED_OBJECT_TYPE,
                        this.name.stringParts
                    );
                } else {
                    dbObjects = Collections.emptyList();
                }
                dbObject = dbObjects.size() == 1 ? dbObjects.getFirst() : null;
                if (dbObjects.size() > 1) {
                    forcedClass = null;
                } else if (dbObject == null && this.name.parts.size() == 1 && this.name.invalidPartsCount == 0) {
                    // 1.4: no objects also, so failing back to default classification
                    SQLQuerySymbolEntry stringEntry = this.name.parts.getFirst();
                    String rawString = stringEntry.getRawName();
                    SQLDialect dialect = context.getConnection().dialect;
                    if (dialect.isQuotedString(rawString)) {
                        forcedClass = SQLQuerySymbolClass.STRING;
                    } else {
                        forcedClass = SQLQueryModelRecognizer.tryFallbackSymbolForStringLiteral(dialect, stringEntry, false);
                    }
                } else {
                    forcedClass = null;
                }
            } else {
                dbObjects = Collections.emptyList();
                dbObject = null;
                forcedClass = null;
            }

            // 2: final classification applications based on findings
            // 2.1: last-chance findings are the simplest
            if (forcedClass != null) {
                this.name.parts.getFirst().getSymbol().setSymbolClass(forcedClass);
                type = forcedClass == SQLQuerySymbolClass.STRING ? SQLQueryExprType.STRING : SQLQueryExprType.UNKNOWN;
            } else if (dbObject != null) {
                // TODO consider bringing DB objects like sequences to the autocompletion proposals
                SQLQuerySymbolClass objClass;
                if (ALLOWED_OBJECT_TYPE.getTypeClass().isAssignableFrom(dbObject.getClass())) {
                    objClass = SQLQuerySymbolClass.OBJECT;
                    type = ALLOWED_OBJECT_EXPR_TYPE;
                } else {
                    objClass = SQLQuerySymbolClass.ERROR;
                    type = SQLQueryExprType.UNKNOWN;

                    String typeName = SQLQuerySemanticUtils.getObjectTypeName(dbObject);
                    statistics.appendError(this.name.syntaxNode, "Illegal database object reference " + typeName);
                }
                SQLQuerySemanticUtils.setNamePartsDefinition(this.name, dbObject, objClass, new SQLQuerySymbolOrigin.RowsDataRef(context));
            } else if (dbObjects.size() > 1) {
                type = SQLQueryExprType.UNKNOWN;
                SQLQuerySemanticUtils.performPartialResolution(
                    context.getRowsSources(),
                    statistics,
                    this.name,
                    columnRefOrigin,
                    Set.of(ALLOWED_OBJECT_TYPE),
                    SQLQuerySymbolClass.OBJECT
                );
            } else {
                type = null;
            }

            if (type == null) {
                if (columnRefEntry != null) {
                    if (columnContext.getRowsSources().hasUnresolvedSource() && resultColumn == null && resultPseudoColumn == null) {
                        // do nothing more and don't generate errors on failed column resolutions while unresolved sources presented
                        type = SQLQueryExprType.UNKNOWN;
                        columnRefEntry.setOrigin(columnRefOrigin);
                    } else {
                        // 2.2: apply column classification
                        if (resultPseudoColumn != null) {
                            resultColumn = null; // not a real column, so we don't need to propagate its source and don't have real entity attribute
                            type = resultPseudoColumn.type;
                            columnRefEntry.setDefinition(resultPseudoColumn);
                            columnRefEntry.setOrigin(columnRefOrigin);
                        } else {
                            SQLQuerySemanticUtils.propagateColumnDefinition(columnRefEntry, resultColumn, statistics, columnRefOrigin);
                            type = resultColumn != null ? resultColumn.type : SQLQueryExprType.UNKNOWN;
                        }
                        if (resultColumn != null || resultPseudoColumn != null) {
                            // try on composite members
                            if (restParts.size() > 1) {
                                SQLQuerySymbolOrigin memberOrigin = new SQLQuerySymbolOrigin.MemberOfType(type);
                                for (int i = 1; i < restParts.size() && type != null && restParts.get(i) != null; i++) {
                                    type = SQLQuerySemanticUtils.tryResolveMemberReference(
                                        statistics,
                                        type,
                                        restParts.get(i),
                                        memberOrigin
                                    );
                                    memberOrigin = new SQLQuerySymbolOrigin.MemberOfType(type);
                                }
                                if (type == null) {
                                    type = SQLQueryExprType.UNKNOWN;
                                }
                            }
                        }
                    }
                } else {
                    // 2.3: no column reference (if rowset-ref presented - it is already classified), so decide on column-related errors
                    resultColumn = null;
                    if (tableRef != null) {
                        SQLQueryExprType rowType = SQLQueryExprType.forReferencedRow(tableRef.key(), tableRef.target());
                        if (this.name.endingPeriodNode == null && tableRef.key().endingPeriodNode == null) {
                            // TODO see the issue about postgre referencing tuples as composite types, maybe introduce a dialect parameter
                            type = rowType;
                        } else  {
                            type = SQLQueryExprType.UNKNOWN;
                            statistics.appendError(this.name.syntaxNode, "Incomplete column reference");
                            if (tableRef.key().endingPeriodNode != null) {
                                tableRef.key().endingPeriodNode.setOrigin(new SQLQuerySymbolOrigin.MemberOfType(rowType));
                            }
                        }
                    } else {
                        type = SQLQueryExprType.UNKNOWN;
                        statistics.appendError(this.name.syntaxNode, "Illegal column reference");
                        if (!this.name.parts.isEmpty() && this.name.parts.getFirst() != null && this.name.parts.getFirst().getOrigin() == null) {
                            this.name.parts.getFirst().setOrigin(new SQLQuerySymbolOrigin.RowsDataRef(context));
                        }
                    }
                }
            }
        } else {
            statistics.appendError(this.getSyntaxNode(), "Invalid column reference");
            resultColumn = null;
            type = SQLQueryExprType.UNKNOWN;
        }

        this.column = resultColumn;
        return type;
    }

    @Override
    public String toString() {
        String name = this.name == null ? "<NULL>" : this.name.getNameString();
        String type = this.type == null ? "<NULL>" : this.type.toString();
        return "ValueReference[" + name + ":" + type + "]";
    }
}
