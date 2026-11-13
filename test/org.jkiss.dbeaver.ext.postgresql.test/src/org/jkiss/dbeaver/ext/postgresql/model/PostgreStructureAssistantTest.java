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

import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.Collections;

public class PostgreStructureAssistantTest extends DBeaverUnitTest {
    @Test
    public void searchInUnconnectedDatabaseOpensItsMetadataContext() throws Exception {
        PostgreDataSource dataSource = Mockito.mock(PostgreDataSource.class, Mockito.RETURNS_DEEP_STUBS);
        PostgreDatabase database = Mockito.mock(PostgreDatabase.class);
        PostgreDatabase defaultDatabase = Mockito.mock(PostgreDatabase.class);
        PostgreExecutionContext defaultContext = Mockito.mock(PostgreExecutionContext.class);
        PostgreExecutionContext metadataContext = Mockito.mock(PostgreExecutionContext.class);
        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);

        Mockito.when(defaultContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(defaultContext.getDefaultCatalog()).thenReturn(defaultDatabase);
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(database.getDatabase()).thenReturn(database);
        Mockito.when(database.getMetaContext()).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            Mockito.when(database.getMetaContext()).thenReturn(metadataContext);
            return null;
        }).when(database).checkInstanceConnection(monitor);
        Mockito.when(metadataContext.openSession(
            ArgumentMatchers.eq(monitor), ArgumentMatchers.eq(DBCExecutionPurpose.META), ArgumentMatchers.anyString()
        ))
            .thenReturn(session);
        Mockito.when(session.getProgressMonitor()).thenReturn(monitor);
        Mockito.when(session.prepareStatement(ArgumentMatchers.anyString())).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);

        DBSStructureAssistant.ObjectsSearchParams params = new DBSStructureAssistant.ObjectsSearchParams(
            new DBSObjectType[] {RelationalObjectType.TYPE_SCHEMA}, "test%"
        );
        params.setParentObject(database);
        params.setGlobalSearch(true);

        Assertions.assertEquals(
            Collections.emptyList(), new PostgreStructureAssistant(dataSource).findObjectsByMask(monitor, defaultContext, params)
        );

        InOrder inOrder = Mockito.inOrder(database, metadataContext);
        inOrder.verify(database).checkInstanceConnection(monitor);
        inOrder.verify(database).getMetaContext();
        inOrder.verify(metadataContext).openSession(
            ArgumentMatchers.eq(monitor), ArgumentMatchers.eq(DBCExecutionPurpose.META), ArgumentMatchers.anyString()
        );
        Mockito.verify(defaultContext, Mockito.never()).openSession(
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString()
        );
    }
}
