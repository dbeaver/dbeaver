/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;

public class ResultSetPasteSettings {
    private boolean insertMultipleRows;
    private boolean insertNulls;
    private String nullValueMark;

    public ResultSetPasteSettings() {
        this.insertMultipleRows = true;
        this.insertNulls = false;
        this.nullValueMark = "";
    }

    public boolean isInsertMultipleRows() {
        return insertMultipleRows;
    }

    public void setInsertMultipleRows(boolean insertMultipleRows) {
        this.insertMultipleRows = insertMultipleRows;
    }

    public boolean isInsertNulls() {
        return insertNulls;
    }

    public void setInsertNulls(boolean insertNulls) {
        this.insertNulls = insertNulls;
    }

    @NotNull
    public String getNullValueMark() {
        return nullValueMark;
    }

    public void setNullValueMark(@NotNull String nullValueMark) {
        this.nullValueMark = nullValueMark;
    }
}
