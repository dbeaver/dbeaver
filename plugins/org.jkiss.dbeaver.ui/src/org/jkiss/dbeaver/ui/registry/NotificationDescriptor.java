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
package org.jkiss.dbeaver.ui.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

public class NotificationDescriptor extends AbstractDescriptor {
    public static final String ELEMENT_ID = "notification";

    private final String id;
    private final String name;
    private final String description;
    private final boolean soundEnabled;
    private final boolean hidden;

    NotificationDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.name = config.getAttribute("name");
        this.description = config.getAttribute("description");
        this.soundEnabled = CommonUtils.getBoolean(config.getAttribute("soundEnabled"));
        this.hidden = CommonUtils.getBoolean(config.getAttribute("hidden"));
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isHidden() {
        return hidden;
    }
}
