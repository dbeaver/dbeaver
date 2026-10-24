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

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Connection;
import java.util.List;

/**
 * A canceled operation that never ends (dead network) gets its connection closed, so that the editor and the
 * next statements on that connection are not stuck forever.
 */
public class PostgreCanceledExecutionAbortTest extends DBeaverUnitTest {

    private static final long ABORT_TIMEOUT = 200;
    private static final long WAIT_TIMEOUT = 5000;

    @Mock
    DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    DBRBlockingObject mockBlock;
    @Mock
    JDBCExecutionContext mockContext;
    @Mock
    Connection mockConnection;

    private PostgreDataSource dataSource;

    @BeforeEach
    public void setUp() throws Exception {
        SimplePreferenceStore preferenceStore = new SimplePreferenceStore() {
            @Override
            public void save() {
            }
        };
        preferenceStore.setValue(ModelPreferences.EXECUTE_CANCEL_ABORT_TIMEOUT, ABORT_TIMEOUT);
        Mockito.lenient().when(mockDataSourceContainer.getDriver())
            .thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("postgresql"));
        Mockito.lenient().when(mockDataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);
        Mockito.lenient().when(mockDataSourceContainer.getName()).thenReturn("Test DS");
        Mockito.lenient().when(mockContext.getConnectionOrNull()).thenReturn(mockConnection);
        Mockito.lenient().when(mockContext.getContextName()).thenReturn("Main");

        dataSource = new PostgreDataSource(mockDataSourceContainer, "PG Test", "postgres");
    }

    @Test
    public void abortsConnectionWhenCanceledOperationDoesNotEnd() throws Exception {
        operationStillRunning(true);
        Mockito.when(mockContext.isAutoCommit()).thenReturn(true);

        dataSource.scheduleCanceledExecutionAbort(monitor, mockBlock, mockContext);

        Mockito.verify(mockConnection, Mockito.timeout(WAIT_TIMEOUT)).abort(Mockito.any());
    }

    @Test
    public void keepsConnectionWhenCanceledOperationEnded() throws Exception {
        operationStillRunning(false);
        Mockito.lenient().when(mockContext.isAutoCommit()).thenReturn(true);

        dataSource.scheduleCanceledExecutionAbort(monitor, mockBlock, mockContext);

        Mockito.verify(mockConnection, Mockito.after(ABORT_TIMEOUT * 5).never()).abort(Mockito.any());
    }

    @Test
    public void keepsConnectionWithOpenTransaction() throws Exception {
        operationStillRunning(true);
        Mockito.when(mockContext.isAutoCommit()).thenReturn(false);

        dataSource.scheduleCanceledExecutionAbort(monitor, mockBlock, mockContext);

        Mockito.verify(mockConnection, Mockito.after(ABORT_TIMEOUT * 5).never()).abort(Mockito.any());
    }

    private void operationStillRunning(boolean running) {
        List<DBRBlockingObject> blocks = running ? List.of(mockBlock) : List.of();
        Mockito.when(monitor.getActiveBlocks()).thenReturn(blocks);
    }
}
