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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;

import java.util.List;

/**
 * Pure text helpers for result-set cell selection drag-and-drop payloads.
 * Multi-column display joins match LightGrid row-header {@code TextTransfer}
 * ({@code ", "} within a row, {@code '\n'} between rows).
 */
public final class ResultSetCellDragUtils {

    private ResultSetCellDragUtils() {
    }

    /**
     * Joins SQL literal fragments with {@code ", "} (single-column cell drag / IN-list style).
     */
    @NotNull
    public static String joinSqlLiterals(@NotNull List<String> sqlLiterals) {
        if (sqlLiterals.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String literal : sqlLiterals) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(literal);
        }
        return text.toString();
    }

    /**
     * Joins already-formatted display strings in row-major order using the same
     * separators as LightGrid row-header drag text.
     *
     * @param rows each inner list is one logical row of display values (e.g. {@code [NULL]})
     */
    @NotNull
    public static String joinDisplayRows(@NotNull List<List<String>> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            if (r > 0) {
                text.append('\n');
            }
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                if (c > 0) {
                    text.append(", ");
                }
                text.append(row.get(c));
            }
        }
        return text.toString();
    }
}
