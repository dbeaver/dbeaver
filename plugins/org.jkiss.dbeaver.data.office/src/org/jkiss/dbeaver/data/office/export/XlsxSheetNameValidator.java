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

import java.util.Set;

public class XlsxSheetNameValidator {

    public static final String DEFAULT_SPREAD_SHEET = "Sheet1";
    private static final Set<String> RESERVED_WORDS = Set.of("History");

    public static String toValidExcelSheetName(String originSheetName) {
        String result = originSheetName.length() > 31 ? originSheetName.substring(0, 31) : originSheetName;
        result = result.replaceAll("[\\\\/*\\[\\]:?]", "_");
        result = result.strip();
        if (result.startsWith("'") || result.endsWith("'")) {
            result = result.replaceAll("^'+|'+$", "");
        }
        if (RESERVED_WORDS.contains(result) || result.isBlank()) {
            result = DEFAULT_SPREAD_SHEET;
        }
        return result;
    }
}
