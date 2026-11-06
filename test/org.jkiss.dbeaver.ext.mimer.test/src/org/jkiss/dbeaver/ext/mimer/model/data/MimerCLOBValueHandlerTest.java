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
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentCLOB;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentChars;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.StringReader;
import java.sql.Types;

/**
 * {@link MimerCLOBValueHandler} reads a {@code CLOB}/{@code NCLOB} column via {@code
 * ResultSet.getCharacterStream()} instead of the base class's {@code getObject()} - see its own
 * javadoc for the Mimer SQL JDBC Driver limitation this works around. One handler instance
 * covers both type IDs, so both are exercised here. Same shape as {@code
 * MimerBLOBValueHandlerTest} for the binary side of the LOB family.
 *
 * @author Mimer Information Technology
 */
public class MimerCLOBValueHandlerTest extends DBeaverUnitTest {

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
    }

    @Test
    public void readsClobValueViaGetCharacterStreamNotGetObject() throws Exception {
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("hello clob"));

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentChars.class, result);
        Assertions.assertEquals("hello clob", ((JDBCContentChars) result).getRawValue());
        Mockito.verify(resultSet).getCharacterStream(1);
        Mockito.verify(resultSet, Mockito.never()).getObject(ArgumentMatchers.anyInt());
        Mockito.verify(resultSet, Mockito.never()).getClob(ArgumentMatchers.anyInt());
        Mockito.verify(resultSet, Mockito.never()).getString(ArgumentMatchers.anyInt());
    }

    @Test
    public void readsNclobValueViaGetCharacterStreamToo() throws Exception {
        Mockito.when(type.getTypeID()).thenReturn(Types.NCLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("hello nclob"));

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentChars.class, result);
        Assertions.assertEquals("hello nclob", ((JDBCContentChars) result).getRawValue());
    }

    @Test
    public void nullClobValueProducesAnEmptyContentWrapperRatherThanFailing() throws Exception {
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(null);

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentCLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }
}
