/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.ai.functions;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

class GetDatasourceInfoFunctionTest {
    @Test
    void reportsStructureRatherThanGenericCatalogAndSchemaTerms() throws Exception {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class,
            Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        DBPDataSourceInfo info = Mockito.mock(DBPDataSourceInfo.class);
        Mockito.when(dataSource.getInfo()).thenReturn(info);
        Mockito.when(info.getCatalogTerm()).thenReturn("Catalog");
        Mockito.when(info.getSchemaTerm()).thenReturn("Schema");
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        AIDatabaseContext databaseContext = Mockito.mock(AIDatabaseContext.class);
        Mockito.when(databaseContext.getExecutionContext()).thenReturn(executionContext);
        AIFunctionContext context = Mockito.mock(AIFunctionContext.class);
        Mockito.when(context.getContext()).thenReturn(databaseContext);
        DBSObjectContainer container = (DBSObjectContainer) dataSource;
        GetDatasourceInfoFunction function = new GetDatasourceInfoFunction();

        Mockito.doReturn(DBSEntity.class).when(container).getPrimaryChildType(Mockito.any());
        String tableOnly = function.callFunction(context, Map.of()).getValue().toString();
        Assertions.assertTrue(tableOnly.contains("Supports Catalogs: false"));
        Assertions.assertTrue(tableOnly.contains("Supports Schemas: false"));

        Mockito.doReturn(DBSSchema.class).when(container).getPrimaryChildType(Mockito.any());
        Assertions.assertTrue(function.callFunction(context, Map.of()).getValue().toString().contains("Supports Schemas: true"));

        Mockito.doReturn(DBSCatalog.class).when(container).getPrimaryChildType(Mockito.any());
        DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(executionContext.getContextDefaults()).thenReturn(defaults);
        Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);
        String catalogsAndSchemas = function.callFunction(context, Map.of()).getValue().toString();
        Assertions.assertTrue(catalogsAndSchemas.contains("Supports Catalogs: true"));
        Assertions.assertTrue(catalogsAndSchemas.contains("Supports Schemas: true"));
    }
}
