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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.util.ArrayList;
import java.util.List;

public record DBDValuePath(@NotNull List<Element> elements) {
    public sealed interface Element {
        @Nullable
        Object extract(@NotNull Object object) throws DBException;

        @NotNull
        String toString();
    }

    record TypeAttribute(@NotNull DBDAttributeBinding binding) implements Element {
        @Override
        public Object extract(@NotNull Object object) throws DBCException {
            return binding.extractNestedValue(object, 0);
        }

        @NotNull
        @Override
        public String toString() {
            return DBUtils.getObjectFullName(binding, DBPEvaluationContext.UI);
        }
    }

    record ArrayIndex(@NotNull DBDAttributeBinding binding, int index) implements Element {
        @Override
        public Object extract(@NotNull Object object) throws DBException {
            return binding.extractNestedValue(object, index);
        }

        @NotNull
        @Override
        public String toString() {
            return "[" + index + "]";
        }
    }

    public DBDValuePath(@NotNull List<Element> elements) {
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("Path elements must not be empty");
        }
        this.elements = List.copyOf(elements);
    }

    @NotNull
    public static DBDValuePath of(@NotNull Element... elements) {
        return new DBDValuePath(List.of(elements));
    }

    @NotNull
    public DBDValuePath add(@NotNull Element element) {
        List<Element> copy = new ArrayList<>(elements.size() + 1);
        copy.addAll(elements);
        copy.add(element);
        return new DBDValuePath(copy);
    }

    @Nullable
    public Object extract(@Nullable Object object) throws DBException {
        Object result = object;
        for (Element element : elements) {
            if (result == null) {
                break;
            }
            result = element.extract(result);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Element element : elements) {
            if (element instanceof TypeAttribute && !sb.isEmpty()) {
                sb.append('.');
            }
            sb.append(element.toString());
        }
        return sb.toString();
    }
}
