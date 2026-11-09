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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

class JDBCTransactionSavepointTest {
    @Test
    void protectsManualTransaction() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Savepoint savepoint = Mockito.mock(Savepoint.class);
        Mockito.when(connection.setSavepoint()).thenReturn(savepoint);

        Assertions.assertDoesNotThrow(() -> {
            try (JDBCTransactionSavepoint transaction = new JDBCTransactionSavepoint(connection)) {
                transaction.rollback();
            }
        });

        var calls = Mockito.inOrder(connection);
        calls.verify(connection).setSavepoint();
        calls.verify(connection).rollback(savepoint);
        calls.verify(connection).releaseSavepoint(savepoint);
    }

    @Test
    void autoCommitDoesNotCreateSavepoint() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> {
            try (JDBCTransactionSavepoint transaction = new JDBCTransactionSavepoint(connection)) {
                transaction.rollback();
            }
        });

        Mockito.verify(connection, Mockito.never()).setSavepoint();
        Mockito.verify(connection, Mockito.never()).rollback(Mockito.any(Savepoint.class));
        Mockito.verify(connection, Mockito.never()).releaseSavepoint(Mockito.any(Savepoint.class));
    }

    @Test
    void cannotProceedWithoutSavepointInManualTransaction() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.setSavepoint()).thenThrow(new SQLException("Savepoints unsupported"));

        Assertions.assertThrows(SQLException.class, () -> new JDBCTransactionSavepoint(connection));
    }
}
