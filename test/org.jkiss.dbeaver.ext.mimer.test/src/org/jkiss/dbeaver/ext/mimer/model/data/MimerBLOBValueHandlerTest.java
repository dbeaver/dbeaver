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
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBLOB;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link MimerBLOBValueHandler} reads a {@code BLOB} column via {@code
 * ResultSet.getBinaryStream()} instead of the base class's {@code getObject()} - see its own
 * javadoc for the Mimer SQL JDBC Driver limitation this works around, and for why it spools the
 * stream itself (bounded, cancellable) rather than handing it to the base class's unconditional
 * drain-to-memory fallback. Exercised through the real, inherited {@code fetchValueObject} (not
 * the {@code protected fetchColumnValue} directly), so the 0-based-to-1-based index translation
 * that method does is covered too. The small-content (stays-in-memory) path is the main one
 * covered here; the temp-file spill path is only re-tested where this class's own logic around
 * it - not the underlying {@code ContentUtils}/{@code TemporaryContentStorage} machinery core
 * itself already covers for a real {@code Blob} - is actually at risk: cleanup on failure, and
 * per-call temp file naming.
 *
 * @author Mimer Information Technology
 */
public class MimerBLOBValueHandlerTest extends DBeaverUnitTest {

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
        Mockito.when(type.getTypeID()).thenReturn(Types.BLOB);
    }

    @Test
    public void readsBlobValueViaGetBinaryStreamNotGetObject() throws Exception {
        byte[] expected = {1, 2, 3, 4, 5};
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(new ByteArrayInputStream(expected));

        DBDContent result = (DBDContent) MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentBLOB.class, result);
        DBDContentStorage storage = ((JDBCContentBLOB) result).getContents(progressMonitor);
        Assertions.assertInstanceOf(DBDContentCached.class, storage);
        Assertions.assertArrayEquals(expected, (byte[]) ((DBDContentCached) storage).getCachedValue());
        Mockito.verify(resultSet).getBinaryStream(1);
        Mockito.verify(resultSet, Mockito.never()).getObject(ArgumentMatchers.anyInt());
        Mockito.verify(resultSet, Mockito.never()).getBlob(ArgumentMatchers.anyInt());
        Mockito.verify(resultSet, Mockito.never()).getBytes(ArgumentMatchers.anyInt());
    }

    @Test
    public void cancellingPartwayThroughStillReturnsWhateverWasReadSoFar() throws Exception {
        byte[] expected = {1, 2, 3, 4, 5};
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(new ByteArrayInputStream(expected));
        Mockito.when(progressMonitor.isCanceled()).thenReturn(true);

        DBDContent result = (DBDContent) MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentBLOB.class, result);
        Assertions.assertFalse(result.isNull());
    }

    @Test
    public void nullBlobValueProducesAnEmptyContentWrapperRatherThanFailing() throws Exception {
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(null);

        DBDContent result = (DBDContent) MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentBLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }

    @Test
    public void nullValueStaysABlobWrapperEvenWhenTheTypeIdIsNotReliable() throws Exception {
        // MimerValueHandlerProvider's type-name fallback can dispatch here with a type ID that
        // isn't Types.BLOB (the driver's own vendor code for a national LOB variant) - the null
        // branch must not redispatch on that ID, or it would silently come back as a binary
        // JDBCContentBytes wrapper instead.
        Mockito.when(type.getTypeID()).thenReturn(-10);
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(null);

        DBDContent result = (DBDContent) MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(JDBCContentBLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }

    @Test
    public void getValueFromObjectWithNullStaysABlobWrapperEvenWhenTheTypeIdIsNotReliable() throws Exception {
        // New-row initialization and the "Set to NULL" grid action both call
        // getValueFromObject(session, type, null, false, false) directly - not through
        // fetchColumnValue/fetchValueObject, so that path needs its own coverage separate from
        // nullValueStaysABlobWrapperEvenWhenTheTypeIdIsNotReliable above.
        Mockito.when(type.getTypeID()).thenReturn(-10);

        DBDContent result = MimerBLOBValueHandler.INSTANCE.getValueFromObject(session, type, null, false, false);

        Assertions.assertInstanceOf(JDBCContentBLOB.class, result);
        Assertions.assertTrue(result.isNull());
    }

    @Test
    public void displayStringReturnsNullForASqlNullValueInsteadOfAZeroLengthPlaceholder() {
        MimerContentBLOB content = new MimerContentBLOB(executionContext);

        Assertions.assertNull(content.getDisplayString(DBDDisplayFormat.UI),
            "A SQL NULL value should display as NULL, not a zero-length \"[BLOB, 0B]\" placeholder");
    }

    @Test
    public void noTempFileIsLeftBehindWhenSomethingGoesWrongWhileSpillingToDisk() throws Exception {
        // Past MEMORY_CONTENT_MAX_SIZE so fetchColumnValue has already committed to the
        // temp-file branch before the stream fails - exactly the case the Throwable catch
        // (as opposed to the IOException one, which OutputStream#write/ContentUtils#copyStreams
        // would raise on their own) exists for.
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        Path lobFolder = ContentUtils.getLobFolder(progressMonitor, DBWorkbench.getPlatform());
        Set<Path> before = listFiles(lobFolder);
        // Comfortably past memoryLimit, with enough slack that the initial in-memory read
        // loop (which reads in chunks, not byte-by-byte) finishes normally and we're already
        // into the temp-file copyStreams() phase by the time this budget runs out.
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(failingAfter(memoryLimit + 50_000));

        Assertions.assertThrows(DBCException.class,
            () -> MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0));

        Assertions.assertEquals(before, listFiles(lobFolder), "No leftover temp file should remain after a failed spill");
    }

    @Test
    public void noTempFileIsLeftBehindWhenClosingTheStreamFailsAfterASuccessfulSpill() throws Exception {
        // Unlike the test above, the spill itself succeeds here - only the stream's own close()
        // (in the finally) fails afterward. The temp file the just-built content already owns is
        // otherwise unreachable once this throws, since nothing else holds a reference to call
        // release() on it - this is what would leak a temp file on every retry without the fix.
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        byte[] largeContent = new byte[memoryLimit + 1000];
        Path lobFolder = ContentUtils.getLobFolder(progressMonitor, DBWorkbench.getPlatform());
        Set<Path> before = listFiles(lobFolder);
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(failingToCloseAfter(largeContent));

        Assertions.assertThrows(DBCException.class,
            () -> MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0));

        Assertions.assertEquals(before, listFiles(lobFolder),
            "No leftover temp file should remain after a close failure following a successful spill");
    }

    @Test
    public void repeatedSpillsToDiskInQuickSuccessionDoNotCollideOnTheSameMillisecond() throws Exception {
        // ContentUtils.makeTempFile names the file "<name>-" + System.currentTimeMillis() +
        // ".data" - millisecond resolution, with no uniqueness of its own beyond whatever name
        // the caller passes in. Spilling enough times in a tight loop reliably lands two or more
        // calls in the same millisecond on any normal machine - this is what actually threw
        // FileAlreadyExistsException live before TEMP_FILE_SEQUENCE's per-call counter was added.
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        byte[] largeContent = new byte[memoryLimit + 1000];
        Path lobFolder = ContentUtils.getLobFolder(progressMonitor, DBWorkbench.getPlatform());
        Set<Path> before = listFiles(lobFolder);

        JDBCContentBLOB[] created = new JDBCContentBLOB[30];
        try {
            for (int i = 0; i < created.length; i++) {
                Mockito.when(resultSet.getBinaryStream(1)).thenReturn(new ByteArrayInputStream(largeContent));
                created[i] = (JDBCContentBLOB) MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);
            }

            Assertions.assertEquals(before.size() + created.length, listFiles(lobFolder).size(),
                "Every spill should get its own, distinct temp file, not overwrite an earlier one");
        } finally {
            for (JDBCContentBLOB content : created) {
                if (content != null) {
                    content.release();
                }
            }
        }
    }

    @Test
    public void nonIOExceptionDuringTheInitialReadStillWrapsIntoDBCException() throws Exception {
        // Well under memoryLimit, so this fails inside the initial in-memory read loop - before
        // any temp file exists, and before the loop's own "copy the rest to disk" branch is ever
        // reached. fetchColumnValue's own catch has to be broad enough to still convert this to
        // a DBCException rather than letting the raw exception escape uncaught.
        Mockito.when(resultSet.getBinaryStream(1)).thenReturn(failingAfter(5));

        Assertions.assertThrows(DBCException.class,
            () -> MimerBLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0));
    }

    @NotNull
    private static InputStream failingAfter(int byteCount) {
        return new InputStream() {
            private int remaining = byteCount;

            @Override
            public int read() {
                if (remaining-- > 0) {
                    return 'x';
                }
                throw new SimulatedFailure("Simulated non-IOException failure mid-copy");
            }
        };
    }

    private static final class SimulatedFailure extends RuntimeException {
        SimulatedFailure(String message) {
            super(message);
        }
    }

    @NotNull
    private static InputStream failingToCloseAfter(byte[] content) {
        return new ByteArrayInputStream(content) {
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

    @Test
    public void displayStringShowsSizeOnceSpilledToATempFileInsteadOfStayingInMemory() throws Exception {
        // Not exercised through fetchValueObject - a real spill needs real file I/O (see the
        // class javadoc for why that's not re-tested here). A plain DBDContentStorage mock is
        // not a DBDContentCached, matching exactly what TemporaryContentStorage looks like to
        // getDisplayString - that's the only thing this override actually branches on.
        DBDContentStorage spilledStorage = Mockito.mock(DBDContentStorage.class);
        Mockito.when(spilledStorage.getContentLength()).thenReturn(277_000L);
        MimerContentBLOB content = new MimerContentBLOB(executionContext);
        content.setStorage(spilledStorage);

        Assertions.assertEquals("[BLOB, 270KiB]", content.getDisplayString(DBDDisplayFormat.UI));
    }
}
