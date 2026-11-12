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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentCLOB;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentLOB;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

/**
 * The {@code CLOB}/{@code NCLOB} sibling of {@link MimerContentBLOB} - same reasoning for why
 * {@link #setStorage} writes the inherited field directly, see its javadoc. Unlike BLOB, a short
 * text preview is worth showing, so {@code getDisplayString} builds one fresh from whatever
 * {@code storage} currently is on every call rather than caching it - that way an edited cell
 * ({@code updateContents}) or a copy-pasted one ({@code cloneValue}) just works, since both swap
 * {@code storage} directly with no separate field to keep in sync.
 * <p>
 * One {@link MimerCLOBValueHandler} instance covers both {@code CLOB} and {@code NCLOB} columns,
 * so {@code typeLabel} tracks which one an individual value actually is. It's mutable, not final
 * - {@link #setTypeLabel} lets {@link MimerCLOBValueHandler#getValueFromObject} correct it later,
 * since the column's real type can resolve after this object's first construction (see that
 * method's javadoc).
 *
 * @author Mimer Information Technology
 */
class MimerContentCLOB extends JDBCContentCLOB {

    // Same bound regardless of which MimerPreviewableStorage kind ends up providing the preview,
    // or whether it's instead truncated from some other already-in-memory cached value.
    static final int PREVIEW_LENGTH = 200;

    @NotNull
    private String typeLabel;

    MimerContentCLOB(DBCExecutionContext executionContext) {
        this(executionContext, "CLOB");
    }

    MimerContentCLOB(DBCExecutionContext executionContext, @NotNull String typeLabel) {
        super(executionContext, null);
        this.typeLabel = typeLabel;
    }

    void setStorage(DBDContentStorage storage) {
        this.storage = storage;
    }

    void setTypeLabel(@NotNull String typeLabel) {
        this.typeLabel = typeLabel;
    }

    @Override
    protected JDBCContentLOB createNewContent() {
        return new MimerContentCLOB(executionContext, typeLabel);
    }

    @Nullable
    @Override
    public String getDisplayString(@NotNull DBDDisplayFormat format) {
        DBDContentStorage currentStorage = storage;
        if (currentStorage == null) {
            // SQL NULL - let the superclass report the NULL marker, not a zero-length CLOB.
            return super.getDisplayString(format);
        }
        try {
            String text;
            long length;
            if (currentStorage instanceof MimerPreviewableStorage previewable) {
                // Synchronizing on currentStorage makes this atomic with its own release(): both
                // MimerChunkedCharsStorage and the other synchronized methods it exposes lock on
                // the same instance, so a release() can't land between the preview and length
                // reads and leave them describing two different states.
                synchronized (currentStorage) {
                    text = previewable.peekPreview(PREVIEW_LENGTH);
                    length = currentStorage.getContentLength();
                }
            } else if (currentStorage instanceof DBDContentCached cached) {
                // Already fully in memory - derive length from the same read instead of a second
                // call.
                String full = CommonUtils.toString(cached.getCachedValue());
                length = full.length();
                text = full.length() > PREVIEW_LENGTH ? full.substring(0, PREVIEW_LENGTH) : full;
            } else {
                // Not expected in practice, but a safe fallback.
                return MimerLobUtil.sizedPlaceholder(typeLabel, currentStorage.getContentLength());
            }
            if (text.length() >= length) {
                return text;
            }
            return text + "... " + MimerLobUtil.sizedPlaceholder(typeLabel, length);
        } catch (IOException e) {
            return super.getDisplayString(format);
        }
    }
}
