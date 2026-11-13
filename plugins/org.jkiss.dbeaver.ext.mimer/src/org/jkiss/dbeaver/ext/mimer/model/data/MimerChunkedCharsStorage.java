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
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * The {@code char[]} sibling of {@link MimerChunkedBytesStorage} - see its javadoc for why this
 * stays chunked until something actually needs the real value. Mirrors {@code
 * StringContentStorage}'s own choices once materialized (default file encoding for the byte
 * stream, a plain {@link StringReader}) since that's exactly what this replaces, just lazily.
 *
 * @author Mimer Information Technology
 */
final class MimerChunkedCharsStorage implements DBDContentStorage, DBDContentCached, MimerPreviewableStorage {

    // See MimerChunkedBytesStorage for why these are only ever touched inside a synchronized
    // method, and why `released` is a dedicated flag rather than inferred from the other two.
    private List<char[]> chunks;
    private final int total;
    private String materialized;
    private boolean released;

    MimerChunkedCharsStorage(@NotNull List<char[]> chunks, int total) {
        this.chunks = chunks;
        this.total = total;
    }

    @NotNull
    private synchronized String materialize() {
        if (materialized == null) {
            if (released) {
                materialized = "";
            } else {
                materialized = MimerLobUtil.concatenateChars(chunks, total);
                chunks = null;
            }
        }
        return materialized;
    }

    /**
     * Up to {@code maxLength} characters from the start of the value, without flattening the rest
     * - {@link MimerContentCLOB#getDisplayString} uses this instead of {@link #getCachedValue()}
     * so a preview never costs more than {@code maxLength} characters, regardless of the real
     * value's size.
     */
    @NotNull
    @Override
    public synchronized String peekPreview(int maxLength) {
        if (materialized != null) {
            return materialized.length() <= maxLength ? materialized : materialized.substring(0, maxLength);
        }
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        return MimerLobUtil.extractPreview(chunks, maxLength);
    }

    @NotNull
    @Override
    public InputStream getContentStream() throws IOException {
        return new ByteArrayInputStream(materialize().getBytes(GeneralUtils.getDefaultFileEncoding()));
    }

    @NotNull
    @Override
    public Reader getContentReader() {
        return new StringReader(materialize());
    }

    @Override
    public synchronized long getContentLength() {
        // Must agree with materialize()'s degraded-empty answer once released.
        return released ? 0 : total;
    }

    @NotNull
    @Override
    public String getCharset() {
        return GeneralUtils.getDefaultFileEncoding();
    }

    @NotNull
    @Override
    public synchronized DBDContentStorage cloneStorage(@NotNull DBRProgressMonitor monitor) {
        // See MimerChunkedBytesStorage's identical cloneStorage for why.
        MimerChunkedCharsStorage clone = new MimerChunkedCharsStorage(chunks != null ? chunks : List.of(), total);
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
