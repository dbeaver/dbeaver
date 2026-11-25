/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.List;

/**
 * Firebird index, read from RDB$INDICES.
 * In Firebird the sort order applies to the whole index, not to single columns, and an index is either
 * built on columns or on an expression (COMPUTED BY). Since Firebird 5 an index can be partial (WHERE).
 */
public class FireBirdTableIndex extends GenericTableIndex {

    private static final String WHERE_KEYWORD = "WHERE";

    private boolean descending;
    private boolean inactive;
    private final double selectivity;
    @Nullable
    private String expression;
    @Nullable
    private String condition;
    @Nullable
    private String description;

    public FireBirdTableIndex(
        @NotNull GenericTableBase table,
        boolean nonUnique,
        @Nullable String indexName,
        boolean descending,
        boolean inactive,
        double selectivity,
        @Nullable String expression,
        @Nullable String condition,
        @Nullable String description,
        boolean persisted
    ) {
        super(table, nonUnique, null, 0, indexName, DBSIndexType.OTHER, persisted);
        this.descending = descending;
        this.inactive = inactive;
        this.selectivity = selectivity;
        this.expression = expression;
        this.condition = condition;
        this.description = description;
    }

    /**
     * Firebird has no index qualifier.
     */
    @Nullable
    @Override
    @Property(hidden = true)
    public String getQualifier() {
        return super.getQualifier();
    }

    /**
     * Not provided by Firebird; the selectivity is shown instead.
     */
    @Override
    @Property(hidden = true)
    public long getCardinality() {
        return super.getCardinality();
    }

    /**
     * True if the index is sorted in descending order. In the UI the order can only be chosen per column,
     * so a new index is also descending if one of its columns is.
     */
    @Property(viewable = true, order = 6)
    public boolean isDescending() {
        if (descending) {
            return true;
        }
        List<GenericTableIndexColumn> columns = getAttributeReferences(null);
        return !isPersisted() && columns != null && columns.stream().anyMatch(column -> !column.isAscending());
    }

    public void setDescending(boolean descending) {
        this.descending = descending;
    }

    /**
     * Inactive indexes are not maintained and not used by the optimizer.
     */
    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    /**
     * Selectivity from the last statistics update: 1 / number of distinct keys, 0 if never computed.
     */
    @Property(viewable = true, order = 8)
    public double getSelectivity() {
        return selectivity;
    }

    /**
     * Expression of an expression index (COMPUTED BY), without the enclosing parentheses.
     */
    @Nullable
    @Property(viewable = true, editable = true, length = PropertyLength.MULTILINE, order = 9)
    public String getExpression() {
        return expression;
    }

    public void setExpression(@Nullable String expression) {
        this.expression = expression;
    }

    /**
     * Condition of a partial index (Firebird 5+), without the WHERE keyword.
     */
    @Nullable
    @Property(viewable = true, editable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getCondition() {
        return condition;
    }

    public void setCondition(@Nullable String condition) {
        this.condition = condition;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Removes the parentheses Firebird stores around the expression of an expression index.
     * COMPUTED BY requires them and the source is stored as written, so they always enclose the whole expression.
     */
    @Nullable
    public static String extractExpression(@Nullable String source) {
        if (source == null) {
            return null;
        }
        String expression = source.trim();
        if (expression.startsWith("(") && expression.endsWith(")")) {
            expression = expression.substring(1, expression.length() - 1).trim();
        }
        return expression;
    }

    /**
     * Removes the WHERE keyword Firebird stores with the condition of a partial index.
     */
    @Nullable
    public static String extractCondition(@Nullable String source) {
        if (source == null) {
            return null;
        }
        String result = source.trim();
        if (result.regionMatches(true, 0, WHERE_KEYWORD, 0, WHERE_KEYWORD.length())) {
            result = result.substring(WHERE_KEYWORD.length()).trim();
        }
        return result;
    }
}
