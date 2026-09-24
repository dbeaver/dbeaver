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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLMetadataRefreshTargetResolver.RefreshLevel;
import org.jkiss.dbeaver.model.sql.SQLMetadataRefreshTargetResolver.RefreshTarget;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.util.List;

public class SQLMetadataRefreshTargetResolverTest extends DBeaverUnitTest {
    private DBRProgressMonitor monitor;
    private DBCExecutionContext executionContext;
    private DBCExecutionContextDefaults<?, ?> defaults;

    @BeforeEach
    void setUp() {
        monitor = Mockito.mock(DBRProgressMonitor.class);
        executionContext = Mockito.mock(DBCExecutionContext.class);
        defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        Mockito.when(executionContext.getContextDefaults()).thenReturn(defaults);
    }

    @ParameterizedTest
    @EnumSource(value = SQLObjectOperation.Operation.class, names = {"CREATE", "DROP", "RENAME"})
    void databaseAndCatalogStructuralChangesRefreshDataSource(SQLObjectOperation.Operation operation) {
        assertTarget(operation, SQLObjectOperation.ObjectKind.DATABASE, List.of("database"),
            target(RefreshLevel.DATA_SOURCE, null, null));
        assertTarget(operation, SQLObjectOperation.ObjectKind.CATALOG, List.of("catalog"),
            target(RefreshLevel.DATA_SOURCE, null, null));
    }

    @Test
    void databaseAndCatalogAlterRefreshesNamedCatalog() {
        assertTarget(SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.DATABASE, List.of("database"),
            target(RefreshLevel.CATALOG, "DATABASE", null));
        assertTarget(SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.CATALOG, List.of("catalog"),
            target(RefreshLevel.CATALOG, "CATALOG", null));
    }

    @ParameterizedTest
    @EnumSource(value = SQLObjectOperation.Operation.class, names = {"CREATE", "DROP", "RENAME"})
    void schemaStructuralChangesRefreshContainingCatalog(SQLObjectOperation.Operation operation) {
        assertTarget(operation, SQLObjectOperation.ObjectKind.SCHEMA, List.of("catalog", "schema"),
            target(RefreshLevel.CATALOG, "CATALOG", null));
    }

    @Test
    void schemaAlterRefreshesNamedSchema() {
        assertTarget(SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.SCHEMA,
            List.of("catalog", "schema"), target(RefreshLevel.SCHEMA, "CATALOG", "SCHEMA"));
    }

    @Test
    void unqualifiedSchemaUsesRefreshedDefaultCatalog() {
        DBSCatalog staleCatalog = Mockito.mock(DBSCatalog.class);
        DBSCatalog currentCatalog = Mockito.mock(DBSCatalog.class);
        Mockito.when(staleCatalog.getName()).thenReturn("stale_catalog");
        Mockito.when(currentCatalog.getName()).thenReturn("current_catalog");
        Mockito.when(defaults.supportsCatalogChange()).thenReturn(true);
        Mockito.doReturn(staleCatalog, currentCatalog).when(defaults).getDefaultCatalog();

        RefreshTarget actual = SQLMetadataRefreshTargetResolver.createTarget(
            monitor,
            executionContext,
            operation(SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SCHEMA, List.of("schema")),
            (progressMonitor, contextDefaults, context) -> {
            }
        );

        Assertions.assertEquals(target(RefreshLevel.CATALOG, "current_catalog", null), actual);
    }

    @Test
    void schemaAuthorizationWithoutNameUsesConservativeTargets() {
        DBSCatalog defaultCatalog = Mockito.mock(DBSCatalog.class);
        Mockito.when(defaultCatalog.getName()).thenReturn("default_catalog");
        Mockito.doReturn(defaultCatalog).when(defaults).getDefaultCatalog();

        assertTarget(SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SCHEMA, List.of(),
            target(RefreshLevel.CATALOG, "default_catalog", null));
        assertTarget(SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.SCHEMA, List.of(),
            target(RefreshLevel.DATA_SOURCE, null, null));
    }

