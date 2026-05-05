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

package org.jkiss.dbeaver.ext.gaussdb.model;

public enum DBCompatibilityEnum {

    ORACLE("Oracle", "A", "ORA"), MYSQL("MySQL", "B", "MYSQL"), TEDATA("Teradata", "C", "TD"), POSTGRES("PostgreSQL", "PG", "PG");

    private final String text;
    private final String cValue;
    private final String dValue;

    /**
     * Instantiates a new DBCompatibility enum.
     * 
     * @param text
     *            the text
     * @param cValue
     *            the cValue
     * @param dValue
     *            the dValue
     */
    private DBCompatibilityEnum(String text, String cValue, String dValue) {
        this.text = text;
        this.cValue = cValue;
        this.dValue = dValue;
    }

    /**
     * Gets text.
     * 
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Gets Centralized value.
     * 
     * @return the cValue
     */
    public String getcValue() {
        return cValue;
    }

    /**
     * Gets Distributed value.
     * 
     * @return the dValue
     */
    public String getdValue() {
        return dValue;
    }

    /**
     * Gets DBCompatibilityEnum by text.
     * 
     * @param text
     *            the text
     * @return the DBCompatibilityEnum
     */
    public static DBCompatibilityEnum of(String text) {
        DBCompatibilityEnum[] enums = values();
        for (DBCompatibilityEnum e : enums) {
            if (e.getText().equals(text)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Query DBCompatibilityEnum text by value.
     * 
     * @param cValue
     *            or dValue
     * @return the DBCompatibilityEnum text
     */
    public static String queryTextByValue(String value) {
        DBCompatibilityEnum[] enums = values();
        for (DBCompatibilityEnum e : enums) {
            if (e.cValue.equalsIgnoreCase(value) || e.dValue.equalsIgnoreCase(value)) {
                return e.text;
            }
        }
        return "";
    }
}
