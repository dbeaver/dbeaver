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
package org.jkiss.dbeaver.ext.mimer.model.data;

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBLOB;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBytes;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.sql.Types;

/**
 * {@link MimerBLOBValueHandler} reads a {@code BLOB} column via {@code
 * ResultSet.getBinaryStream()} instead of the base class's {@code getObject()} - see its own
 * javadoc for the Mimer SQL JDBC Driver limitation this works around. Exercised through the real,
 * inherited {@code fetchValueObject} (not the {@code protected fetchColumnValue} directly), so
 * the 0-based-to-1-based index translation that method does is covered too.
 *
 * @author Mimer Information Technology
 */
public class MimerBLOBValueHandlerTest extends DBeaverUnitTest {

    @Mock
    private DBCSession session;

    @Mock
    private DBCExecutionContext executionContext;

    @Mock
    private JDBCResultSet resultSet;

    @Mock
    private DBSTypedObject type;

    @BeforeEach
    public void setUp() {
        Mockito.when(session.getExecutionContext()).thenReturn(executionContext);
        Mockito.when(type.getTypeID()).thenReturn(Types.BLOB);
    }

    @Test
    public void readsBlobValueViaGetBinaryStreamNotGetObject() throws Exception {
        byte[] expected = {1, 2, 3, 4, 5};
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(new ByteArrayInputStream(expected));

        DBDContent result = (DBDContent) MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentBytes.class, result);
        Assertions.assertArrayEquals(expected, ((JDBCContentBytes) result).getRawValue());
        Mockito.verify(resultSet).getBinaryStream(1);
        Mockito.verify(resultSet, Mockito.never()).getObject(ArgumentMatchers.anyInt());
        Mockito.verify(resultSet, Mockito.never()).getBlob(ArgumentMatchers.anyInt());
        Mockito.verify(resultSet, Mockito.never()).getBytes(ArgumentMatchers.anyInt());
    }

    @Test
    public void nullBlobValueProducesAnEmptyContentWrapperRatherThanFailing() throws Exception {
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(null);

        DBDContent result = (DBDContent) MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentBLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }
}
