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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.util.Collection;
import java.util.Collections;

public class PostgreQueryBuilder {
    @NotNull
    private final String columnsToSelect;

    @NotNull
    private final String fromClause;

    @Nullable
    private String whereClause;

    @NotNull
    private final String name;

    private boolean caseSensitive;

    @Nullable
    private String descriptionClause;

    @NotNull
    private final Collection<? extends DBSSchema> schemas;

    @NotNull
    private final String namespace;

    @NotNull
    private final String orderBy;

    private int maxResults;

    @Nullable
    private String definitionClause;

    public PostgreQueryBuilder(
        @NotNull String columnsToSelect,
        @NotNull String fromClause,
        @NotNull String name,
        @NotNull Collection<? extends DBSSchema> schemas,
        @NotNull String namespace,
        @NotNull String orderBy
    ) {
        this.columnsToSelect = columnsToSelect;
        this.fromClause = fromClause;
        this.name = name;
        this.schemas = schemas;
        this.namespace = namespace;
        this.orderBy = orderBy;
    }

    @NotNull
    public String getColumnsToSelect() {
        return columnsToSelect;
    }

    @NotNull
    public String getFromClause() {
        return fromClause;
    }

    @Nullable
    public String getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(@Nullable String whereClause) {
        this.whereClause = whereClause;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Nullable
    public String getDescriptionClause() {
        return descriptionClause;
    }

    public void setDescriptionClause(@Nullable String descriptionClause) {
        this.descriptionClause = descriptionClause;
    }

    @NotNull
    public Collection<DBSSchema> getSchemas() {
        return Collections.unmodifiableCollection(schemas);
    }

    @NotNull
    public String getNamespace() {
        return namespace;
    }

    @NotNull
    public String getOrderBy() {
        return orderBy;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    @Nullable
    public String getDefinitionClause() {
        return definitionClause;
    }

    public void setDefinitionClause(@Nullable String definitionClause) {
        this.definitionClause = definitionClause;
    }

    public String build() {
        StringBuilder sql = new StringBuilder("SELECT ").append(columnsToSelect);
        sql.append(" FROM ").append(fromClause).append(" WHERE ");
        if (whereClause != null) {
            sql.append(whereClause).append(" AND ");
        }
        boolean addParentheses = definitionClause != null || descriptionClause != null;
        if (addParentheses) {
            sql.append("(");
        }
        String likeClause = caseSensitive ? " LIKE ?" : " ILIKE ?";
        sql.append(name).append(likeClause).append(" ");
        if (descriptionClause != null) {
            sql.append("OR ").append(descriptionClause).append(likeClause);
        }
        if (definitionClause != null) {
            sql.append(" OR (").append(definitionClause).append(likeClause).append(")");
        }
        if (addParentheses) {
            sql.append(")");
        }
        if (!schemas.isEmpty()) {
            sql.append("AND ").append(namespace).append(" IN (");
            sql.append(SQLUtils.generateParamList(schemas.size())).append(") ");
        }
        sql.append("ORDER BY ").append(orderBy);
        if (maxResults > 0) {
            sql.append(" LIMIT ").append(maxResults);
        }
        return sql.toString();
    }


}
