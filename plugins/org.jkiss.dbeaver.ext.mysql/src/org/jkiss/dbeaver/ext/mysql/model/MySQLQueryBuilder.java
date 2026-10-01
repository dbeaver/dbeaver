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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

final class MySQLQueryBuilder {
    @NotNull
    private final String objectNameColumn;

    @Nullable
    private String commentColumnName;

    @Nullable
    private String schemaColumnName;

    @NotNull
    private final String select;

    @NotNull
    private final String from;

    private int maxResults;

    @Nullable
    private String definitionColumnName;
    private boolean isCaseSensitive;

    MySQLQueryBuilder(@NotNull String objectNameColumn, @NotNull String select, @NotNull String from) {
        this.objectNameColumn = objectNameColumn;
        this.select = select;
        this.from = from;
    }

    @NotNull
    public String getObjectNameColumn() {
        return objectNameColumn;
    }

    @Nullable
    public String getCommentColumnName() {
        return commentColumnName;
    }

    public void setCommentColumnName(@Nullable String commentColumnName) {
        this.commentColumnName = commentColumnName;
    }

    @Nullable
    public String getSchemaColumnName() {
        return schemaColumnName;
    }

    public void setSchemaColumnName(@Nullable String schemaColumnName) {
        this.schemaColumnName = schemaColumnName;
    }

    @NotNull
    public String getSelect() {
        return select;
    }

    @NotNull
    public String getFrom() {
        return from;
    }

    @Nullable
    public String getDefinitionColumnName() {
        return definitionColumnName;
    }

    public void setDefinitionColumnName(@Nullable String definitionColumnName) {
        this.definitionColumnName = definitionColumnName;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        isCaseSensitive = caseSensitive;
    }

    private static void addNameWithLikeCondition(@NotNull StringBuilder sql, @NotNull String name, boolean caseSensitive, boolean addOR) {
        if (addOR) {
            sql.append(" OR ");
        }
        if (!caseSensitive) {
            sql.append("UPPER(");
        }
        sql.append(name);
        if (!caseSensitive) {
            sql.append(")");
        }
        sql.append(" LIKE ?");
    }

    public String build() {
        StringBuilder sql = new StringBuilder("SELECT ").append(select).append(" FROM ").append(from).append(" WHERE ");
        boolean addParentheses = commentColumnName != null || definitionColumnName != null;
        if (addParentheses) {
            sql.append("(");
        }
        addNameWithLikeCondition(sql, objectNameColumn, isCaseSensitive, false);
        if (!CommonUtils.isEmpty(commentColumnName)) {
            addNameWithLikeCondition(sql, commentColumnName, isCaseSensitive, true);
        }
        if (!CommonUtils.isEmpty(definitionColumnName)) {
            addNameWithLikeCondition(sql, definitionColumnName, isCaseSensitive, true);
        }
        if (addParentheses) {
            sql.append(") ");
        }
        if (!CommonUtils.isEmpty(schemaColumnName)) {
            sql.append("AND ").append(schemaColumnName).append(" = ? ");
        }
        sql.append("ORDER BY ").append(objectNameColumn);
        if (maxResults > 0) {
            sql.append(" LIMIT ").append(maxResults);
        }
        return sql.toString();
    }

}
