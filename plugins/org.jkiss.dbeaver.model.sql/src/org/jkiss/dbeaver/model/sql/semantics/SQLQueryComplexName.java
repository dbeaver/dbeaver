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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Objects;

/**
 * Describes qualified or compound name of the database entity
 */
public class SQLQueryComplexName {

    @NotNull
    public final STMTreeNode syntaxNode;

    @NotNull
    public final List<SQLQuerySymbolEntry> parts;

    @NotNull
    public final List<String> stringParts;

    public final int invalidPartsCount;

    @Nullable
    public final SQLQueryMemberAccessEntry endingPeriodNode;

    public SQLQueryComplexName(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQuerySymbolEntry> parts,
        int invalidPartsCount,
        @Nullable SQLQueryMemberAccessEntry endingPeriodNode
    ) {
        this.syntaxNode = syntaxNode;
        this.parts = parts;
        this.stringParts = parts.stream().map(e -> e == null ? null : e.getName()).toList();
        this.invalidPartsCount = invalidPartsCount;
        this.endingPeriodNode = endingPeriodNode;
    }

    private SQLQueryComplexName(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQuerySymbolEntry> parts,
        @NotNull List<String> stringParts,
        int invalidPartsCount,
        @Nullable SQLQueryMemberAccessEntry endingPeriodNode
    ) {
        this.syntaxNode = syntaxNode;
        this.parts = parts;
        this.stringParts = stringParts;
        this.invalidPartsCount = invalidPartsCount;
        this.endingPeriodNode = endingPeriodNode;
    }

    @Nullable
    public SQLQueryComplexName trimEnd() {
        return this.parts.size() < 2 ? null : new SQLQueryComplexName(
            this.syntaxNode,
            this.parts.subList(0, this.parts.size() - 1),
            this.stringParts.subList(0, this.parts.size() - 1),
            this.invalidPartsCount,
            this.parts.getLast() != null ? this.parts.getLast().getMemberAccess() : this.endingPeriodNode
        );
    }

    @Nullable
    public SQLQueryComplexName trimStart() {
        return this.parts.size() < 2 ? null : new SQLQueryComplexName(
            this.syntaxNode,
            this.parts.subList(1, this.parts.size()),
            this.stringParts.subList(1, this.parts.size()),
            this.invalidPartsCount,
            this.endingPeriodNode
        );
    }

    @NotNull
    public SQLQueryComplexName prepend(SQLQuerySymbolEntry entry) {
        return new SQLQueryComplexName(
            this.syntaxNode,
            STMUtils.combineLists(List.of(entry), this.parts),
            STMUtils.combineLists(List.of(entry.getName()), this.stringParts),
            this.invalidPartsCount,
            this.endingPeriodNode
        );
    }

    public String getNameString() {
        return String.join("", STMUtils.expandTermStrings(this.syntaxNode).reversed());
    }

    public boolean isNotClassified() {
        if (this.invalidPartsCount == 0) {
            return this.parts.getLast().isNotClassified();
        } else {
            return this.parts.stream().filter(Objects::nonNull).allMatch(SQLQuerySymbolEntry::isNotClassified);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SQLQueryComplexName name)) {
            return false;
        }
        return Objects.equals(this.stringParts, name.stringParts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.stringParts);
    }

    @Override
    public String toString() {
        return "SQLQueryComplexName[" + this.getNameString() + "]";
    }
}
