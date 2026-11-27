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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Attribute values and their declaring source, with child-first lookup.
 */
public final class InheritedConfiguration<T> {
    @NotNull
    private final IConfigurationElement element;
    @NotNull
    private final T source;
    @Nullable
    private final InheritedConfiguration<T> parent;

    public InheritedConfiguration(
        @NotNull IConfigurationElement element,
        @NotNull T source,
        @Nullable InheritedConfiguration<T> parent
    ) {
        this.element = element;
        this.source = source;
        this.parent = parent;
    }

    @Nullable
    public String getAttribute(@NotNull String name) {
        return getAttributeConfiguration(name).element.getAttribute(name);
    }

    /**
     * Returns the source declaring the attribute, or the local source if the attribute is absent throughout the chain.
     */
    @NotNull
    public T getAttributeSource(@NotNull String name) {
        return getAttributeConfiguration(name).source;
    }

    @NotNull
    private InheritedConfiguration<T> getAttributeConfiguration(@NotNull String name) {
        // Empty strings, false and zero are explicit overrides, not missing attributes.
        if (element.getAttribute(name) != null || parent == null) {
            return this;
        }
        InheritedConfiguration<T> inherited = parent.getAttributeConfiguration(name);
        return inherited.element.getAttribute(name) == null ? this : inherited;
    }
}
