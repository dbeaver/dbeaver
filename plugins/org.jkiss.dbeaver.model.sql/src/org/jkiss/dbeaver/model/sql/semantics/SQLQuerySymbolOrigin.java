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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * The origin providing symbols occurring as identifiers in a query text (actually faced with or potential)
 */
public abstract class SQLQuerySymbolOrigin {

    public interface Visitor {
        void visitDbObjectFromDbObject(DbObjectFromDbObject origin);

        void visitDbObjectFromContext(DbObjectFromContext origin);

        void visitRowsetRefFromContext(RowsetRefFromContext origin);

        void visitValueRefFromContext(ValueRefFromContext origin);

        void visitColumnRefFromReferencedContext(ColumnRefFromReferencedContext origin);

        void visitColumnNameFromContext(ColumnNameFromContext origin);

        void visitMemberOfType(MemberOfType origin);

        void visitDataContextSymbol(DataContextSymbolOrigin origin);
    }

    public abstract boolean isChained();

    public abstract void apply(Visitor visitor);

    public static class DataContextSymbolOrigin extends SQLQuerySymbolOrigin {

        private final SQLQueryDataContext dataContext;

        public DataContextSymbolOrigin(SQLQueryDataContext dataContext) {
            this.dataContext = dataContext;
        }

        @Override
        public boolean isChained() {
            return false;
        }

        public SQLQueryDataContext getDataContext() {
            return this.dataContext;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitDataContextSymbol(this);
        }
    }

    /**
     * DB object is a scope for its child name
     */
    public static class DbObjectFromDbObject extends SQLQuerySymbolOrigin {

        private final DBSObject object;

        public DbObjectFromDbObject(DBSObject object) {
            this.object = object;
        }

        public DBSObject getObject() {
            return this.object;
        }

        @Override
        public boolean isChained() {
            return true;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitDbObjectFromDbObject(this);
        }
    }

    /**
     * Context is a scope for DB object name
     */
    public static class DbObjectFromContext extends DataContextSymbolOrigin {

        public DbObjectFromContext(SQLQueryDataContext dataContext) {
            super(dataContext);
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitDbObjectFromContext(this);
        }
    }

    /**
     * Context is a scope for rowset reference (rowset alias or table name)
     */
    public static class RowsetRefFromContext extends DataContextSymbolOrigin {

        public RowsetRefFromContext(SQLQueryDataContext dataContext) {
            super(dataContext);
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitRowsetRefFromContext(this);
        }
    }

    /**
     * Context is a scope for value reference (column name of any kind: simple or fully-qualified, single or tuple)
     */
    public static class ValueRefFromContext extends DataContextSymbolOrigin {

        public ValueRefFromContext(SQLQueryDataContext dataContext) {
            super(dataContext);
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitValueRefFromContext(this);
        }
    }

    /**
     * Explicitly referenced source is a scope for column name provided by the underlying rowset query node
     * (like a column provided by the table or subquery through the corresponding alias, or through the fully-qualified table name)
     */
    public static class ColumnRefFromReferencedContext extends SQLQuerySymbolOrigin {

        private final SourceResolutionResult referencedSource;

        public ColumnRefFromReferencedContext(SourceResolutionResult referencedSource) {
            this.referencedSource = referencedSource;
        }

        @Override
        public boolean isChained() {
            return true;
        }

        public SourceResolutionResult getRowsSource() {
            return this.referencedSource;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitColumnRefFromReferencedContext(this);
        }
    }

    /**
     * Context is a scope for strictly simple separate column name
     */
    public static class ColumnNameFromContext extends DataContextSymbolOrigin {
        public ColumnNameFromContext(SQLQueryDataContext dataContext) {
            super(dataContext);
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitColumnNameFromContext(this);
        }
    }

    /**
     * Type is a scope for its member name
     */
    public static class MemberOfType extends SQLQuerySymbolOrigin {
        @NotNull
        private final SQLQueryExprType type;

        public MemberOfType(@NotNull SQLQueryExprType type) {
            this.type = type;
        }

        public final SQLQueryExprType getType() {
            return this.type;
        }

        @Override
        public boolean isChained() {
            return true;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitMemberOfType(this);
        }
    }
}
