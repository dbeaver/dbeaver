/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

import java.util.List;

public record ResultSetValuePath(
    @NotNull List<PathItem> pathItems
) {

    public interface PathItemVisitor<T, R> {
        @Nullable
        R visitAttributeItem(@NotNull PathAttributeItem attrItem, @NotNull T arg);

        @Nullable
        R visitIndexItem(@NotNull PathIndexItem indexItem, @NotNull T arg);
    }

    public sealed interface PathItem {
        @Nullable
        <T, R> R apply(@NotNull PathItemVisitor<T, R> visitor, @NotNull T arg);
    }

    public record PathAttributeItem(
        @NotNull DBSAttributeBase attribute
    ) implements PathItem {
        @Nullable
        @Override
        public <T, R> R apply(@NotNull PathItemVisitor<T, R> visitor, @NotNull T arg) {
            return visitor.visitAttributeItem(this, arg);
        }
    }

    public record PathIndexItem(
        int index
    ) implements PathItem {
        @Nullable
        @Override
        public <T, R> R apply(@NotNull PathItemVisitor<T, R> visitor, @NotNull T arg) {
            return visitor.visitIndexItem(this, arg);
        }
    }

}
