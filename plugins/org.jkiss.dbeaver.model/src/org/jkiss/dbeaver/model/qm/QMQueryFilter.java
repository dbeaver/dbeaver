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
package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.time.Instant;
import java.util.Objects;

public final class QMQueryFilter {
    @NotNull
    private final String query;
    @NotNull
    private final String text;
    @Nullable
    private String title;
    @Nullable
    private final Instant lastUsed;
    private final int useCount;

    public QMQueryFilter(
        @NotNull String query,
        @NotNull String text,
        @Nullable String title,
        @Nullable Instant lastUsed,
        int useCount
    ) {
        this.query = query;
        this.text = text;
        this.title = title;
        this.lastUsed = lastUsed;
        this.useCount = useCount;
    }

    @NotNull
    public String query() {
        return query;
    }

    @NotNull
    public String text() {
        return text;
    }

    @Nullable
    public String title() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public Instant lastUsed() {
        return lastUsed;
    }

    public int useCount() {
        return useCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (QMQueryFilter) obj;
        return Objects.equals(this.query, that.query) &&
            Objects.equals(this.text, that.text) &&
            Objects.equals(this.title, that.title) &&
            Objects.equals(this.lastUsed, that.lastUsed) &&
            this.useCount == that.useCount;
    }

}
