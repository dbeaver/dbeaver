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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentCLOB;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link MimerCLOBValueHandler} reads a {@code CLOB}/{@code NCLOB} column via {@code
 * ResultSet.getCharacterStream()} instead of the base class's {@code getObject()} - see its own
 * javadoc for the Mimer SQL JDBC Driver limitation this works around, and for why it spools the
 * stream itself rather than the base class's unconditional drain-to-memory fallback. One handler
 * instance covers both type IDs, so both are exercised here. Same shape as {@code
 * MimerBLOBValueHandlerTest} for the binary side of the LOB family - see its own javadoc for why
 * the temp-file spill path isn't separately re-tested here.
 *
 * @author Mimer Information Technology
 */
public class MimerCLOBValueHandlerTest extends DBeaverUnitTest {

    @Mock
    private DBCSession session;

    @Mock
    private DBCExecutionContext executionContext;

    @Mock
    private DBRProgressMonitor progressMonitor;

    @Mock
    private DBPDataSource dataSource;

    @Mock
    private DBPDataSourceContainer dataSourceContainer;

    @Mock
    private DBPPreferenceStore preferenceStore;

    @Mock
    private JDBCResultSet resultSet;

    @Mock
    private DBSTypedObject type;

    @BeforeEach
    public void setUp() {
        Mockito.when(session.getExecutionContext()).thenReturn(executionContext);
        Mockito.when(session.getProgressMonitor()).thenReturn(progressMonitor);
        Mockito.when(progressMonitor.isCanceled()).thenReturn(false);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);
    }

    @Test
    public void readsClobValueViaGetCharacterStreamNotGetObject() throws Exception {
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("hello clob"));

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentCLOB.class, result);
        DBDContentStorage storage = ((JDBCContentCLOB) result).getContents(progressMonitor);
        Assertions.assertInstanceOf(DBDContentCached.class, storage);
        Assertions.assertEquals("hello clob", ((DBDContentCached) storage).getCachedValue());
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

        Assertions.assertInstanceOf(JDBCContentCLOB.class, result);
        DBDContentStorage storage = ((JDBCContentCLOB) result).getContents(progressMonitor);
        Assertions.assertEquals("hello nclob", ((DBDContentCached) storage).getCachedValue());
    }

    @Test
    public void nullClobValueProducesAnEmptyContentWrapperRatherThanFailing() throws Exception {
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(null);

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentCLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }

    @Test
    public void nullValueStaysAClobWrapperEvenWhenTheTypeIdIsNotReliable() throws Exception {
        // MimerValueHandlerProvider's type-name fallback can dispatch here with a type ID that
        // isn't Types.CLOB/NCLOB (the driver's own vendor code for NATIONAL CHAR LARGE OBJECT) -
        // the null branch must not redispatch on that ID, or it would silently come back as a
        // binary JDBCContentBytes wrapper instead.
        Mockito.when(type.getTypeID()).thenReturn(-10);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(null);

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentCLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }

    @Test
    public void getValueFromObjectWithNullStaysAClobWrapperEvenWhenTheTypeIdIsNotReliable() throws Exception {
        // New-row initialization and the "Set to NULL" grid action both call
        // getValueFromObject(session, type, null, false, false) directly - not through
        // fetchColumnValue/fetchValueObject, so that path needs its own coverage separate from
        // nullValueStaysAClobWrapperEvenWhenTheTypeIdIsNotReliable above.
        Mockito.when(type.getTypeID()).thenReturn(-10);

        DBDContent result = MimerCLOBValueHandler.INSTANCE.getValueFromObject(session, type, null, false, false);

        Assertions.assertInstanceOf(JDBCContentCLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }

    @Test
    public void displayStringReturnsNullForASqlNullValueInsteadOfAZeroLengthPlaceholder() {
        MimerContentCLOB content = new MimerContentCLOB(executionContext);

        Assertions.assertNull(content.getDisplayString(DBDDisplayFormat.UI),
            "A SQL NULL value should display as NULL, not a zero-length \"[CLOB, 0B]\" placeholder");
    }

    @Test
    public void displayStringShowsSizeOnceSpilledToATempFileInsteadOfStayingInMemory() throws Exception {
        // See MimerBLOBValueHandlerTest's identical test for why a plain DBDContentStorage mock
        // (not a DBDContentCached) stands in for a real spilled TemporaryContentStorage here.
        DBDContentStorage spilledStorage = Mockito.mock(DBDContentStorage.class);
        Mockito.when(spilledStorage.getContentLength()).thenReturn(56_550L);
        MimerContentCLOB content = new MimerContentCLOB(executionContext);
        content.setStorage(spilledStorage);

        Assertions.assertEquals("[CLOB, 55KiB]", content.getDisplayString(DBDDisplayFormat.UI));
    }

    @Test
    public void noTempFileIsLeftBehindWhenSomethingGoesWrongWhileSpillingToDisk() throws Exception {
        // See MimerBLOBValueHandlerTest's identical test for why this specifically targets the
        // Throwable catch (as opposed to the IOException one) around the temp-file write.
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        Path lobFolder = ContentUtils.getLobFolder(progressMonitor, DBWorkbench.getPlatform());
        Set<Path> before = listFiles(lobFolder);
        // Comfortably past memoryLimit, with enough slack that the initial in-memory read
        // loop (which reads in chunks, not char-by-char) finishes normally and we're already
        // into the temp-file copyStreams() phase by the time this budget runs out.
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(failingAfter(memoryLimit + 50_000));

        Assertions.assertThrows(DBCException.class,
            () -> MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0));

        Assertions.assertEquals(before, listFiles(lobFolder), "No leftover temp file should remain after a failed spill");
    }

    @Test
    public void noTempFileIsLeftBehindWhenClosingTheReaderFailsAfterASuccessfulSpill() throws Exception {
        // See MimerBLOBValueHandlerTest's identical test for why this specifically targets close()
        // failing after the spill itself already succeeded.
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        char[] largeContent = new char[memoryLimit + 1000];
        java.util.Arrays.fill(largeContent, 'x');
        Path lobFolder = ContentUtils.getLobFolder(progressMonitor, DBWorkbench.getPlatform());
        Set<Path> before = listFiles(lobFolder);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(failingToCloseAfter(largeContent));

        Assertions.assertThrows(DBCException.class,
            () -> MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0));

        Assertions.assertEquals(before, listFiles(lobFolder),
            "No leftover temp file should remain after a close failure following a successful spill");
    }

    @Test
    public void repeatedSpillsToDiskInQuickSuccessionDoNotCollideOnTheSameMillisecond() throws Exception {
        // See MimerBLOBValueHandlerTest's identical test for why this reliably reproduces the
        // real, live FileAlreadyExistsException TEMP_FILE_SEQUENCE's per-call counter fixes.
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        char[] largeContent = new char[memoryLimit + 1000];
        java.util.Arrays.fill(largeContent, 'x');
        Path lobFolder = ContentUtils.getLobFolder(progressMonitor, DBWorkbench.getPlatform());
        Set<Path> before = listFiles(lobFolder);

        JDBCContentCLOB[] created = new JDBCContentCLOB[30];
        try {
            for (int i = 0; i < created.length; i++) {
                Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new java.io.CharArrayReader(largeContent));
                created[i] = (JDBCContentCLOB) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);
            }

            Assertions.assertEquals(before.size() + created.length, listFiles(lobFolder).size(),
                "Every spill should get its own, distinct temp file, not overwrite an earlier one");
        } finally {
            for (JDBCContentCLOB content : created) {
                if (content != null) {
                    content.release();
                }
            }
        }
    }

    @Test
    public void nonIOExceptionDuringTheInitialReadStillWrapsIntoDBCException() throws Exception {
        // See MimerBLOBValueHandlerTest's identical test for why this specifically targets the
        // in-memory read loop, before any temp file exists.
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(failingAfter(5));

        Assertions.assertThrows(DBCException.class,
            () -> MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0));
    }

    @NotNull
    private static Reader failingAfter(int charCount) {
        return new Reader() {
            private int remaining = charCount;

            @Override
            public int read(char[] cbuf, int off, int len) {
                if (remaining > 0) {
                    cbuf[off] = 'x';
                    remaining--;
                    return 1;
                }
                throw new SimulatedFailure("Simulated non-IOException failure mid-copy");
            }

            @Override
            public void close() {
            }
        };
    }

    private static final class SimulatedFailure extends RuntimeException {
        SimulatedFailure(String message) {
            super(message);
        }
    }

    @NotNull
    private static Reader failingToCloseAfter(char[] content) {
        // Not a CharArrayReader subclass - its own close() has no throws clause at all (confirmed
        // via javap, not guessed), so it can't be overridden to throw a real IOException. Reader's
        // own close() is abstract with one, so implementing it directly can.
        return new Reader() {
            private int position = 0;

            @Override
            public int read(char[] cbuf, int off, int len) {
                if (position >= content.length) {
                    return -1;
                }
                int n = Math.min(len, content.length - position);
                System.arraycopy(content, position, cbuf, off, n);
                position += n;
                return n;
            }

            @Override
            public void close() throws IOException {
                throw new IOException("Simulated close failure after a successful read");
            }
        };
    }

    @NotNull
    private static Set<Path> listFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return Set.of();
        }
        try (var files = Files.list(dir)) {
            return files.collect(Collectors.toSet());
        }
    }
}
