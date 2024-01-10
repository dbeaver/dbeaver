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
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.parser.TokenEntry;

import java.util.Comparator;

/**
 * Strong comparator implementing foll comparison of data carried by two token entries.
 * Establishes strong ordering on the continuity of possible token entries.
 */
class ExactTokenEntryComparator extends TokenEntryComparatorBase implements Comparator<TokenEntry> {
    public static final ExactTokenEntryComparator INSTANCE = new ExactTokenEntryComparator();

    private ExactTokenEntryComparator() {

    }

    @Override
    public int compare(@NotNull TokenEntry first, @NotNull TokenEntry second) {
        // keep in sync with TokenEntryMatchingComparator implementation: look at the partially comparable part of the data at first!
        int result = compareByTokenTypes(first, second);
        if (result != 0) {
            return result;
        }
        return compareByStrings(first, second);
    }
}
