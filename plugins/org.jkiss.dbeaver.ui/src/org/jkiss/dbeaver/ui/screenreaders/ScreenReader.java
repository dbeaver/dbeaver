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
package org.jkiss.dbeaver.ui.screenreaders;

public enum ScreenReader {

    DEFAULT("Default"), // $NON-NLS-0$
    NVDA("NVDA"), // $NON-NLS-0$
    JAWS("JAWS"), // $NON-NLS-0$
    NARRATOR("Narrator"), // $NON-NLS-0$
    OTHER("Other"); // $NON-NLS-0$

    private String screenReaderName;

    private ScreenReader(String name) {
        this.screenReaderName = name;
    }

    /**
     * Gets the reader abbreviation name
     *
     * @return - name
     */
    public String getScreenReaderName() {
        return this.screenReaderName;
    }

    private static ScreenReader getScreenReaderByName(String scereenReaderName) {
        if (scereenReaderName != null) {
            for (ScreenReader reader : ScreenReader.values()) {
                if (scereenReaderName.equalsIgnoreCase(reader.getScreenReaderName())) {
                    return reader;
                }
            }
        }
        return ScreenReader.DEFAULT;
    }

    /**
     * The general method to retrieve screen reader by name and value or return
     * default
     *
     * @param scereenReader
     * @return
     */
    public static ScreenReader getScreenReader(String scereenReader) {
        ScreenReader result = null;
        if (scereenReader != null) {
            for (ScreenReader reader : ScreenReader.values()) {
                if (scereenReader.equals(reader.name())) {
                    result = reader;
                    break;
                }
            }
            if (result == null) {
                result = getScreenReaderByName(scereenReader);
            }
        }
        if (result == null) {
            result = ScreenReader.DEFAULT;
        }
        return result;
    }

}
