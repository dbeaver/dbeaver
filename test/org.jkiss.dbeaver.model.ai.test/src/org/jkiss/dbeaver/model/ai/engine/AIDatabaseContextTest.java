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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class AIDatabaseContextTest extends DBeaverUnitTest {
    private static final String EMPTY_SCOPE_MESSAGE =
        "Custom scope is empty. Add database objects or select a non-custom scope in the AI context settings.";

    @Test
    public void rejectsMissingCustomEntities() {
        DBException error = Assertions.assertThrows(DBException.class, () -> createBuilder(AIDatabaseScope.CUSTOM).build());

        Assertions.assertEquals(EMPTY_SCOPE_MESSAGE, error.getMessage());
    }

    @Test
    public void rejectsEmptyCustomEntities() {
        DBException error = Assertions.assertThrows(
            DBException.class,
            () -> createBuilder(AIDatabaseScope.CUSTOM).setCustomEntities(List.of()).build()
        );

        Assertions.assertEquals(EMPTY_SCOPE_MESSAGE, error.getMessage());
    }

    @Test
    public void acceptsNonEmptyCustomEntities() throws DBException {
        List<DBSObject> entities = List.of(Mockito.mock(DBSEntity.class));
        AIDatabaseContext context = createBuilder(AIDatabaseScope.CUSTOM).setCustomEntities(entities).build();

        Assertions.assertEquals(entities, context.getCustomEntities());
        Assertions.assertEquals(AIDatabaseScope.CUSTOM, context.getScope());
    }

    @Test
    public void acceptsAllObjectsWithoutCustomEntities() throws DBException {
        AIDatabaseContext context = createBuilder(AIDatabaseScope.CURRENT_DATASOURCE).build();

        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, context.getScope());
    }

    @Test
    public void excludesConfiguredObjectAndItsDescendants() throws DBException {
        DBPDataSourceContainer dataSource = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(dataSource.getId()).thenReturn("data-source");
        Mockito.when(dataSource.getExtension(AIContextSettingsDataSource.AI_DS_EXTENSION))
            .thenReturn(Map.of("excludedObjects", List.of("data-source/restricted")));

        DBSObjectContainer restrictedSchema = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(restrictedSchema.getName()).thenReturn("restricted");
        Mockito.when(restrictedSchema.getParentObject()).thenReturn(dataSource);

        DBSEntity restrictedTable = Mockito.mock(DBSEntity.class);
        Mockito.when(restrictedTable.getName()).thenReturn("documents");
        Mockito.when(restrictedTable.getParentObject()).thenReturn(restrictedSchema);

        DBSObjectContainer publicSchema = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(publicSchema.getName()).thenReturn("public");
        Mockito.when(publicSchema.getParentObject()).thenReturn(dataSource);

        AIDatabaseContext context = new AIDatabaseContext.Builder(new DBSLogicalDataSource(dataSource))
            .setScope(AIDatabaseScope.CURRENT_DATASOURCE)
            .setExecutionContext(Mockito.mock(DBCExecutionContext.class))
            .build();

        Assertions.assertTrue(context.isObjectExcluded(restrictedSchema));
        Assertions.assertTrue(context.isObjectExcluded(restrictedTable));
        Assertions.assertFalse(context.isObjectExcluded(publicSchema));
    }

    @NotNull
    private AIDatabaseContext.Builder createBuilder(@NotNull AIDatabaseScope scope) {
        return new AIDatabaseContext.Builder(new DBSLogicalDataSource(Mockito.mock(DBPDataSourceContainer.class)))
            .setScope(scope)
            .setExecutionContext(Mockito.mock(DBCExecutionContext.class));
    }
}
