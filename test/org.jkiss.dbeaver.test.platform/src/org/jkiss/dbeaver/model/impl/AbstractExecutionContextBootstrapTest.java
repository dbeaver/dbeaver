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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractExecutionContextBootstrapTest {
    @Test
    public void executesExactValidatedSnapshotDespiteConfigurationAndVariableChanges() throws Exception {
        try (var handlers = Mockito.mockStatic(QMUtils.class)) {
            handlers.when(QMUtils::getDefaultHandler).thenReturn(Mockito.mock(QMExecutionHandler.class));
            AbstractExecutionContext<?, ?> context = Mockito.mock(AbstractExecutionContext.class, Mockito.CALLS_REAL_METHODS);
            DBPDataSource dataSource = Mockito.mock(DBPDataSource.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.doReturn(dataSource).when(context).getDataSource();
            AtomicReference<String> variable = new AtomicReference<>("1");
            Mockito.when(dataSource.getContainer().getVariablesResolver(true)).thenReturn(name -> variable.get());
            DBPConnectionBootstrap bootstrap = new DBPConnectionBootstrap();
            bootstrap.getInitQueries().add("CALL ${value}");
            Mockito.doReturn(bootstrap).when(context).getBootstrapSettings();
            Mockito.doAnswer(invocation -> {
                Assertions.assertEquals(List.of("CALL 1"), invocation.getArgument(0));
                variable.set("2");
                bootstrap.getInitQueries().add("CALL 3");
                return null;
            }).when(context).validateBootstrapQueries(Mockito.anyList());
            DBCSession session = Mockito.mock(DBCSession.class);
            Mockito.doReturn(session).when(context).openSession(Mockito.any(), Mockito.any(), Mockito.anyString());
            DBCStatement statement = Mockito.mock(DBCStatement.class);
            Mockito.when(session.prepareStatement(DBCStatementType.SCRIPT, "CALL 1", false, false, false)).thenReturn(statement);

            Assertions.assertTrue(context.initContextBootstrap(new VoidProgressMonitor(), true));

            Mockito.verify(statement).executeStatement();
            Mockito.verify(session).prepareStatement(DBCStatementType.SCRIPT, "CALL 1", false, false, false);
            Mockito.verify(statement).close();
            Mockito.verifyNoMoreInteractions(statement);
        }
    }

    @Test
    public void denialCannotBeIgnoredByBootstrapIgnoreErrors() throws Exception {
        try (var handlers = Mockito.mockStatic(QMUtils.class)) {
            handlers.when(QMUtils::getDefaultHandler).thenReturn(Mockito.mock(QMExecutionHandler.class));
            AbstractExecutionContext<?, ?> context = Mockito.mock(AbstractExecutionContext.class, Mockito.CALLS_REAL_METHODS);
            DBPDataSource dataSource = Mockito.mock(DBPDataSource.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.doReturn(dataSource).when(context).getDataSource();
            DBPConnectionBootstrap bootstrap = new DBPConnectionBootstrap();
            bootstrap.setIgnoreErrors(true);
            bootstrap.getInitQueries().add("CALL 1");
            Mockito.doReturn(bootstrap).when(context).getBootstrapSettings();
            Mockito.doThrow(new DBCException("Declined")).when(context).validateBootstrapQueries(Mockito.anyList());

            Assertions.assertThrows(DBCException.class, () -> context.initContextBootstrap(new VoidProgressMonitor(), true));

            Mockito.verify(context, Mockito.never()).openSession(Mockito.any(), Mockito.any(), Mockito.anyString());
        }
    }
}
