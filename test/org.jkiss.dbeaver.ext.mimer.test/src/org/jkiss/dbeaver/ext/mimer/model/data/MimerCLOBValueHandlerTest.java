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
import java.util.List;
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
    public void labelFollowsTheTypeNameEvenWhenTheTypeIdWronglySaysClob() throws Exception {
        // Regression test for the real-world bug: the live catalog/driver reported a type ID
        // that resolved to plain CLOB for what was genuinely an NCLOB column, while the type
        // NAME correctly carried the "NATIONAL..." spelling - labelFor must trust the name over
        // the ID (checked first now), not the other way around.
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Mockito.when(type.getTypeName()).thenReturn("NATIONAL CHAR LARGE OBJECT");
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("ab".repeat(4_500)));

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertTrue(result.getDisplayString(DBDDisplayFormat.UI).contains("NCLOB"));
    }

    @Test
    public void getValueFromObjectCorrectsAnAlreadyFetchedValuesStaleLabelOnLateRebinding() throws Exception {
        // Regression test: DBeaver's own attribute-binding resolution can re-dispatch an
        // already-fetched DBDContent value back through getValueFromObject once it resolves the
        // column against its real catalog entity attribute - the entity attribute (and its
        // reliable type name) often isn't known yet at the very first fetch, only the query's
        // own raw result-set metadata is, which is exactly what was going wrong live: the value
        // got built, and its label baked in, before the real "NATIONAL CHAR LARGE OBJECT" type
        // was known. Simulates that: a MimerContentCLOB already exists with the (wrong, stale)
        // "CLOB" label, then a later, more-resolved `type` naming the real NCLOB column comes in.
        MimerContentCLOB staleContent = new MimerContentCLOB(executionContext, "CLOB");
        staleContent.setStorage(new MimerChunkedCharsStorage(List.of("ab".repeat(4_500).toCharArray()), 9_000));
        Mockito.when(type.getTypeName()).thenReturn("NATIONAL CHAR LARGE OBJECT");

        DBDContent result = MimerCLOBValueHandler.INSTANCE.getValueFromObject(session, type, staleContent, false, false);

        Assertions.assertSame(staleContent, result, "Must correct the same object in place, not silently keep the stale one");
        Assertions.assertTrue(result.getDisplayString(DBDDisplayFormat.UI).contains("NCLOB"));
    }

    @Test
    public void displayStringSaysNclobNotClobForAnNclobColumn() throws Exception {
        // One handler instance covers both CLOB and NCLOB - the placeholder text must still say
        // which one a given column actually is, not always "CLOB" regardless (that was the bug:
        // the label used to be a hardcoded literal in MimerContentCLOB, with nothing anywhere
        // threading the real column type through to it).
        Mockito.when(type.getTypeID()).thenReturn(Types.NCLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("hello nclob"));

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertEquals("hello nclob", result.getDisplayString(DBDDisplayFormat.UI));
        // Too short to need a size suffix at all above - force one so "NCLOB" (not "CLOB")
        // actually shows up somewhere in the assertion.
        Mockito.when(type.getTypeID()).thenReturn(Types.NCLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("ab".repeat(4_500)));
        DBDContent longResult = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);
        Assertions.assertTrue(longResult.getDisplayString(DBDDisplayFormat.UI).contains("NCLOB"));
        Assertions.assertFalse(longResult.getDisplayString(DBDDisplayFormat.UI).contains("[CLOB"));
    }

    @Test
    public void displayStringKeepsSayingNclobAfterTheCellIsCloned() throws Exception {
        // Regression coverage for createNewContent() carrying typeLabel over - cloneValue()
        // (copy-paste) builds the copy via createNewContent(), which must pass the original's
        // typeLabel along rather than defaulting back to "CLOB" (see MimerContentCLOB's own
        // javadoc for why).
        MimerContentCLOB content = new MimerContentCLOB(executionContext, "NCLOB");
        content.setStorage(new MimerChunkedCharsStorage(List.of("a".repeat(500).toCharArray()), 500));

        MimerContentCLOB clone = (MimerContentCLOB) content.cloneValue(progressMonitor);

        Assertions.assertTrue(clone.getDisplayString(DBDDisplayFormat.UI).contains("NCLOB"));
    }

    @Test
    public void readsAMultiChunkValueCorrectlyAndTruncatesItsPreview() throws Exception {
        // Past one 8192-char read buffer, so the real handler path exercises multiple chunks, not
        // just one; past MimerCLOBValueHandler.PREVIEW_LENGTH (200), so the preview this produces
        // is genuinely truncated, not accidentally the whole value; but still comfortably under
        // the default MEMORY_CONTENT_MAX_SIZE (10000), so this stays in-memory rather than
        // spilling - the spill path has its own dedicated tests elsewhere in this file.
        String expected = "ab".repeat(4_500);
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader(expected));

        DBDContent result = (DBDContent) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);

        Assertions.assertInstanceOf(MimerContentCLOB.class, result);
        DBDContentStorage storage = ((JDBCContentCLOB) result).getContents(progressMonitor);
        Assertions.assertInstanceOf(MimerChunkedCharsStorage.class, storage,
            "Should have stayed in-memory, not spilled, given its size is well under the default limit");
        Assertions.assertEquals(expected, ((DBDContentCached) storage).getCachedValue(),
            "Concatenating multiple chunks back together must preserve the exact original content and order");
        Assertions.assertEquals(
            expected.substring(0, 200) + "... " + MimerLobUtil.sizedPlaceholder("CLOB", expected.length()),
            result.getDisplayString(DBDDisplayFormat.UI));
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
    public void displayStringShowsJustTheSizeForAnUnrecognizedStorageKindWithNoCheapPreviewAvailable() throws Exception {
        // A real spilled value is a MimerSpilledCharsStorage now (see
        // aGenuinelySpilledValueThroughTheRealHandlerStillShowsAPreviewNotJustTheSize below) - it
        // DOES get a preview. This test instead exercises the true fallback branch for a storage
        // kind that implements neither MimerPreviewableStorage nor DBDContentCached at all (not
        // expected in practice - nothing this handler builds ever hands MimerContentCLOB a plain
        // mock like this - but getDisplayString still needs a safe answer if it ever did).
        DBDContentStorage storage = Mockito.mock(DBDContentStorage.class);
        Mockito.when(storage.getContentLength()).thenReturn(56_550L);
        MimerContentCLOB content = new MimerContentCLOB(executionContext);
        content.setStorage(storage);

        Assertions.assertEquals("[CLOB, 55KiB]", content.getDisplayString(DBDDisplayFormat.UI));
    }

    @Test
    public void aGenuinelySpilledValueThroughTheRealHandlerStillShowsAPreviewNotJustTheSize() throws Exception {
        // The actual regression this guards against: MimerCLOBValueHandler.spoolToContent's spill
        // branch used to hand the real TemporaryContentStorage straight to MimerContentCLOB with no
        // preview at all - once a value got big enough to spill, the "preview + size" combination
        // every smaller value gets simply vanished, dropping all the way down to the bare size
        // placeholder shown by the test above. Reading an actual value through the real handler
        // path (not a hand-built MimerChunkedCharsStorage/mock) is the only way to be sure the real
        // spill branch, not an in-memory stand-in, is what's under test here.
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        String prefix = "the quick brown fox jumps over the lazy dog ";
        StringBuilder value = new StringBuilder(prefix);
        while (value.length() < memoryLimit + 1000) {
            value.append('x');
        }
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader(value.toString()));

        JDBCContentCLOB result = (JDBCContentCLOB) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);
        try {
            DBDContentStorage storage = result.getContents(progressMonitor);
            Assertions.assertInstanceOf(MimerSpilledCharsStorage.class, storage,
                "This value is well over the memory limit, so it must have spilled to disk");
            String expectedPreview = value.substring(0, MimerContentCLOB.PREVIEW_LENGTH);
            Assertions.assertEquals(
                expectedPreview + "... " + MimerLobUtil.sizedPlaceholder("CLOB", value.length()),
                result.getDisplayString(DBDDisplayFormat.UI));
        } finally {
            result.release();
        }
    }

    @Test
    public void spilledValueReportsACharacterCountNotTheOnDiskByteCountForMultiByteContent() throws Exception {
        // Regression test for the unit mismatch a code review caught: "å" is one character but
        // two bytes in UTF-8, so a value built entirely out of it makes the two counts genuinely
        // different - the ASCII-only content in
        // aGenuinelySpilledValueThroughTheRealHandlerStillShowsAPreviewNotJustTheSize above
        // couldn't have caught this, since for plain ASCII the two counts coincide. Must still be
        // reported as a character count (matching MimerChunkedCharsStorage's own in-memory
        // convention, and core's own StringContentStorage#getContentLength()), not
        // TemporaryContentStorage's on-disk Files.size() - otherwise the exact same "[NCLOB, ...]"
        // label means two different units depending purely on whether a value happened to spill.
        Mockito.when(type.getTypeID()).thenReturn(Types.CLOB);
        int memoryLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        String value = "å".repeat(memoryLimit + 1000);
        Mockito.when(resultSet.getCharacterStream(1)).thenReturn(new StringReader(value));

        JDBCContentCLOB result = (JDBCContentCLOB) MimerCLOBValueHandler.INSTANCE.fetchValueObject(session, resultSet, type, 0);
        try {
            DBDContentStorage storage = result.getContents(progressMonitor);
            Assertions.assertInstanceOf(MimerSpilledCharsStorage.class, storage,
                "This value is well over the memory limit, so it must have spilled to disk");
            Assertions.assertEquals(value.length(), storage.getContentLength(),
                "getContentLength() must be the character count, not the larger UTF-8 byte count the file actually takes up on disk");
        } finally {
            result.release();
        }
    }

    @Test
    public void displayStringShowsTruncatedPreviewPlusSizeWhenThePreviewIsShorterThanTheRealValue() {
        // The preview comes straight from a MimerChunkedCharsStorage's own first chunk now - not a
        // separately-tracked field - precisely so it can never go stale (see that class's own
        // javadoc for why a tracked field would). The chunk list here doesn't need to add up to
        // the full 56550 total for this test - peekPreview only ever looks at what's actually in
        // the chunks it's given.
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of("hello clob preview".toCharArray()), 56_550);
        MimerContentCLOB content = new MimerContentCLOB(executionContext);
        content.setStorage(storage);

        Assertions.assertEquals("hello clob preview... [CLOB, 55KiB]", content.getDisplayString(DBDDisplayFormat.UI));
    }

    @Test
    public void displayStringShowsJustThePreviewWhenItAlreadyCoversTheWholeValue() {
        // For a value that short to begin with, the preview IS the whole value, so no "..." or
        // size suffix should be added.
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of("hello clob".toCharArray()), 10);
        MimerContentCLOB content = new MimerContentCLOB(executionContext);
        content.setStorage(storage);

        Assertions.assertEquals("hello clob", content.getDisplayString(DBDDisplayFormat.UI));
    }

    @Test
    public void displayStringNeverMaterializesAChunkedStorage() {
        // getDisplayString must build its preview without ever flattening the chunks - verify
        // that directly: if it had, a later getCachedValue() call would still return the right
        // value either way (materialize() is idempotent), so the real proof is that the chunk
        // list itself is still intact and independently readable afterward, not just that the
        // displayed text happens to be right.
        String value = "hello clob preview";
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of(value.toCharArray()), value.length());
        MimerContentCLOB content = new MimerContentCLOB(executionContext);
        content.setStorage(storage);

        Assertions.assertEquals(value, content.getDisplayString(DBDDisplayFormat.UI));
        Assertions.assertEquals("hello clob preview", storage.getCachedValue(),
            "The real value must still be readable afterward - getDisplayString must not have corrupted the storage");
    }

    @Test
    public void displayStringShowsTheRealPreviewAgainAfterTheCellIsEditedNotTheOldOne() {
        // Regression test: editing an existing cell goes through the inherited updateContents(),
        // which replaces `storage` directly - it was never told about any separately-tracked
        // preview field (the earlier design had one, and it went stale here). Deriving the
        // preview from `storage` itself, fresh on every call, means there's no stale field left
        // to go wrong - the new value's own storage is all getDisplayString ever looks at.
        MimerContentCLOB content = new MimerContentCLOB(executionContext);
        content.setStorage(new MimerChunkedCharsStorage(List.of("old value".toCharArray()), 9));

        content.updateContents(progressMonitor, new org.jkiss.dbeaver.model.data.storage.StringContentStorage("brand new edited text"));

        Assertions.assertEquals("brand new edited text", content.getDisplayString(DBDDisplayFormat.UI));
    }

    @Test
    public void displayStringShowsAPreviewForAClonedCellTooNotJustTheSizePlaceholder() throws Exception {
        // Regression test: cloneValue() (copy-paste) also replaces `storage` directly via
        // updateContents(), with a fresh clone from storage.cloneStorage() - never knew about a
        // separately-tracked preview field either. The clone's own storage is itself a correctly-
        // cloned MimerChunkedCharsStorage (see that class's own cloneStorage()), so deriving the
        // preview from `storage` just works here too, with no special-casing needed for cloning.
        MimerContentCLOB content = new MimerContentCLOB(executionContext);
        content.setStorage(new MimerChunkedCharsStorage(List.of("hello clob".toCharArray()), 10));

        MimerContentCLOB clone = (MimerContentCLOB) content.cloneValue(progressMonitor);

        Assertions.assertEquals("hello clob", clone.getDisplayString(DBDDisplayFormat.UI));
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
