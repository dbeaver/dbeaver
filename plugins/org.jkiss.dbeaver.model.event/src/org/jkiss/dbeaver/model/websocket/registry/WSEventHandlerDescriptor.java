/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.websocket.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.websocket.WSEventHandler;

/**
 * CB event handler descriptor
 */
public class WSEventHandlerDescriptor extends AbstractDescriptor {
    private final ObjectType implType;

    protected WSEventHandlerDescriptor(IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.implType = new ObjectType(contributorConfig, "class");
    }

    @NotNull
    public WSEventHandler getInstance() {
        try {
            return implType.createInstance(WSEventHandler.class);
        } catch (DBException e) {
            throw new IllegalStateException("Can not instantiate event handler '" + implType.getImplName() + "'", e);
        }
    }
}
