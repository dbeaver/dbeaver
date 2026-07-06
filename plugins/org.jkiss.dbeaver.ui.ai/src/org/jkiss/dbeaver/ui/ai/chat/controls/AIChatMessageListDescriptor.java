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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

public class AIChatMessageListDescriptor extends AbstractDescriptor {

    @NotNull
    private final String id;
    @Nullable
    private final String replaces;
    @NotNull
    private final ObjectType implType;

    AIChatMessageListDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.replaces = config.getAttribute("replaces");
        this.implType = new ObjectType(config.getAttribute("class"));
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getReplaces() {
        return replaces;
    }

    @NotNull
    public WebViewMessageList createMessageList(@NotNull AIChatControl chat, @NotNull Composite parent) throws DBException {
        return implType.createInstance(WebViewMessageList.class, chat, parent);
    }
}
