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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * {@link MimerChunkedBytesStorage} - see its own javadoc for why it stays chunked until something
 * actually needs the real value.
 *
 * @author Mimer Information Technology
 */
public class MimerChunkedBytesStorageTest extends DBeaverUnitTest {

    @Mock
    private DBRProgressMonitor progressMonitor;

    @Test
    public void concatenatesMultipleChunksInOrderAndPreservesExactContent() throws Exception {
        byte[] chunk1 = {1, 2, 3};
        byte[] chunk2 = {4, 5};
        byte[] chunk3 = {6, 7, 8, 9};
        MimerChunkedBytesStorage storage = new MimerChunkedBytesStorage(List.of(chunk1, chunk2, chunk3), 9, "UTF-8");

        byte[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Assertions.assertArrayEquals(expected, (byte[]) storage.getCachedValue());

        try (InputStream in = storage.getContentStream()) {
            ByteArrayOutputStream read = new ByteArrayOutputStream();
            in.transferTo(read);
            Assertions.assertArrayEquals(expected, read.toByteArray());
        }
    }

    @Test
    public void getCachedValueIsComputedOnceAndCachedAfterward() {
        // If this weren't actually cached, a second call would try to re-concatenate from the
        // chunk list - which materialize() has already discarded by then, and would throw. Calling
        // it twice without that happening is itself proof the caching works, not just that the
        // value stays correct.
        List<byte[]> chunks = List.of(new byte[]{1, 2, 3});
        MimerChunkedBytesStorage storage = new MimerChunkedBytesStorage(chunks, 3, "UTF-8");

        Object first = storage.getCachedValue();
        Object second = storage.getCachedValue();

        Assertions.assertSame(first, second, "The second call must return the same cached array, not recompute it");
    }

    @Test
    public void getContentLengthNeverTouchesTheChunks() {
        @SuppressWarnings("unchecked")
        List<byte[]> chunks = Mockito.mock(List.class);
        MimerChunkedBytesStorage storage = new MimerChunkedBytesStorage(chunks, 12345, "UTF-8");

        Assertions.assertEquals(12345L, storage.getContentLength());

        Mockito.verifyNoInteractions(chunks);
    }

    @Test
    public void cloneStorageSharesTheSameUnflattenedChunksAndBothMaterializeCorrectly() throws Exception {
        List<byte[]> chunks = List.of(new byte[]{10, 20, 30});
        MimerChunkedBytesStorage original = new MimerChunkedBytesStorage(chunks, 3, "UTF-8");

        DBDContentStorage clone = original.cloneStorage(progressMonitor);

        Assertions.assertArrayEquals(new byte[]{10, 20, 30}, (byte[]) original.getCachedValue());
        Assertions.assertInstanceOf(MimerChunkedBytesStorage.class, clone);
        Assertions.assertArrayEquals(new byte[]{10, 20, 30}, (byte[]) ((DBDContentCached) clone).getCachedValue());
    }

    @Test
    public void handlesAnEmptyChunkListAsAZeroLengthValue() {
        MimerChunkedBytesStorage storage = new MimerChunkedBytesStorage(List.of(), 0, "UTF-8");

        Assertions.assertEquals(0L, storage.getContentLength());
        Assertions.assertArrayEquals(new byte[0], (byte[]) storage.getCachedValue());
    }

    @Test
    public void getCachedValueDegradesToEmptyInsteadOfThrowingWhenCalledAfterRelease() {
        // A caller can still be holding this exact storage instance after release() ran on it
        // elsewhere (e.g. a LOB viewer left open past the grid row it came from being
        // refreshed/discarded) - must degrade gracefully, not NPE trying to re-flatten chunks
        // that are already gone.
        MimerChunkedBytesStorage storage = new MimerChunkedBytesStorage(List.of(new byte[]{1, 2, 3}), 3, "UTF-8");
        storage.release();

        Assertions.assertArrayEquals(new byte[0], (byte[]) storage.getCachedValue());
        Assertions.assertEquals(0L, storage.getContentLength(),
            "getContentLength() must agree with the degraded-empty getCachedValue() above, not keep reporting the original size");
    }

    @Test
    public void cloningAnAlreadyReleasedInstanceStillDegradesToEmptyNotZeroFilledGarbage() throws Exception {
        // Regression test: cloneStorage() used to pass an empty List.of() fallback straight into
        // the clone's constructor whenever chunks was already null, with no way for the clone to
        // tell "released" apart from "nothing to flatten yet" - materialize() on such a clone
        // would then concatenate zero real chunks into a `total`-sized array, silently producing
        // `total` zero bytes instead of a genuinely empty value.
        MimerChunkedBytesStorage storage = new MimerChunkedBytesStorage(List.of(new byte[]{1, 2, 3}), 3, "UTF-8");
        storage.release();

        MimerChunkedBytesStorage clone = (MimerChunkedBytesStorage) storage.cloneStorage(Mockito.mock(DBRProgressMonitor.class));

        Assertions.assertArrayEquals(new byte[0], (byte[]) clone.getCachedValue());
        Assertions.assertEquals(0L, clone.getContentLength());
    }
}
