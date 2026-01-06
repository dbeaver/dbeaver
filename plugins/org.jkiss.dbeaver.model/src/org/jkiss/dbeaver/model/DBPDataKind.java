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

package org.jkiss.dbeaver.model;

/**
 * Data kind
 */
public enum DBPDataKind
{
    BOOLEAN(false),
    NUMERIC(false),
    STRING(false),
    DATETIME(false),
    BINARY(false),
    CONTENT(false),
    STRUCT(true),
    DOCUMENT(true),
    ARRAY(true),
    OBJECT(true),
    REFERENCE(true),
    ROWID(false),
    ANY(true),
    UNKNOWN(false);

    private final boolean complex;

    DBPDataKind(boolean complex) {
        this.complex = complex;
    }

    public boolean isComplex() {
        return complex;
    }

    public boolean supportsRangeSearch() {
        return !complex && this != ROWID;
    }

    /**
     * The commonality describes how common is a given data kind, mostly
     * its ability for one type to consume another <b>without losing any data</b>.
     * <p>
     * For example, when comparing {@link #NUMERIC} and {@link #STRING},
     * the latter one would be more common because it can represent
     * {@link #NUMERIC} (but no other way around).
     * <p>
     * Using this concept it is possible to peek a best-suitable
     * type for document-based attributes where the containing data
     * could wary from one document to another.
     *
     * @return commonality of given data kind
     */
    public int getCommonality() {
        switch (this) {
            case BINARY:
            case CONTENT:
                return 3;
            case STRING:
                return 2;
            case BOOLEAN:
            case NUMERIC:
            case DATETIME:
            case STRUCT:
            case ARRAY:
            case OBJECT:
            case REFERENCE:
            case ROWID:
                return 1;
            default:
                return 0;
        }
    }

    public static boolean canConsume(DBPDataKind sourceKind, DBPDataKind targetKind) {
        if (targetKind == CONTENT || targetKind == BINARY) {
            return sourceKind == STRING || sourceKind == BINARY || sourceKind == CONTENT;
        } else if (targetKind == STRING) {
            return sourceKind == STRING;// || sourceKind == NUMERIC || sourceKind == BOOLEAN || sourceKind == DATETIME;
        } else {
            return sourceKind == targetKind;
        }
    }
}
