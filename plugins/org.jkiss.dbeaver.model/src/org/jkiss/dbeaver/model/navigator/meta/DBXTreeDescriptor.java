/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.navigator.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * DBXTreeDescriptor
 */
public class DBXTreeDescriptor extends DBXTreeItem {

    private final boolean supportsEntityMerge;

    public DBXTreeDescriptor(AbstractDescriptor source, DBXTreeNode parent, IConfigurationElement config, String path, String propertyName, boolean optional, boolean navigable, boolean inline, boolean virtual, boolean standalone, String visibleIf, String recursiveLink) {
        super(source, parent, config, path, propertyName, optional, navigable, inline, virtual, standalone, visibleIf, recursiveLink);

        this.supportsEntityMerge = config != null && CommonUtils.toBoolean(config.getAttribute("supportsEntityMerge"));
    }

    public DBXTreeDescriptor(@NotNull AbstractDescriptor source, @NotNull DBXTreeDescriptor item) {
        super(source, null, item);

        this.supportsEntityMerge = item.supportsEntityMerge;
    }

    public boolean supportsEntityMerge() {
        return supportsEntityMerge;
    }
}
