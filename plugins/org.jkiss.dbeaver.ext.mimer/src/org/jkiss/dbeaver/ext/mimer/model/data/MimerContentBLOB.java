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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBLOB;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentLOB;

/**
 * A {@link JDBCContentBLOB} with no {@code java.sql.Blob} of its own - {@link
 * MimerBLOBValueHandler} populates it via {@link #setStorage} instead, writing the inherited
 * {@code protected storage} field directly rather than going through the public {@code
 * JDBCContentLOB#updateContents}, which would also flip {@code modified} to {@code true} - wrong
 * for freshly-fetched data. Same pattern {@code OracleContentXML} already uses for the identical
 * reason.
 * <p>
 * Also overrides {@code getDisplayString} to show the real content length even when the base
 * class's own check (storage must be {@link DBDContentCached}, i.e. still in memory) would
 * otherwise fall back to a bare {@code "[BLOB]"} - true for every value {@link
 * MimerBLOBValueHandler} had to spill to a temp file. {@code storage.getContentLength()} is cheap
 * even then (a file size, not a read), so there's no reason to show less than that.
 *
 * @author Mimer Information Technology
 */
class MimerContentBLOB extends JDBCContentBLOB {

    MimerContentBLOB(DBCExecutionContext executionContext) {
        super(executionContext, null);
    }

    void setStorage(DBDContentStorage storage) {
        this.storage = storage;
    }

    @Override
    protected JDBCContentLOB createNewContent() {
        return new MimerContentBLOB(executionContext);
    }

    @Nullable
    @Override
    public String getDisplayString(@NotNull DBDDisplayFormat format) {
        if (storage == null || storage instanceof DBDContentCached) {
            // storage == null means this value is SQL NULL (blob is never set, see the
            // constructor) - let the superclass report the NULL marker instead of treating it
            // as a zero-length BLOB below.
            return super.getDisplayString(format);
        }
        try {
            return MimerLobUtil.sizedPlaceholder("BLOB", getContentLength());
        } catch (DBCException e) {
            return super.getDisplayString(format);
        }
    }
}
