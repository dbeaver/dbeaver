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
package org.jkiss.dbeaver.ui.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

public class ConfirmationDescriptor extends AbstractDescriptor {
    public static final String ELEMENT_ID = "confirmation";

    private final String id;
    private final String title;
    private final String description;
    private final String message;
    private final String toggleMessage;
    private final String group;

    ConfirmationDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.title = config.getAttribute("title");
        this.description = config.getAttribute("description");
        this.message = config.getAttribute("message");
        this.toggleMessage = config.getAttribute("toggleMessage");
        this.group = config.getAttribute("group");
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getToggleMessage() {
        return toggleMessage;
    }

    @NotNull
    public String getGroup() {
        return group;
    }
}
