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
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentCLOB;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentLOB;
import org.jkiss.utils.ByteNumberFormat;

/**
 * The {@code CLOB}/{@code NCLOB} sibling of {@link MimerContentBLOB} - same reasoning, see its
 * javadoc, including the {@code getDisplayString} override for a size-aware placeholder once the
 * content has been spilled to a temp file.
 *
 * @author Mimer Information Technology
 */
class MimerContentCLOB extends JDBCContentCLOB {

    MimerContentCLOB(DBCExecutionContext executionContext) {
        super(executionContext, null);
    }

    void setStorage(DBDContentStorage storage) {
        this.storage = storage;
    }

    @Override
    protected JDBCContentLOB createNewContent() {
        return new MimerContentCLOB(executionContext);
    }

    @Nullable
    @Override
    public String getDisplayString(@NotNull DBDDisplayFormat format) {
        if (storage == null || storage instanceof DBDContentCached) {
            // storage == null means this value is SQL NULL (clob is never set, see the
            // constructor) - let the superclass report the NULL marker instead of treating it
            // as a zero-length CLOB below.
            return super.getDisplayString(format);
        }
        try {
            return "[CLOB, " + new ByteNumberFormat(ByteNumberFormat.BinaryPrefix.ISO).format(getContentLength()) + "]";
        } catch (DBCException e) {
            return super.getDisplayString(format);
        }
    }
}
