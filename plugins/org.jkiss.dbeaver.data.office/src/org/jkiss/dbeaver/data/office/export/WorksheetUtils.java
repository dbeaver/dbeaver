/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.data.office.export;

import org.apache.poi.ss.usermodel.Workbook;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.util.Set;

/**
 * Xlsx sheet name validator.
 * Follow the rules https://support.microsoft.com/en-us/office/rename-a-worksheet-3f1f7148-ee83-404d-8ef0-9ff99fbad1f9
 */
public class WorksheetUtils {

    private static final Log log = Log.getLog(WorksheetUtils.class);

    public static final String DEFAULT_SHEET_NAME = "Sheet";

    private static final Set<String> RESERVED_WORDS = Set.of("History");
    private static final int MAX_NAME_LENGTH = 31;
    private static final int MAX_NAME_GENERATION_ATTEMPTS = 1000;

    private WorksheetUtils() {
    }

    @NotNull
    public static String makeUniqueSheetName(@NotNull Workbook workbook, @Nullable String sheetName) {
        String name = toValidExcelSheetName(sheetName);
        for (int i = 0; i < MAX_NAME_GENERATION_ATTEMPTS; i++) {
            String result;
            if (i == 0) {
                result = name;
            } else {
                String suffix = String.valueOf(i);
                result = name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH - suffix.length())) + suffix;
            }
            if (workbook.getSheet(result) == null) {
                return result;
            }
        }
        log.error("Unable to generate unique sheet name; using the original name instead");
        return name;
    }

    @NotNull
    private static String toValidExcelSheetName(@Nullable String originSheetName) {
        if (originSheetName == null) {
            return DEFAULT_SHEET_NAME;
        }
        String result = originSheetName;
        result = result.replaceAll("[\\\\/*\\[\\]:?]", "_"); // remove special characters
        result = result.strip(); // remove redundant spaces
        result = result.replaceAll("^'+|'+$", ""); // remove enclosing single quotes
        result = result.substring(0, Math.min(result.length(), MAX_NAME_LENGTH)); // trim to max length
        if (RESERVED_WORDS.contains(result) || result.isBlank()) {
            result = DEFAULT_SHEET_NAME;
        }
        return result;
    }
}
