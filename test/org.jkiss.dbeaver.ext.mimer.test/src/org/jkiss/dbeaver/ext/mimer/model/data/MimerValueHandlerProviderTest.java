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

import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Types;

/**
 * {@link MimerValueHandlerProvider} is scoped to {@code java.sql.Types.BLOB}/{@code CLOB}/{@code
 * NCLOB} - every type the Mimer SQL JDBC Driver limitation {@link MimerBLOBValueHandler}/{@link
 * MimerCLOBValueHandler} work around affects (see their own javadoc). Every other
 * type must fall through to {@code null} (core's own default handler), not get silently
 * redirected too - this plugin.xml's {@code <type>} list must stay in sync with this switch, see
 * {@link MimerValueHandlerProvider}'s own javadoc. The 11.0.6+ cutoff must return a real handler
 * ({@link JDBCContentValueHandler#INSTANCE}), never {@code null} - see that class's own javadoc
 * for why {@code null} here would silently fall through to {@code DefaultValueHandler} instead of
 * the core JDBC-standard provider.
 *
 * @author Mimer Information Technology
 */
public class MimerValueHandlerProviderTest extends DBeaverUnitTest {

    private final MimerValueHandlerProvider provider = new MimerValueHandlerProvider();

    @Mock
    private DBPDataSource dataSource;

    @Mock
    private MimerDataSource mimerDataSource;

    @Mock
    private DBDFormatSettings preferences;

    @Mock
    private DBSTypedObject type;

    @Test
    public void returnsTheBlobHandlerForBlobType() {
        Mockito.when(type.getTypeID()).thenReturn(Types.BLOB);

        DBDValueHandler handler = provider.getValueHandler(dataSource, preferences, type);

        Assertions.assertSame(MimerBLOBValueHandler.INSTANCE, handler);
    }

    @Test
    public void returnsTheClobHandlerForClobType() {
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);

        DBDValueHandler handler = provider.getValueHandler(dataSource, preferences, type);

        Assertions.assertSame(MimerCLOBValueHandler.INSTANCE, handler);
    }

    @Test
    public void returnsTheClobHandlerForNclobTypeToo() {
        Mockito.when(type.getTypeID()).thenReturn(Types.NCLOB);

        DBDValueHandler handler = provider.getValueHandler(dataSource, preferences, type);

        Assertions.assertSame(MimerCLOBValueHandler.INSTANCE, handler);
    }

    @Test
    public void returnsNullForUnrelatedTypes() {
        Mockito.when(type.getTypeID()).thenReturn(Types.VARCHAR);

        Assertions.assertNull(provider.getValueHandler(dataSource, preferences, type));
    }

    @Test
    public void fallsBackToTheStandardContentHandlerNotNullOnceTheDriverIsFixed() {
        Mockito.when(mimerDataSource.isDriverVersionAtLeast(11, 0, 6)).thenReturn(true);
        Mockito.when(type.getTypeID()).thenReturn(Types.BLOB);

        DBDValueHandler handler = provider.getValueHandler(mimerDataSource, preferences, type);

        // Must be a real handler, not null - null would skip the global JDBCStandardValueHandlerProvider
        // entirely (DataTypeProviderRegistry#getValueHandlerProvider already committed to this,
        // the non-global match) and fall through to DefaultValueHandler, which can't bind values.
        Assertions.assertSame(JDBCContentValueHandler.INSTANCE, handler);
    }

    @Test
    public void fallsBackToTheStandardContentHandlerForClobAndNclobToo() {
        Mockito.when(mimerDataSource.isDriverVersionAtLeast(11, 0, 6)).thenReturn(true);

        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Assertions.assertSame(JDBCContentValueHandler.INSTANCE, provider.getValueHandler(mimerDataSource, preferences, type));

        Mockito.when(type.getTypeID()).thenReturn(Types.NCLOB);
        Assertions.assertSame(JDBCContentValueHandler.INSTANCE, provider.getValueHandler(mimerDataSource, preferences, type));
    }

    @Test
    public void stillUsesTheMimerHandlerBelowTheVersionCutoff() {
        Mockito.when(mimerDataSource.isDriverVersionAtLeast(11, 0, 6)).thenReturn(false);
        Mockito.when(type.getTypeID()).thenReturn(Types.BLOB);

        DBDValueHandler handler = provider.getValueHandler(mimerDataSource, preferences, type);

        Assertions.assertSame(MimerBLOBValueHandler.INSTANCE, handler);
    }
}
