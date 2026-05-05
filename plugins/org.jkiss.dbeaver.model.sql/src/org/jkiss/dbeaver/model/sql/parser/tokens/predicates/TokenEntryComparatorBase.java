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
