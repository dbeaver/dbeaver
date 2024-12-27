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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.jkiss.dbeaver.model.text.TextUtils;

public class SQLQueryWordEntry {

    public static final boolean USE_FUZZY_COMPARISON = true;

    public final int offset;
    public final String string;
    public final String filterString;

    public SQLQueryWordEntry(int offset, String string) {
        this.offset = offset;
        this.string = string;
        this.filterString = string.toLowerCase();
    }

    public int matches(SQLQueryWordEntry filterKeyOrNull, boolean searchInside) {
        return matches(this.filterString, filterKeyOrNull, searchInside);
    }

    public int matches(String filterKeyStringOrNull, boolean searchInside) {
        return matches(this.filterString, filterKeyStringOrNull, searchInside);
    }

    public static int matches(String string, SQLQueryWordEntry filterKeyOrNull, boolean searchInside) {
        return filterKeyOrNull == null ? Integer.MAX_VALUE : matches(string, filterKeyOrNull.filterString, searchInside);
    }

    public static int matches(String string, String filterKeyStringOrNull, boolean searchInside) {
        if (filterKeyStringOrNull == null) {
            return Integer.MAX_VALUE;
        }

        if (searchInside) {
            return TextUtils.fuzzyScore(string, filterKeyStringOrNull);
        } else {
            return string.startsWith(filterKeyStringOrNull) ? Integer.MAX_VALUE : 0;
        }
    }
}
