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
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * Holds a {@link MimerBLOBValueHandler}-read BLOB as independent chunks, exactly as read - never
 * flattened into one {@code byte[]} until something actually needs the real value ({@link
 * #getCachedValue()}, {@link #getContentStream()}, or export). {@link
 * MimerContentBLOB#getDisplayString} never touches this class at all, so scrolling past
 * thousands of unviewed rows costs nothing beyond the initial read.
 *
 * @author Mimer Information Technology
 */
final class MimerChunkedBytesStorage implements DBDContentStorage, DBDContentCached {

    // Every read and write of these three fields is confined to a synchronized(this) method
    // below, so the lazy-init/release state transitions are always seen as a consistent whole.
    private List<byte[]> chunks;
    private final int total;
    private final String encoding;
    private byte[] materialized;
    // Distinguishes "released" from "not yet materialized" - both would otherwise look like
    // chunks == null && materialized == null.
    private boolean released;

    MimerChunkedBytesStorage(@NotNull List<byte[]> chunks, int total, @NotNull String encoding) {
        this.chunks = chunks;
        this.total = total;
        this.encoding = encoding;
    }

    @NotNull
    private synchronized byte[] materialize() {
        if (materialized == null) {
            if (released) {
                // A caller can still hold this instance after it was released elsewhere (e.g. a
                // LOB viewer left open past its row being discarded) - degrade to empty instead
                // of failing.
                materialized = new byte[0];
            } else {
                materialized = MimerLobUtil.concatenateBytes(chunks, total);
                chunks = null;
            }
        }
        return materialized;
    }

    @NotNull
    @Override
    public InputStream getContentStream() {
        return new ByteArrayInputStream(materialize());
    }

    @NotNull
    @Override
    public Reader getContentReader() throws IOException {
        return new InputStreamReader(getContentStream(), encoding);
    }

    @Override
    public synchronized long getContentLength() {
        // Must agree with materialize()'s degraded-empty answer once released.
        return released ? 0 : total;
    }

    @NotNull
    @Override
    public String getCharset() {
        return encoding;
    }

    @NotNull
    @Override
    public synchronized DBDContentStorage cloneStorage(@NotNull DBRProgressMonitor monitor) {
        // chunks can be null here if already materialized or released; the List.of() fallback
        // then satisfies the constructor's @NotNull contract but is never actually read, since
        // materialized/released are copied right after and take priority in materialize().
        MimerChunkedBytesStorage clone = new MimerChunkedBytesStorage(
            chunks != null ? chunks : List.of(), total, encoding);
        clone.materialized = this.materialized;
        clone.released = this.released;
        return clone;
    }

    @Override
    public synchronized void release() {
        chunks = null;
        materialized = null;
        released = true;
    }

    @NotNull
    @Override
    public Object getCachedValue() {
        return materialize();
    }
}
