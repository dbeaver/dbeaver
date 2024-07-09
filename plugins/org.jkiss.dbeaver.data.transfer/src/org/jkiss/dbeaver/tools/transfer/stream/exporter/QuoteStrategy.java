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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

public enum QuoteStrategy {

    DISABLED("disabled"),
    ALL("all"),
    STRINGS("strings"),
    ALL_BUT_NUMBERS("all but numbers"),
    ALL_BUT_NULLS("all but nulls");

    private final String value;

    QuoteStrategy(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static QuoteStrategy fromValue(String v) {
        for (QuoteStrategy s: QuoteStrategy.values()) {
            if (s.value.equals(v)) {
                return s;
            }
        }
        // backward compatibility
        if ("true".equalsIgnoreCase(v)) {
            return ALL;
        } else if ("false".equalsIgnoreCase(v)) {
            return DISABLED;
        }
        // default value if not provided any
        return DISABLED;
    }
}
