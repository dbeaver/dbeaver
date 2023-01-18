/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Contains methods used for query generation
 */
public interface SQLDialectQueryGenerator {

    /**
     * Appends filter statement to query
     *
     * @param dataSource database
     * @param query query to append conditions to
     * @param tableAlias alias of the table
     * @param dataFilter filter containing conditions
     */
    void appendQueryConditions(DBPDataSource dataSource,
        @NotNull StringBuilder query,
        @Nullable String tableAlias,
        @Nullable DBDDataFilter dataFilter);

    /**
     * Appends order statement to query
     *
     * @param dataSource database
     * @param query query to append conditions to
     * @param tableAlias alias of the table
     * @param dataFilter filter containing conditions
     */
    void appendQueryOrder(DBPDataSource dataSource,
        @NotNull StringBuilder query,
        @Nullable String tableAlias,
        @Nullable DBDDataFilter dataFilter);

    /**
     * Appends filter conditions to query
     *
     * @param filter filter containing conditions
     * @param dataSource database
     * @param conditionTable alias of the table
     * @param query query to append conditions to
     * @param inlineCriteria does query has inlineCriteria
     */
    void appendConditionString(@NotNull DBDDataFilter filter,
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        @NotNull StringBuilder query,
        boolean inlineCriteria);


    /**
     * Appends filter conditions to query
     *
     * @param filter filter containing conditions
     * @param dataSource database
     * @param conditionTable alias of the table
     * @param query query to append conditions to
     * @param inlineCriteria does query has inlineCriteria
     * @param subQuery is query part of another query
     */
    void appendConditionString(@NotNull DBDDataFilter filter,
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        @NotNull StringBuilder query,
        boolean inlineCriteria,
        boolean subQuery);

    /**
     * Appends filter conditions to query
     *
     * @param filter filter containing conditions
     * @param constraints list of attribute constraints
     * @param dataSource database
     * @param conditionTable alias of the table
     * @param query query to append conditions to
     * @param inlineCriteria does query has inlineCriteria
     * @param subQuery is query part of another query
     */
    void appendConditionString(@NotNull DBDDataFilter filter,
        @NotNull List<DBDAttributeConstraint> constraints,
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        @NotNull StringBuilder query,
        boolean inlineCriteria,
        boolean subQuery);

    /**
     * Applies filters to the existing user queries
     * @param monitor database progress monitor
     * @param dataSource datasource
     * @param sqlQuery user query
     * @param dataFilter filter conditions
     * @return modified query
     */
    @NotNull
    String getQueryWithAppliedFilters(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull String sqlQuery,
        @NotNull DBDDataFilter dataFilter
    );

    /**
     * returns user query with filter and order
     *
     * @param dataSource datasource
     * @param sqlQuery user query
     * @param dataFilter filter conditions
     * @return modified query
     */
    @NotNull
    String getWrappedFilterQuery(@NotNull DBPDataSource dataSource, @NotNull String sqlQuery, @NotNull DBDDataFilter dataFilter);

    /**
     * Appends order conditions to query
     *
     * @param filter list of query column constraints
     * @param dataSource database
     * @param conditionTable alias of the table
     * @param subQuery is query part of another query
     * @param query query to append conditions to
     */
    void appendOrderString(@NotNull DBDDataFilter filter,
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        boolean subQuery,
        @NotNull StringBuilder query);

    /**
     * Returns constraint condition for constraint
     *
     * @param dataSource database
     * @param constraint attribute constraint
     * @param conditionTable name of table
     * @param inlineCriteria does query has inlineCriteria
     * @return string with the constraint condition
     */
    @Nullable
    String getConstraintCondition(@NotNull DBPDataSource dataSource,
        @NotNull DBDAttributeConstraint constraint,
        @Nullable String conditionTable,
        boolean inlineCriteria);
}
