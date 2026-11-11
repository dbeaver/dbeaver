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
 * MimerCLOBValueHandler} work around affects (see their own javadoc) - plus a type-name fallback
 * for when the type ID doesn't land on one of those three (see {@link
 * MimerValueHandlerProvider}'s own javadoc for why that happens and why it's matched by name
 * rather than trusted to always be normalized away upstream). Every other type must fall through
 * to {@code null} (core's own default handler), not get silently redirected too - this
 * plugin.xml's {@code <type>} list must stay in sync with this switch. The 11.0.6+ cutoff must
 * return a real handler ({@link JDBCContentValueHandler#INSTANCE}), never {@code null} - see that
 * class's own javadoc for why {@code null} here would silently fall through to {@code
 * DefaultValueHandler} instead of the core JDBC-standard provider.
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

    // The type ID a real column reports is normally already normalized (see this class's own
    // javadoc) - these tests simulate the type ID NOT being one of BLOB/CLOB/NCLOB (a bogus
    // vendor-specific code, same shape as the confirmed-live NATIONAL CHAR LARGE OBJECT driver
    // bug) to pin down the name-based fallback independently of that normalization chain.
    private static final int UNRECOGNIZED_TYPE_ID = -10;

    @Test
    public void fallsBackToTheClobHandlerByNameWhenTheTypeIdDoesNotMatch() {
        Mockito.when(type.getTypeID()).thenReturn(UNRECOGNIZED_TYPE_ID);
        Mockito.when(type.getTypeName()).thenReturn("NATIONAL CHAR LARGE OBJECT");

        DBDValueHandler handler = provider.getValueHandler(dataSource, preferences, type);

        Assertions.assertSame(MimerCLOBValueHandler.INSTANCE, handler);
    }

    @Test
    public void fallsBackToTheClobHandlerForTheAnticipatedSpelledOutNationalName() {
        // A possible future catalog spelling instead of "NATIONAL CHAR LARGE OBJECT" above -
        // see MimerConstants#TYPE_NAMES_LARGE_OBJECT's own comment.
        Mockito.when(type.getTypeID()).thenReturn(UNRECOGNIZED_TYPE_ID);
        Mockito.when(type.getTypeName()).thenReturn("NATIONAL CHARACTER LARGE OBJECT");

        DBDValueHandler handler = provider.getValueHandler(dataSource, preferences, type);

        Assertions.assertSame(MimerCLOBValueHandler.INSTANCE, handler);
    }

    @Test
    public void fallsBackToTheBlobHandlerByNameWhenTheTypeIdDoesNotMatch() {
        Mockito.when(type.getTypeID()).thenReturn(UNRECOGNIZED_TYPE_ID);
        Mockito.when(type.getTypeName()).thenReturn("BINARY LARGE OBJECT");

        DBDValueHandler handler = provider.getValueHandler(dataSource, preferences, type);

        Assertions.assertSame(MimerBLOBValueHandler.INSTANCE, handler);
    }

    @Test
    public void returnsNullWhenNeitherTheTypeIdNorTheNameMatch() {
        Mockito.when(type.getTypeID()).thenReturn(UNRECOGNIZED_TYPE_ID);
        Mockito.when(type.getTypeName()).thenReturn("VARCHAR");

        Assertions.assertNull(provider.getValueHandler(dataSource, preferences, type));
    }

    @Test
    public void theNameFallbackIgnoresTheDriverVersionCutoff() {
        // The 11.0.6+ cutoff is for the separate getObject()/stream-read bug, not the vendor-ID
        // mismatch the name fallback exists for - a column only reached via that fallback still
        // has the wrong type ID regardless of driver version, so it must keep using the
        // Mimer-specific wrapper even past the cutoff.
        Mockito.when(mimerDataSource.isDriverVersionAtLeast(11, 0, 6)).thenReturn(true);
        Mockito.when(type.getTypeID()).thenReturn(UNRECOGNIZED_TYPE_ID);
        Mockito.when(type.getTypeName()).thenReturn("NATIONAL CHAR LARGE OBJECT");

        DBDValueHandler handler = provider.getValueHandler(mimerDataSource, preferences, type);

        Assertions.assertSame(MimerCLOBValueHandler.INSTANCE, handler);
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
