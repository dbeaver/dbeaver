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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionTextProvider;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLQueryCompletionColumnQualifierTest extends DBeaverUnitTest {

    private static final SQLDialect TABLE_ONLY_DIALECT = new BasicSQLDialect() {
        @Override
        public boolean supportsSchemaQualifiedColumnNames() {
            return false;
        }
    };

    @Test
    void schemaQualifierIsRemovedForTableOnlyColumnReferences() {
        assertEquals(List.of("orders"), SQLQueryCompletionTextProvider.columnReferenceQualifierParts(
            TABLE_ONLY_DIALECT, List.of("owner", "orders")));
        assertEquals(List.of("orders"), SQLQueryCompletionTextProvider.columnReferenceQualifierParts(
            TABLE_ONLY_DIALECT, List.of("database", "owner", "orders")));
    }

    @Test
    void existingFullyQualifiedColumnReferencesArePreservedByDefault() {
        assertEquals(List.of("owner", "orders"), SQLQueryCompletionTextProvider.columnReferenceQualifierParts(
            BasicSQLDialect.INSTANCE, List.of("owner", "orders")));
    }

    @Test
    void bareTableQualifierAndEmptyQualifierAreNotChanged() {
        assertEquals(List.of("orders"), SQLQueryCompletionTextProvider.columnReferenceQualifierParts(
            TABLE_ONLY_DIALECT, List.of("orders")));
        assertEquals(List.of(), SQLQueryCompletionTextProvider.columnReferenceQualifierParts(
            TABLE_ONLY_DIALECT, List.of()));
    }
}
