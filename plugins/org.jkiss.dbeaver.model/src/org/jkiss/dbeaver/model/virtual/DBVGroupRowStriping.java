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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persists result set group row striping.
 */
public class DBVGroupRowStriping {

    static final String JSON_KEY = "group-row-striping";

    private boolean enabled;
    @NotNull
    private final List<String> columnNames = new ArrayList<>();
    @Nullable
    private String backgroundColor1;
    @Nullable
    private String backgroundColor2;
    private boolean sortByGroupColumns;

    public DBVGroupRowStriping() {
    }

    public DBVGroupRowStriping(@NotNull DBVGroupRowStriping source) {
        this.enabled = source.enabled;
        this.columnNames.addAll(source.columnNames);
        this.backgroundColor1 = source.backgroundColor1;
        this.backgroundColor2 = source.backgroundColor2;
        this.sortByGroupColumns = source.sortByGroupColumns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NotNull
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    public void setColumnNames(@NotNull List<String> columnNames) {
        this.columnNames.clear();
        this.columnNames.addAll(columnNames);
    }

    @Nullable
    public String getBackgroundColor1() {
        return backgroundColor1;
    }

    public void setBackgroundColor1(@Nullable String backgroundColor1) {
        this.backgroundColor1 = backgroundColor1;
    }

    @Nullable
    public String getBackgroundColor2() {
        return backgroundColor2;
    }

    public void setBackgroundColor2(@Nullable String backgroundColor2) {
        this.backgroundColor2 = backgroundColor2;
    }

    public boolean isSortByGroupColumns() {
        return sortByGroupColumns;
    }

    public void setSortByGroupColumns(boolean sortByGroupColumns) {
        this.sortByGroupColumns = sortByGroupColumns;
    }

    public boolean hasValuableData() {
        return enabled
            && !CommonUtils.isEmpty(columnNames)
            && !CommonUtils.isEmpty(backgroundColor1)
            && !CommonUtils.isEmpty(backgroundColor2);
    }
}
