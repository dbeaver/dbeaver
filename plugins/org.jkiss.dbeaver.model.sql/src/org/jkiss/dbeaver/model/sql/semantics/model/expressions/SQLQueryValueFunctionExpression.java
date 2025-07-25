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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryConnectionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class SQLQueryValueFunctionExpression extends SQLQueryValueExpression {
    @Nullable
    private final SQLQueryComplexName procName;
    @NotNull
    private final List<SQLQueryValueExpression> operands;

    @Nullable
    private DBSProcedure procedure = null;

    private boolean forRows;

    public SQLQueryValueFunctionExpression(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryComplexName procName,
        @NotNull List<SQLQueryValueExpression> operands,
        boolean forRows
    ) {
        super(syntaxNode, operands.toArray(SQLQueryValueExpression[]::new));
        this.procName =  procName;
        this.operands = operands;
        this.forRows = forRows;
    }

    @Nullable
    @Override
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return SQLQuerySemanticUtils.getIdentifierSymbolClass(this.procName);
    }

    @Nullable
    public SQLQueryComplexName getProcName() {
        return this.procName;
    }

    @Nullable
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.procName != null && this.procName.parts.getLast() != null
            ? this.procName.parts.getLast().getSymbol()
            : new SQLQuerySymbol("?");
    }

    @NotNull
    public List<SQLQueryValueExpression> getOperands() {
        return this.operands;
    }

    @Override
    protected void resolveRowSourcesImpl(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
        for (SQLQueryValueExpression expr : this.operands) {
            expr.resolveRowSources(context, statistics);
        }
    }

    @NotNull
    @Override
    protected SQLQueryExprType resolveValueTypeImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.procName == null) {
            statistics.appendError(this.getSyntaxNode(), "Invalid function reference");
            return SQLQueryExprType.UNKNOWN;
        }

        SQLQuerySymbolOrigin origin = this.forRows
            ? new SQLQuerySymbolOrigin.RowsSourceRef(context.getRowsSources())
            : new SQLQuerySymbolOrigin.RowsDataRef(context);

        if (this.procName.invalidPartsCount > 0) {
            SQLQuerySemanticUtils.performPartialResolution(
                context.getRowsSources(),
                statistics,
                this.procName,
                origin,
                Set.of(RelationalObjectType.TYPE_UNKNOWN),
                SQLQuerySymbolClass.ERROR
            );
            statistics.appendError(this.getSyntaxNode(), "Invalid function reference");
            return SQLQueryExprType.UNKNOWN;
        }

        if (context.getConnection().isDummy()) {
            this.procName.parts.forEach(p -> p.getSymbol().setSymbolClass(SQLQuerySymbolClass.FUNCTION));
            return SQLQueryExprType.UNKNOWN;
        }

        if (!statistics.validateFunctions()) {
            SQLQuerySemanticUtils.performPartialResolution(
                context.getRowsSources(),
                statistics,
                this.procName,
                origin,
                Set.of(RelationalObjectType.TYPE_UNKNOWN),
                SQLQuerySymbolClass.FUNCTION
            );
            return SQLQueryExprType.UNKNOWN;
        }

        if (this.procName.parts.size() == 1) {
            SQLQuerySymbolEntry name = this.procName.parts.getFirst();
            if (context.getConnection().dialect.getFunctions().contains(name.getName())) {
                name.getSymbol().setSymbolClass(SQLQuerySymbolClass.FUNCTION);
                name.setOrigin(origin);
                return SQLQueryExprType.UNKNOWN;
            }
        }

        List<? extends DBSObject> candidates = context.getConnection().findRealObjects(
            statistics.getMonitor(),
            RelationalObjectType.TYPE_PROCEDURE,
            this.procName.stringParts
        );

        List<CandidateProcedure> procs = new ArrayList<>(candidates.size());
        try {
            for (DBSObject candidate : candidates) {
                DBSObject targetObj = SQLQueryConnectionContext.expandAliases(statistics.getMonitor(), candidate);
                if (targetObj instanceof DBSProcedure p) {
                    procs.add(prepareFunctionApplication(this, candidate, p, statistics));
                }
            }
        } catch (DBException ex) {
            statistics.appendError(this.procName.syntaxNode, "Failed to obtain function information", ex);
            return SQLQueryExprType.UNKNOWN;
        }

        CandidateProcedure proc;
        if (procs.isEmpty()) {
            proc = null;
        } else if (procs.size() == 1) {
            proc = procs.getFirst();
        } else {
            CandidateProcedure firstMatch = procs.stream().filter(p -> CommonUtils.isEmpty(p.validationErrors)).findFirst().orElse(null);
            long totalOkMatches = procs.stream().filter(p -> CommonUtils.isEmpty(p.validationErrors)).count();
            if (procs.stream().allMatch(CandidateProcedure::checked)) {
                if (firstMatch != null && totalOkMatches == 1) {
                    proc = firstMatch; // only one overload with no errors, take it
                } else {
                    proc = null; // cannot decide, so ignore them all
                }
            } else {
                proc = null;
            }
            if (proc == null) {
                this.procName.parts.getLast().getSymbol().setSymbolClass(SQLQuerySymbolClass.FUNCTION);
                if (this.procName.parts.size() > 1) {
                    SQLQuerySemanticUtils.performPartialResolution(
                        context.getRowsSources(),
                        statistics,
                        this.procName.trimEnd(),
                        origin,
                        Set.of(RelationalObjectType.TYPE_UNKNOWN),
                        SQLQuerySymbolClass.FUNCTION
                    );
                }
                return SQLQueryExprType.UNKNOWN;
            }
        }

        this.procedure = proc == null ? null : proc.procedure;

        if (this.procedure != null) {
            SQLQuerySemanticUtils.setNamePartsDefinition(this.procName, proc.referenceTarget, SQLQuerySymbolClass.FUNCTION, origin);

            if (proc.validationErrors != null) {
                proc.validationErrors.forEach(Runnable::run);
            }

            SQLQueryExprType resultType = null;
            try {
                if (proc.results != null) {
                    if (proc.results.size() == 1) {
                        resultType = SQLQueryExprType.forTypedObject(
                            statistics.getMonitor(),
                            proc.results.getFirst().getParameterType(),
                            SQLQuerySymbolClass.FUNCTION
                        );
                    } else if (proc.results.size() > 1 && proc.results.getFirst().getParameterKind() == DBSProcedureParameterKind.TABLE) {
                        Map<String, SQLQueryExprType> fields = new HashMap<>(proc.results.size());
                        for (DBSProcedureParameter ret : proc.results) {
                            SQLQueryExprType fieldType = SQLQueryExprType.forTypedObject(
                                statistics.getMonitor(),
                                ret.getParameterType(),
                                SQLQuerySymbolClass.COMPOSITE_FIELD
                            );
                            fields.put(ret.getName(), fieldType);
                        }
                        SQLQuerySymbolDefinition declarator = new SQLQuerySymbolByDbObjectDefinition(
                            this.procedure,
                            SQLQuerySymbolClass.FUNCTION
                        );
                        resultType = SQLQueryExprType.forSynthesizedArray(
                            this.procName.getNameString() + ":resultTable",
                            declarator,
                            SQLQueryExprType.forSynthesizedComposite(
                                this.procName.getNameString() + ":resultRow",
                                this.procedure.getDataSource(),
                                declarator,
                                fields
                            )
                        );
                    }
                }
                if (resultType == null && proc.returnType != null) {
                    resultType = SQLQueryExprType.forTypedObject(
                        statistics.getMonitor(),
                        proc.returnType,
                        SQLQuerySymbolClass.FUNCTION
                    );
                }
            } catch (DBException e) {
                statistics.appendError(this.procName.syntaxNode, this.procName.getNameString() + " failed to obtain function info", e);
            } finally {
                if (resultType == null) {
                    resultType = SQLQueryExprType.UNKNOWN;
                }
            }
            return resultType;
        } else {
            SQLQuerySymbolClass tableSymbolClass = statistics.isTreatErrorsAsWarnings()
                ? SQLQuerySymbolClass.FUNCTION
                : SQLQuerySymbolClass.ERROR;
            SQLQuerySemanticUtils.performPartialResolution(
                context.getRowsSources(),
                statistics,
                this.procName,
                origin,
                Set.of(RelationalObjectType.TYPE_UNKNOWN),
                tableSymbolClass
            );
            if (candidates.isEmpty()) {
                statistics.appendError(this.procName.syntaxNode, "Function " + this.procName.getNameString() + " not found");
            }
            return SQLQueryExprType.UNKNOWN;
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueFunctionExpr(this, arg);
    }

    private record CandidateProcedure(
        @NotNull
        DBSObject referenceTarget,
        @NotNull
        DBSProcedure procedure,

        @Nullable
        List<DBSProcedureParameter> arguments,
        @Nullable
        List<DBSProcedureParameter> results,
        @Nullable
        DBSTypedObject returnType,

        boolean checked,
        @Nullable
        List<Runnable> validationErrors
    ) {
    }

    @NotNull
    private static CandidateProcedure prepareFunctionApplication(
        @NotNull SQLQueryValueFunctionExpression callExpr,
        @NotNull DBSObject referenceTarget,
        @NotNull DBSProcedure procedure,
        @NotNull SQLQueryRecognitionContext statistics
    ) throws DBException {

        DBSTypedObject returnType = procedure.getReturnType(statistics.getMonitor());
        List<DBSProcedureParameter> arguments;
        List<DBSProcedureParameter> results;
        List<Runnable> errors = null;
        boolean checked;

        Collection<? extends DBSProcedureParameter> params = procedure.getParameters(statistics.getMonitor());
        if (params != null) {
            arguments = new ArrayList<>(params.size());
            results = new ArrayList<>(params.size());

            for (DBSProcedureParameter param : params) {
                switch (param.getParameterKind()) {
                    case IN:
                    case OUT:
                    case INOUT:
                        arguments.add(param);
                        break;
                    case RETURN:
                    case RESULTSET:
                    case TABLE:
                        results.add(param);
                        break;
                    default:
                }
            }
        } else {
            arguments = null;
            results = null;
        }

        if (arguments != null) {
            if (arguments.size() != callExpr.operands.size()) {
                errors = noteError(
                    errors, callExpr.getSyntaxNode(),
                    "Illegal amount of arguments: given " + callExpr.operands.size() + " while expected " + arguments.size(),
                    statistics
                );
            }
            for (int i = 0; i < arguments.size() && i < callExpr.operands.size(); i++) {
                SQLQueryExprType srcType = callExpr.operands.get(i).type;
                DBSTypedObject tgtType = arguments.get(i).getParameterType();
                DBPDataKind src = srcType.getDataKind();
                DBPDataKind tgt = tgtType.getDataKind();
                boolean comparable = !(
                    src == DBPDataKind.UNKNOWN || src == DBPDataKind.ANY ||
                    tgt == DBPDataKind.UNKNOWN || tgt == DBPDataKind.ANY
                );
                if (comparable && !DBPDataKind.canConsume(src, tgt)) {
                    errors = noteError(
                        errors, callExpr.operands.get(i).getSyntaxNode(),
                        "Inconsistent parameter type: expected " + tgtType.getFullTypeName() + " while given "
                            + srcType.getDisplayName(),
                        statistics
                    );
                }
            }
            checked = true;
        } else {
            checked = false;
        }

        return new CandidateProcedure(
            referenceTarget,
            procedure,
            arguments,
            results,
            returnType,
            checked,
            errors
        );
    }

    @NotNull
    private static List<Runnable> noteError(
        @Nullable List<Runnable> validationErrors,
        @NotNull STMTreeNode syntaxNode,
        @NotNull String message,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (validationErrors == null) {
            validationErrors = new ArrayList<>();
        }
        validationErrors.add(() -> statistics.appendError(syntaxNode, message));
        return validationErrors;
    }
}
