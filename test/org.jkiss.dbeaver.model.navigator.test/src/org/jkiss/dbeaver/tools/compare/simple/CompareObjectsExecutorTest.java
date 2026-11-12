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
package org.jkiss.dbeaver.tools.compare.simple;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

public class CompareObjectsExecutorTest extends DBeaverUnitTest {

    @Test
    public void testCompareObjectsRefreshMetadataSuccess() throws Exception {
        CompareObjectsSettings settings = new CompareObjectsSettings();
        settings.setRefreshMetadata(true);

        DBNDatabaseNode parentNode = Mockito.mock(DBNDatabaseNode.class);
        DBNDatabaseNode node = Mockito.mock(DBNDatabaseNode.class);
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(node.getObject()).thenReturn(object);
        Mockito.when(node.getParentNode()).thenReturn(parentNode);
        Mockito.when(parentNode.getChildren(Mockito.any())).thenReturn(new DBNDatabaseNode[]{node});
        Mockito.when(node.refreshNode(Mockito.any(), Mockito.eq(DBNEvent.FORCE_REFRESH))).thenReturn(parentNode);
        Mockito.when(node.isDisposed()).thenReturn(false);

        settings.setNodes(List.of(node));

        try (MockedStatic<DBNUtils> dbnUtils = Mockito.mockStatic(DBNUtils.class)) {
            dbnUtils.when(() -> DBNUtils.getNodeByObject(Mockito.any(), Mockito.eq(object), Mockito.eq(false)))
                .thenReturn(node);

            CompareObjectsExecutor executor = new CompareObjectsExecutor(settings);
            CompareReport report = executor.compareObjects(new VoidProgressMonitor(), List.of(node));
            Assertions.assertNotNull(report);
        }
    }

    @Test
    public void testCompareObjectsRefreshMetadataStaleDetachedNodeThrowsException() throws Exception {
        CompareObjectsSettings settings = new CompareObjectsSettings();
        settings.setRefreshMetadata(true);

        DBNDatabaseNode parentNode = Mockito.mock(DBNDatabaseNode.class);
        DBNDatabaseNode node = Mockito.mock(DBNDatabaseNode.class);
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(node.getObject()).thenReturn(object);
        Mockito.when(node.getParentNode()).thenReturn(parentNode);
        // Simulate that parent's children no longer contain node (detached node remaining undisposed)
        Mockito.when(parentNode.getChildren(Mockito.any())).thenReturn(new DBNDatabaseNode[0]);
        Mockito.when(node.refreshNode(Mockito.any(), Mockito.eq(DBNEvent.FORCE_REFRESH))).thenReturn(parentNode);
        Mockito.when(node.isDisposed()).thenReturn(false);
        Mockito.when(node.getNodeFullName()).thenReturn("TestTable");

        settings.setNodes(List.of(node));

        CompareObjectsExecutor executor = new CompareObjectsExecutor(settings);
        Assertions.assertThrows(DBException.class, () -> {
            executor.compareObjects(new VoidProgressMonitor(), List.of(node));
        });
    }

    @Test
    public void testCompareObjectsRefreshMetadataDroppedRootThrowsException() throws Exception {
        CompareObjectsSettings settings = new CompareObjectsSettings();
        settings.setRefreshMetadata(true);

        DBNDatabaseNode node = Mockito.mock(DBNDatabaseNode.class);
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(node.getObject()).thenReturn(object);
        Mockito.when(node.refreshNode(Mockito.any(), Mockito.eq(DBNEvent.FORCE_REFRESH))).thenReturn(node);
        Mockito.when(node.isDisposed()).thenReturn(false);
        Mockito.when(node.getNodeFullName()).thenReturn("TestTable");

        settings.setNodes(List.of(node));

        try (MockedStatic<DBNUtils> dbnUtils = Mockito.mockStatic(DBNUtils.class)) {
            // Simulate that the object/node is no longer attached (dropped during refresh)
            dbnUtils.when(() -> DBNUtils.getNodeByObject(Mockito.any(), Mockito.eq(object), Mockito.eq(false)))
                .thenReturn(null);

            CompareObjectsExecutor executor = new CompareObjectsExecutor(settings);
            Assertions.assertThrows(DBException.class, () -> {
                executor.compareObjects(new VoidProgressMonitor(), List.of(node));
            });
        }
    }
}
