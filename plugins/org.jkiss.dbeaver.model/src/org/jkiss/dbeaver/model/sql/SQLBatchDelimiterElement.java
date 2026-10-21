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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * A non-executable delimiter separating two script batches.
 */
public final class SQLBatchDelimiterElement implements SQLScriptElement {
    @Nullable
    private final DBPDataSource dataSource;
    @NotNull
    private final String text;
    private final int offset;
    private final int length;
    @Nullable
    private Object data;

    public SQLBatchDelimiterElement(
        @Nullable DBPDataSource dataSource,
        @NotNull String text,
        int offset,
        int length
    ) {
        this.dataSource = dataSource;
        this.text = text;
        this.offset = offset;
        this.length = length;
    }

    @NotNull
    @Override
    public String getOriginalText() {
        return this.text;
    }

    @NotNull
    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public int getOffset() {
        return this.offset;
    }

    @Override
    public int getLength() {
        return this.length;
    }

    @Nullable
    @Override
    public Object getData() {
        return this.data;
    }

    @Override
    public void setData(@Nullable Object data) {
        this.data = data;
    }

    @Override
    public void reset() {
        this.data = null;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return this.dataSource;
    }
}
