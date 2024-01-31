package org.jkiss.dbeaver.ui.editors.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryExprType;

public class SQLQueryValueMemberExpression extends SQLQueryValueExpression {

    private static final Log log = Log.getLog(SQLQueryValueMemberExpression.class);

    @NotNull
    private final String content;

    @NotNull
    private final SQLQueryValueExpression owner;
    @NotNull
    private final SQLQuerySymbolEntry identifier;

    public SQLQueryValueMemberExpression(
        @NotNull Interval range,
        @NotNull String content,
        @NotNull SQLQueryValueExpression owner,
        @NotNull SQLQuerySymbolEntry identifier
    ) {
        super(range);
        this.content = content;
        this.owner = owner;
        this.identifier = identifier;
    }

    @NotNull
    public String getExprContent() {
        return this.content;
    }

    @NotNull
    public SQLQueryValueExpression getMemberOwner() {
        return this.owner;
    }

    @NotNull
    public SQLQuerySymbolEntry getMemberIdentifier() {
        return this.identifier;
    }

    @NotNull
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.identifier.getSymbol();
    }
    
    @Override
    void propagateContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.owner.propagateContext(context, statistics);

        if (this.identifier.isNotClassified()) {
            SQLQueryExprType type;
            try {
                type = this.owner.getValueType().findNamedMemberType(statistics.getMonitor(), this.identifier.getName());

                if (type != null) {
                    this.identifier.setDefinition(type.getDeclaratorDefinition());
                } else {
                    this.identifier.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                }
            } catch (DBException e) {
                log.debug(e);
                statistics.appendError(this.identifier, "Failed to resolve member reference", e);
                type = null;
            }

            this.type = type != null ? type : SQLQueryExprType.UNKNOWN;
        }
    }
    
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueMemberReferenceExpr(this, arg);
    }

    @Override
    public String toString() {
        return "ValueMember[(" + this.owner.toString() + ")." + this.identifier.getName() + ":" + this.type.toString() + "]";
    }
}
