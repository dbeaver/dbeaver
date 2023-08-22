/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
        return term.getString() != null && term.getTokenType() != null && !term.isInverted();
    }

    @Override
    public boolean isPartiallyComparable(@NotNull TokenEntry term) {
        return term.getTokenType() != null && !term.isInverted();
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