    @ParameterizedTest
    @EnumSource(SQLObjectOperation.Operation.class)
    void qualifiedOrdinaryObjectRefreshesContainingSchema(SQLObjectOperation.Operation operation) {
        assertTarget(operation, SQLObjectOperation.ObjectKind.TABLE, List.of("catalog", "schema", "table"),
            target(RefreshLevel.SCHEMA, "CATALOG", "SCHEMA"));
    }

    @Test
    void unknownObjectKindRefreshesDataSource() {
        DBSSchema defaultSchema = Mockito.mock(DBSSchema.class);
        Mockito.when(defaultSchema.getName()).thenReturn("default_schema");
        Mockito.doReturn(defaultSchema).when(defaults).getDefaultSchema();

        assertTarget(
            SQLObjectOperation.Operation.DROP,
            SQLObjectOperation.ObjectKind.OTHER,
            List.of("role_name"),
            target(RefreshLevel.DATA_SOURCE, null, null)
        );
    }

    @Test
    void escapedQuotesAreUnescapedInRefreshTargetNames() {
        assertTarget(
            SQLObjectOperation.Operation.CREATE,
            SQLObjectOperation.ObjectKind.TABLE,
            List.of("\"cat\"\"alog\"", "\"sche\"\"ma\"", "\"table\""),
            target(RefreshLevel.SCHEMA, "cat\"alog", "sche\"ma")
        );
    }

    @Test
    void quotedIdentifierUsesDialectQuotedStorageCase() {
        DBPDataSource dataSource = executionContext.getDataSource();
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new BasicSQLDialect() {
            @NotNull
            @Override
            public DBPIdentifierCase storesUnquotedCase() {
                return DBPIdentifierCase.LOWER;
            }

            @NotNull
            @Override
            public DBPIdentifierCase storesQuotedCase() {
                return DBPIdentifierCase.LOWER;
            }
        });

        assertTarget(
            SQLObjectOperation.Operation.CREATE,
            SQLObjectOperation.ObjectKind.TABLE,
            List.of("\"MySchema\"", "table"),
            target(RefreshLevel.SCHEMA, null, "myschema")
        );
    }

    @Test
    void unqualifiedOrdinaryObjectUsesRefreshedDefaults() {
        DBSCatalog defaultCatalog = Mockito.mock(DBSCatalog.class);
        DBSSchema defaultSchema = Mockito.mock(DBSSchema.class);
        Mockito.when(defaultCatalog.getName()).thenReturn("default_catalog");
        Mockito.when(defaultSchema.getName()).thenReturn("default_schema");
        Mockito.when(defaults.supportsCatalogChange()).thenReturn(true);
        Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);
        Mockito.doReturn(null, defaultCatalog).when(defaults).getDefaultCatalog();
        Mockito.doReturn(null, defaultSchema).when(defaults).getDefaultSchema();

        assertTarget(SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.TABLE, List.of("table"),
            target(RefreshLevel.SCHEMA, "default_catalog", "default_schema"));
    }

    @Test
    void defaultsResolutionFailureFallsBackToDataSource() {
        Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);

        RefreshTarget actual = SQLMetadataRefreshTargetResolver.createTarget(
            monitor,
            executionContext,
            operation(SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.TABLE, List.of("table")),
            (progressMonitor, contextDefaults, context) -> {
                throw new DBException("Test resolution failure");
            }
        );

        Assertions.assertEquals(target(RefreshLevel.DATA_SOURCE, null, null), actual);
    }

    private void assertTarget(
        SQLObjectOperation.Operation operation,
        SQLObjectOperation.ObjectKind objectKind,
        List<String> nameParts,
        RefreshTarget expected
    ) {
        Assertions.assertEquals(expected, SQLMetadataRefreshTargetResolver.createTarget(
            monitor,
            executionContext,
            operation(operation, objectKind, nameParts)
        ));
    }

    private static SQLObjectOperation operation(
        SQLObjectOperation.Operation operation,
        SQLObjectOperation.ObjectKind objectKind,
        List<String> nameParts
    ) {
        return new SQLObjectOperation(operation, objectKind, nameParts);
    }

    private static RefreshTarget target(RefreshLevel level, String catalogName, String schemaName) {
        return new RefreshTarget(level, catalogName, schemaName);
    }
}
