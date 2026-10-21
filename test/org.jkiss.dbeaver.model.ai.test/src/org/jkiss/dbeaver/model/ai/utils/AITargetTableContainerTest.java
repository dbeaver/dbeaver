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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AITargetTableContainerTest extends DBeaverUnitTest {
    private final AIDatabaseContext databaseContext = Mockito.mock(AIDatabaseContext.class);
    private final DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
    private final DBCExecutionContextDefaults defaults = Mockito.mock(DBCExecutionContextDefaults.class);
    private final DBPDataSource dataSource = Mockito.mock(DBPDataSource.class,
        Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
    private final DBSObjectContainer root = (DBSObjectContainer) dataSource;
    private final DBSSchema schema = Mockito.mock(DBSSchema.class);
    private final AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), databaseContext,
        Mockito.mock(AIPromptGenerator.class));

    @BeforeEach
    public void setUp() throws DBException {
        Mockito.when(databaseContext.getExecutionContext()).thenReturn(executionContext);
        Mockito.when(databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.doReturn(defaults).when(executionContext).getContextDefaults();
        Mockito.when(defaults.getDefaultSchema()).thenReturn(schema);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        Mockito.doReturn(DBSSchema.class).when(root).getPrimaryChildType(Mockito.any());
        Mockito.doReturn(DBSTable.class).when(schema).getPrimaryChildType(Mockito.any());
    }

    @Test
    public void newTableUsesSelectedTargetsDefaultSchema() throws DBException {
        Assertions.assertSame(schema, AIUtils.findTableContainerInContext(context, "album"));
        Assertions.assertSame(schema, AIUtils.findTableContainerInContext(context, null));
        Mockito.verify(root, Mockito.never()).getChild(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void qualifiedNewTableResolvesExplicitSchemaWithQuotedDots() throws DBException {
        DBSSchema otherSchema = Mockito.mock(DBSSchema.class);
        Mockito.doReturn(DBSTable.class).when(otherSchema).getPrimaryChildType(Mockito.any());
        Mockito.when(root.getChild(Mockito.any(), Mockito.eq("analytics"))).thenReturn(otherSchema);
        Assertions.assertSame(otherSchema,
            AIUtils.findTableContainerInContext(context, "\"analytics\".\"New.Album\""));
    }

    @Test
    public void fullyQualifiedNewTableResolvesCatalogAndSchema() throws DBException {
        DBSCatalog catalog = Mockito.mock(DBSCatalog.class);
        Mockito.doReturn(DBSCatalog.class).when(root).getPrimaryChildType(Mockito.any());
        Mockito.when(root.getChild(Mockito.any(), Mockito.eq("target_db"))).thenReturn(catalog);
        Mockito.when(catalog.getChild(Mockito.any(), Mockito.eq("analytics"))).thenReturn(schema);
        Assertions.assertSame(schema,
            AIUtils.findTableContainerInContext(context, "\"target_db\".\"analytics\".\"album\""));
    }

    @Test
    public void unknownSchemaDoesNotFallBackToDefaultSchema() {
        Assertions.assertThrows(DBException.class,
            () -> AIUtils.findTableContainerInContext(context, "missing_schema.album"));
    }

    @Test
    public void excludedTargetSchemaIsRejected() {
        Mockito.when(databaseContext.isObjectExcluded(schema)).thenReturn(true);
        Assertions.assertThrows(DBException.class, () -> AIUtils.findTableContainerInContext(context, "album"));
    }

    @Test
    public void requiresSchemaWhenConnectionDefaultIsNotATableContainer() {
        Mockito.when(defaults.getDefaultSchema()).thenReturn(null);
        Assertions.assertThrows(DBException.class, () -> AIUtils.findTableContainerInContext(context, "album"));
    }

    @Test
    public void metadataErrorsArePropagated() throws DBException {
        DBException failure = new DBException("Metadata permission denied");
        Mockito.when(schema.getPrimaryChildType(Mockito.any())).thenThrow(failure);
        Assertions.assertSame(failure,
            Assertions.assertThrows(DBException.class, () -> AIUtils.findTableContainerInContext(context, "album")));
    }

    @Test
    public void connectionWithoutSchemasUsesDataSourceAsTarget() throws DBException {
        Mockito.when(defaults.getDefaultSchema()).thenReturn(null);
        Mockito.doReturn(DBSTable.class).when(root).getPrimaryChildType(Mockito.any());
        Assertions.assertSame(root, AIUtils.findTableContainerInContext(context, "album"));
    }
}
