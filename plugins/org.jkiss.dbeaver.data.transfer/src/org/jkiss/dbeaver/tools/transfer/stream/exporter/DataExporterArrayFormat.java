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

public enum DataExporterArrayFormat {
    SQUARE_BRACKETS('[', ']'),
    CURLY_BRACKETS('{', '}'),
    BRACKETS('(', ')');

    private char prefix;
    private char suffix;

    DataExporterArrayFormat(char prefix, char suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public static DataExporterArrayFormat getArrayFormat(String bracketPair) {
        for (DataExporterArrayFormat df : DataExporterArrayFormat.values()) {
            bracketPair = bracketPair.trim();
            if (bracketPair.charAt(0) == df.prefix && bracketPair.charAt(bracketPair.length() - 1) == df.suffix) {
                return df;
            }
        }
        throw new IllegalStateException("No suitable DataExporterArrayFormat found");
    }

    public static DataExporterArrayFormat getArrayFormatOnPrefix(char prefix) {
        for (DataExporterArrayFormat df : DataExporterArrayFormat.values()) {
            if (prefix == df.prefix) {
                return df;
            }
        }
        return CURLY_BRACKETS;
    }

    public char getPrefix() {
        return prefix;
    }

    public char getSuffix() {
        return suffix;
    }
}
