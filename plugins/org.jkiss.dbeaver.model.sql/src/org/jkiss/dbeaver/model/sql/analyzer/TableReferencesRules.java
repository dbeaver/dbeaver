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
package org.jkiss.dbeaver.model.sql.analyzer;

import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;

import java.util.Set;

/**
 * Represents internal rules
 */
public class TableReferencesRules {

    public static final Set<String> expandRulesToTableRef = Set.of(
        STMKnownRuleNames.sqlQuery,
        STMKnownRuleNames.directSqlDataStatement,
        STMKnownRuleNames.insertStatement,
        STMKnownRuleNames.selectStatement,
        STMKnownRuleNames.updateStatement,
        STMKnownRuleNames.insertColumnsAndSource,
        STMKnownRuleNames.queryExpression,
        STMKnownRuleNames.nonJoinQueryTerm,
        STMKnownRuleNames.queryPrimary,
        STMKnownRuleNames.nonJoinQueryPrimary,
        STMKnownRuleNames.simpleTable,
        STMKnownRuleNames.joinedTable,
        STMKnownRuleNames.querySpecification,
        STMKnownRuleNames.naturalJoinTerm,
        STMKnownRuleNames.crossJoinTerm,
        STMKnownRuleNames.tableExpression,
        STMKnownRuleNames.fromClause,
        STMKnownRuleNames.tableReference);

    public static final Set<String> extractRulesToTableRef = Set.of(
        STMKnownRuleNames.nonjoinedTableReference,
        STMKnownRuleNames.tableName
    );

    public static final Set<String> expandRulesToTableName = Set.of(
        STMKnownRuleNames.nonjoinedTableReference,
        STMKnownRuleNames.correlationSpecification);

    public static final Set<String> extractRulesToTableName = Set.of(
        STMKnownRuleNames.tableName,
        STMKnownRuleNames.correlationName);

    private TableReferencesRules() {
        // private
    }
}
