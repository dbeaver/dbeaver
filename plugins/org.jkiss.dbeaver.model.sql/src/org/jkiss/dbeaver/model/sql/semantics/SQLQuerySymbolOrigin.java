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

import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * The origin providing symbols occurring as identifiers in a query text (actually faced with or potential)
 */
public interface SQLQuerySymbolOrigin {

    interface Visitor<T> {
        T visitDbObjectFromDbObject(DbObjectFromDbObject origin);

        T visitDbObjectFromContext(DbObjectFromContext origin);

        T visitRowsetRefFromContext(RowsetRefFromContext origin);

        T visitValueRefFromContext(ValueRefFromContext origin);

        T visitColumnRefFromReferencedContext(ColumnRefFromReferencedContext origin);

        T visitColumnNameFromContext(ColumnNameFromContext origin);

        T visitMemberOfType(MemberOfType origin);
    }

    boolean isChained();

    <T> T apply(Visitor<T> visitor);

    /**
     * DB object is a scope for its child name
     */
    record DbObjectFromDbObject(DBSObject object) implements SQLQuerySymbolOrigin {
        @Override
        public boolean isChained() {
            return true;
        }

        @Override
        public <T> T apply(Visitor<T> visitor) {
            return visitor.visitDbObjectFromDbObject(this);
        }
    }

    /**
     * Context is a scope for DB object name
     */
    record DbObjectFromContext(SQLQueryDataContext context) implements SQLQuerySymbolOrigin {
        @Override
        public boolean isChained() {
            return false;
        }

        @Override
        public <T> T apply(Visitor<T> visitor) {
            return visitor.visitDbObjectFromContext(this);
        }
    }

    /**
     * Context is a scope for rowset reference (rowset alias or table name)
     */
    record RowsetRefFromContext(SQLQueryDataContext context) implements SQLQuerySymbolOrigin {
        @Override
        public boolean isChained() {
            return false;
        }

        @Override
        public <T> T apply(Visitor<T> visitor) {
            return visitor.visitRowsetRefFromContext(this);
        }
    }

    /**
     * Context is a scope for value reference (column name of any kind: simple or fully-qualified, single or tuple)
     */
    record ValueRefFromContext(SQLQueryDataContext context) implements SQLQuerySymbolOrigin {
        @Override
        public boolean isChained() {
            return false;
        }

        @Override
        public <T> T apply(Visitor<T> visitor) {
            return visitor.visitValueRefFromContext(this);
        }
    }

    /**
     * Explicitly referenced source is a scope for column name provided by the underlying rowset query node
     * (like a column provided by the table or subquery through the corresponding alias, or through the fully-qualified table name)
     */
    record ColumnRefFromReferencedContext(SourceResolutionResult rowsSource) implements SQLQuerySymbolOrigin {
        @Override
        public boolean isChained() {
            return true;
        }

        @Override
        public <T> T apply(Visitor<T> visitor) {
            return visitor.visitColumnRefFromReferencedContext(this);
        }
    }

    /**
     * Context is a scope for strictly simple separate column name
     */
    record ColumnNameFromContext(SQLQueryDataContext context) implements SQLQuerySymbolOrigin {
        @Override
        public boolean isChained() {
            return false;
        }

        @Override
        public <T> T apply(Visitor<T> visitor) {
            return visitor.visitColumnNameFromContext(this);
        }
    }

    /**
     * Type is a scope for its member name
     */
    record MemberOfType(SQLQueryExprType type) implements SQLQuerySymbolOrigin {
        @Override
        public boolean isChained() {
            return true;
        }

        @Override
        public <T> T apply(Visitor<T> visitor) {
            return visitor.visitMemberOfType(this);
        }
    }
}
