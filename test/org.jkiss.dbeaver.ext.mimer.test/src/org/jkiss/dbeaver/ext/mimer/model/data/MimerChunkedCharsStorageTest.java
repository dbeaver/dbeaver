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

import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.util.List;

/**
 * The {@code char[]} sibling of {@link MimerChunkedBytesStorageTest} - see {@link
 * MimerChunkedCharsStorage}'s own javadoc.
 *
 * @author Mimer Information Technology
 */
public class MimerChunkedCharsStorageTest extends DBeaverUnitTest {

    @Mock
    private DBRProgressMonitor progressMonitor;

    @Test
    public void concatenatesMultipleChunksInOrderAndPreservesExactContent() throws Exception {
        char[] chunk1 = "hello ".toCharArray();
        char[] chunk2 = "mimer ".toCharArray();
        char[] chunk3 = "sql".toCharArray();
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of(chunk1, chunk2, chunk3), 15);

        Assertions.assertEquals("hello mimer sql", storage.getCachedValue());

        try (BufferedReader reader = new BufferedReader(storage.getContentReader())) {
            Assertions.assertEquals("hello mimer sql", reader.readLine());
        }
    }

    @Test
    public void getCachedValueIsComputedOnceAndCachedAfterward() {
        // Same reasoning as MimerChunkedBytesStorageTest's identical test - a second call that
        // weren't served from the cache would try to re-concatenate from the already-discarded
        // chunk list and throw.
        List<char[]> chunks = List.of("abc".toCharArray());
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(chunks, 3);

        Object first = storage.getCachedValue();
        Object second = storage.getCachedValue();

        Assertions.assertSame(first, second, "The second call must return the same cached String, not recompute it");
    }

    @Test
    public void getContentLengthNeverTouchesTheChunks() {
        @SuppressWarnings("unchecked")
        List<char[]> chunks = Mockito.mock(List.class);
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(chunks, 12345);

        Assertions.assertEquals(12345L, storage.getContentLength());

        Mockito.verifyNoInteractions(chunks);
    }

    @Test
    public void cloneStorageSharesTheSameUnflattenedChunksAndBothMaterializeCorrectly() throws Exception {
        List<char[]> chunks = List.of("xyz".toCharArray());
        MimerChunkedCharsStorage original = new MimerChunkedCharsStorage(chunks, 3);

        DBDContentStorage clone = original.cloneStorage(progressMonitor);

        Assertions.assertEquals("xyz", original.getCachedValue());
        Assertions.assertInstanceOf(MimerChunkedCharsStorage.class, clone);
        Assertions.assertEquals("xyz", ((DBDContentCached) clone).getCachedValue());
    }

    @Test
    public void handlesAnEmptyChunkListAsAZeroLengthValue() {
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of(), 0);

        Assertions.assertEquals(0L, storage.getContentLength());
        Assertions.assertEquals("", storage.getCachedValue());
    }

    @Test
    public void getCachedValueDegradesToEmptyInsteadOfThrowingWhenCalledAfterRelease() {
        // See MimerChunkedBytesStorageTest's identical test for why this matters - a caller can
        // still be holding this storage instance after release() ran on it elsewhere.
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of("abc".toCharArray()), 3);
        storage.release();

        Assertions.assertEquals("", storage.getCachedValue());
        Assertions.assertEquals(0L, storage.getContentLength(),
            "getContentLength() must agree with the degraded-empty getCachedValue() above, not keep reporting the original size");
    }

    @Test
    public void peekPreviewAlsoDegradesToEmptyInsteadOfThrowingWhenCalledAfterRelease() {
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of("abc".toCharArray()), 3);
        storage.release();

        Assertions.assertEquals("", storage.peekPreview(200));
    }

    @Test
    public void cloningAnAlreadyReleasedInstanceStillDegradesToEmptyNotZeroFilledGarbage() throws Exception {
        // See MimerChunkedBytesStorageTest's identical regression test for why.
        MimerChunkedCharsStorage storage = new MimerChunkedCharsStorage(List.of("abc".toCharArray()), 3);
        storage.release();

        MimerChunkedCharsStorage clone = (MimerChunkedCharsStorage) storage.cloneStorage(Mockito.mock(DBRProgressMonitor.class));

        Assertions.assertEquals("", clone.getCachedValue());
        Assertions.assertEquals(0L, clone.getContentLength());
    }
}
