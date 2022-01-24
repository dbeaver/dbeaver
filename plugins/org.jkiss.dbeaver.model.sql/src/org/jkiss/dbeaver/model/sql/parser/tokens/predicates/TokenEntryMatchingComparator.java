/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

abstract class TokenEntryComparatorBase {

    protected int compareByStrings(@NotNull TokenEntry first, @NotNull TokenEntry second) {
        if (first.getString() == null) {
            if (second.getString() == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (second.getString() == null) {
                return 1;
            } else {
                return first.getString().compareToIgnoreCase(second.getString());
            }
        }
    }

    protected int compareByTokenTypes(@NotNull TokenEntry first, @NotNull TokenEntry second) {
        if (first.getTokenType() == null) {
            if (second.getTokenType() == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (second.getTokenType() == null) {
                return 1;
            } else {
                return first.getTokenType().compareTo(second.getTokenType());
            }
        }
    }
}

/**
 * Strong comparator implementing foll comparison of data carried by two token entries.
 * Establishes strong ordering on the continuity of possible token entries.
 */
class ExactTokenEntryComparator extends TokenEntryComparatorBase implements Comparator<TokenEntry> {
    public static final ExactTokenEntryComparator INSTANCE = new ExactTokenEntryComparator();

    private ExactTokenEntryComparator() { }

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

/**
 * Partial comparator implementing relaxed comparison of data carried by two token entries.
 * Also responsible for decision whether two token entries could describe the same token.
 * Establishes partial ordering on the continuity of possible token entries with respect to part of the data.
 * Can be used in case when we cannot judge about the order of some token entries because of the missing but considered meaningful part of data.
 * Note that the part of the data under partial comparison should also be the first to look at during strong comparison (implemented by the {@link ExactTokenEntryComparator}).
 */
class TokenEntryMatchingComparator extends TokenEntryComparatorBase implements TrieLookupComparator<TokenEntry> {
    public static final TokenEntryMatchingComparator INSTANCE = new TokenEntryMatchingComparator();

    private TokenEntryMatchingComparator() {
    }

    @Override
    public boolean isStronglyComparable(@NotNull TokenEntry term) {
        return term.getString() != null && term.getTokenType() != null;
    }

    @Override
    public boolean isPartiallyComparable(@NotNull TokenEntry term) {
        return term.getTokenType() != null;
    }

    @Override
    public int compare(@NotNull TokenEntry first, @NotNull TokenEntry second) {
        return super.compareByTokenTypes(first, second);
    }

    @Override
    public boolean match(@NotNull TokenEntry key, @NotNull TokenEntry term) {
        return key.matches(term);
    }
}
