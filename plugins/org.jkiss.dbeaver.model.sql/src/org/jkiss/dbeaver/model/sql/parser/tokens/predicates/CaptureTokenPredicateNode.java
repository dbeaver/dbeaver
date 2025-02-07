/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;

/**
 * Represents node of token predicate capable of capturing matching text parts
 */
public class CaptureTokenPredicateNode extends SQLTokenEntry {

    @NotNull
    public final String key;

    public CaptureTokenPredicateNode(@Nullable String string, @Nullable SQLTokenType type, @NotNull String key) {
        super(string, type, false);
        this.key = key;
    }

    @NotNull
    @Override
    public StringBuilder format(@NotNull StringBuilder sb) {
        sb.append("$");
        return super.format(sb);
    }

    @NotNull
    @Override
    protected <T, R> R applyImpl(@NotNull TokenPredicateNodeVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitCapture(this, arg);
    }
}
