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
package org.jkiss.dbeaver.model.lsm.sql.impl;


import java.util.List;


public abstract class SelectionSource {
    public static class Table extends SelectionSource {
        public String catalogName;
        public String schemaName;
        public String tableName;
        public String alias;
        public List<String> columnNames;
    }

    public static class CrossJoin extends SelectionSource {
        public Table table;
    }

    public enum JoinKind {
        INNER,
        LEFT,
        RIGHT,
        FULL,
        LEFT_OUTER,
        RIGHT_OUTER,
        FULL_OUTER,
        UNION,

        NATURAL_INNER,
        NATURAL_LEFT,
        NATURAL_RIGHT,
        NATURAL_FULL,
        NATURAL_LEFT_OUTER,
        NATURAL_RIGHT_OUTER,
        NATURAL_FULL_OUTER,
        NATURAL_UNION
    }

    public static class NaturalJoin extends SelectionSource {
        public JoinKind kind;
        public Table table;
        public ValueExpression condition;
        public List<String> columnNames;
    }

    public static class Subquery extends SelectionSource {
        public SelectionQuery selectionQuery;
        public String alias;
    }
}
