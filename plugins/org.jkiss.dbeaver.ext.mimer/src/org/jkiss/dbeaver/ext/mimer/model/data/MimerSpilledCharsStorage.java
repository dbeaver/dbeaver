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
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Wraps a real, spilled {@code TemporaryContentStorage} with the cheap preview text {@link
 * MimerCLOBValueHandler#spoolToContent} captured from memory before deciding to spill, so a
 * value too big to stay in memory doesn't lose its preview entirely.
 * <p>
 * {@link #getContentLength} reports the character count {@code spoolToContent} tallied while
 * writing, not {@code delegate.getContentLength()} - for a {@code TemporaryContentStorage} that's
 * {@code Files.size(file)}, the on-disk BYTE count, which for multi-byte content (e.g. {@code
 * NATIONAL CHAR(ACTER) LARGE OBJECT}) can differ substantially from the character count {@link
 * MimerChunkedCharsStorage#getContentLength} reports for the in-memory case - staying consistent
 * with that matters more than with the byte-counting file this happens to be backed by.
 *
 * @author Mimer Information Technology
 */
final class MimerSpilledCharsStorage implements DBDContentStorage, MimerPreviewableStorage {

    @NotNull
    private final DBDContentStorage delegate;
    @NotNull
    private final String preview;
    private final long charLength;

    MimerSpilledCharsStorage(@NotNull DBDContentStorage delegate, @NotNull String preview, long charLength) {
        this.delegate = delegate;
        this.preview = preview;
        this.charLength = charLength;
    }

    @NotNull
    @Override
    public String peekPreview(int maxLength) {
        return preview.length() <= maxLength ? preview : preview.substring(0, maxLength);
    }

    @NotNull
    @Override
    public InputStream getContentStream() throws IOException {
        return delegate.getContentStream();
    }

    @NotNull
    @Override
    public Reader getContentReader() throws IOException {
        return delegate.getContentReader();
    }

    @Override
    public long getContentLength() {
        return charLength;
    }

    @NotNull
    @Override
    public String getCharset() {
        return delegate.getCharset();
    }

    @NotNull
    @Override
    public DBDContentStorage cloneStorage(@NotNull DBRProgressMonitor monitor) throws IOException {
        return new MimerSpilledCharsStorage(delegate.cloneStorage(monitor), preview, charLength);
    }

    @Override
    public void release() {
        delegate.release();
    }
}
