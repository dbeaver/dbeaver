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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class AIDatabaseScopeTest extends DBeaverUnitTest {
    private final DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);

    @Test
    public void disconnectedContextHasAllAndCustomScopes() {
        Assertions.assertEquals(List.of(AIDatabaseScope.CURRENT_DATASOURCE, AIDatabaseScope.CUSTOM),
            AIDatabaseScope.getAvailableScopes(null));
    }

    @Test
    public void flatDatabaseDoesNotOfferSchemasOrCatalogs() {
        Assertions.assertEquals(List.of(AIDatabaseScope.CURRENT_DATASOURCE, AIDatabaseScope.CUSTOM),
            AIDatabaseScope.getAvailableScopes(defaults));
    }

    @Test
    public void schemaSelectorEnablesCurrentSchemaWithoutASelectedSchema() {
        Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);
        Assertions.assertEquals(List.of(AIDatabaseScope.CURRENT_SCHEMA, AIDatabaseScope.CURRENT_DATASOURCE, AIDatabaseScope.CUSTOM),
            AIDatabaseScope.getAvailableScopes(defaults));
    }

    @Test
    public void selectedSchemaIsAvailableWithoutSchemaSwitching() {
        Mockito.doReturn(Mockito.mock(DBSSchema.class)).when(defaults).getDefaultSchema();
        Assertions.assertEquals(List.of(AIDatabaseScope.CURRENT_SCHEMA, AIDatabaseScope.CURRENT_DATASOURCE, AIDatabaseScope.CUSTOM),
            AIDatabaseScope.getAvailableScopes(defaults));
    }

    @Test
    public void catalogSelectorEnablesCurrentDatabaseWithoutASelectedCatalog() {
        Mockito.when(defaults.supportsCatalogChange()).thenReturn(true);
        Assertions.assertEquals(List.of(AIDatabaseScope.CURRENT_DATABASE, AIDatabaseScope.CURRENT_DATASOURCE, AIDatabaseScope.CUSTOM),
            AIDatabaseScope.getAvailableScopes(defaults));
    }

    @Test
    public void selectedCatalogIsAvailableWithoutCatalogSwitching() {
        Mockito.doReturn(Mockito.mock(DBSCatalog.class)).when(defaults).getDefaultCatalog();
        Assertions.assertEquals(List.of(AIDatabaseScope.CURRENT_DATABASE, AIDatabaseScope.CURRENT_DATASOURCE, AIDatabaseScope.CUSTOM),
            AIDatabaseScope.getAvailableScopes(defaults));
    }

    @Test
    public void databaseWithBothNamespacesOffersAllFourScopes() {
        Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);
        Mockito.when(defaults.supportsCatalogChange()).thenReturn(true);
        Assertions.assertEquals(List.of(AIDatabaseScope.values()), AIDatabaseScope.getAvailableScopes(defaults));
    }

    @Test
    public void scopeAvailabilityRefreshesAfterContextChanges() {
        Assertions.assertFalse(AIDatabaseScope.getAvailableScopes(defaults).contains(AIDatabaseScope.CURRENT_SCHEMA));
        Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);
        Assertions.assertTrue(AIDatabaseScope.getAvailableScopes(defaults).contains(AIDatabaseScope.CURRENT_SCHEMA));
        DBCExecutionContextDefaults<?, ?> otherConnection = Mockito.mock(DBCExecutionContextDefaults.class);
        Assertions.assertFalse(AIDatabaseScope.getAvailableScopes(otherConnection).contains(AIDatabaseScope.CURRENT_SCHEMA));
    }
}
