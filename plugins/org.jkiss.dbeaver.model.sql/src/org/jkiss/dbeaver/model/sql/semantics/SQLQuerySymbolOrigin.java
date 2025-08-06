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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.Set;

/**
 * The origin providing symbols occurring as identifiers in a query text (actually faced with or potential)
 */
public abstract class SQLQuerySymbolOrigin {

    public interface Visitor {
        void visitDbObjectFromDbObject(DbObjectFromDbObject origin);

        void visitDbObjectRef(DbObjectRef origin);

        void visitColumnRefFromReferencedContext(ColumnRefFromReferencedContext origin);

        void visitMemberOfType(MemberOfType origin);

        void visitRowsSourceRef(RowsSourceRef rowsSourceRef);

        void visitRowsDataRef(RowsDataRef rowsDataRef);

        void visitExpandableRowsTupleRef(ExpandableRowsTupleRef tupleRef);

        void visitColumnNameFromRowsData(ColumnNameFromRowsData origin);

        void visitSyntaxBasedFromRowsData(SyntaxBasedFromRowsData origin);
    }

    public abstract boolean isChained();

    public abstract void apply(Visitor visitor);

    /**
     * DB object is a scope for its child name
     */
    public static class DbObjectFromDbObject extends SQLQuerySymbolOrigin {

        @NotNull
        private final DBSObject object;

        @NotNull
        private final Set<DBSObjectType> objectTypes;

        public DbObjectFromDbObject(@NotNull DBSObject object, @NotNull DBSObjectType memberType) {
            this(object, Set.of(memberType));
        }

        public DbObjectFromDbObject(@NotNull DBSObject object, @NotNull Set<DBSObjectType> objectTypes) {
            this.object = object;
            this.objectTypes = objectTypes;
        }

        @NotNull
        public DBSObject getObject() {
            return this.object;
        }

        @NotNull
        public Set<DBSObjectType> getMemberTypes() {
            return this.objectTypes;
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
    public static class DbObjectRef extends RowsSourceRef {

        @NotNull
        private final Set<DBSObjectType> objectTypes;

        private final boolean includingRowsets;

        public DbObjectRef(
            @NotNull SQLQueryRowsSourceContext rowsSourceContext,
            @NotNull DBSObjectType objectType
        ) {
            this(rowsSourceContext, Set.of(objectType), false);
        }

        public DbObjectRef(
            @NotNull SQLQueryRowsSourceContext rowsSourceContext,
            @NotNull Set<DBSObjectType> objectTypes,
            boolean includingRowsets
        ) {
            super(rowsSourceContext);
            this.objectTypes = objectTypes;
            this.includingRowsets = includingRowsets;
        }

        @NotNull
        public Set<DBSObjectType> getObjectTypes() {
            return this.objectTypes;
        }

        public boolean isIncludingRowsets() {
            return this.includingRowsets;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitDbObjectRef(this);
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
     * Type is a scope for its member name
     */
    public static class MemberOfType extends SQLQuerySymbolOrigin {
        @NotNull
        private final SQLQueryExprType type;

        public MemberOfType(@NotNull SQLQueryExprType type) {
            this.type = type;
        }

        @NotNull
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

    public static class RowsSourceRef extends SQLQuerySymbolOrigin {

        @NotNull
        private final SQLQueryRowsSourceContext rowsSourceContext;

        public RowsSourceRef(@NotNull SQLQueryRowsSourceContext rowsSourceContext) {
            this.rowsSourceContext = rowsSourceContext;
        }

        public boolean isChained() {
            return false;
        }

        public @NotNull SQLQueryRowsSourceContext getRowsSourceContext() {
            return this.rowsSourceContext;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitRowsSourceRef(this);
        }
    }

    public static class RowsDataRef extends SQLQuerySymbolOrigin {
        
        @NotNull
        private final SQLQueryRowsDataContext rowsDataContext;

        public RowsDataRef(@NotNull SQLQueryRowsDataContext rowsDataContext) {
            this.rowsDataContext = rowsDataContext;
        }

        public boolean isChained() {
            return false;
        }

        @NotNull
        public SQLQueryRowsDataContext getRowsDataContext() {
            return this.rowsDataContext;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitRowsDataRef(this);
        }
    }

    /**
     * Context is a scope for strictly simple separate column name
     */
    public static class ColumnNameFromRowsData extends RowsDataRef {

        public ColumnNameFromRowsData(@NotNull SQLQueryRowsDataContext dataContext) {
            super(dataContext);
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitColumnNameFromRowsData(this);
        }
    }

    /**
     * Placeholder is a reference to a columns subset provided by the referencedSource or to all columns of the rows data
     */
    public static class ExpandableRowsTupleRef extends RowsDataRef {

        @NotNull
        private final STMTreeNode placeholder;

        @Nullable
        private final SourceResolutionResult referencedSource;

        public ExpandableRowsTupleRef(
            @NotNull STMTreeNode placeholder,
            @NotNull SQLQueryRowsDataContext dataContext,
            @Nullable SourceResolutionResult referencedSource
        ) {
            super(dataContext);
            this.placeholder = placeholder;
            this.referencedSource = referencedSource;
        }

        @Override
        public boolean isChained() {
            return true;
        }

        @NotNull
        public STMTreeNode getPlaceholder() {
            return this.placeholder;
        }

        @Nullable
        public SourceResolutionResult getReferencedSource() {
            return this.referencedSource;
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitExpandableRowsTupleRef(this);
        }
    }

    /**
     * Context is a scope for strictly simple separate column name
     */
    public static class SyntaxBasedFromRowsData extends RowsDataRef {

        public SyntaxBasedFromRowsData(@NotNull SQLQueryRowsDataContext dataContext) {
            super(dataContext);
        }

        @Override
        public void apply(Visitor visitor) {
            visitor.visitSyntaxBasedFromRowsData(this);
        }
    }
}
