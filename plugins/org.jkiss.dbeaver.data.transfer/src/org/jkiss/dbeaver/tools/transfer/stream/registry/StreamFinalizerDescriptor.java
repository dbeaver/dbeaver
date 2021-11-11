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
package org.jkiss.dbeaver.tools.transfer.stream.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamTransferFinalizer;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamTransferFinalizerConfigurator;

public class StreamFinalizerDescriptor extends AbstractDescriptor {
    private final String id;
    private final ObjectType type;
    private final ObjectType configuratorType;
    private final String label;
    private final String description;

    protected StreamFinalizerDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.type = new ObjectType(config.getAttribute("class"));
        this.configuratorType = config.getAttribute("configurator") == null ? null : new ObjectType(config.getAttribute("configurator"));
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public IStreamTransferFinalizer create() throws DBException {
        type.checkObjectClass(IStreamTransferFinalizer.class);
        try {
            return type
                .getObjectClass(IStreamTransferFinalizer.class)
                .getDeclaredConstructor()
                .newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create finalizer", e);
        }
    }

    @Nullable
    public IStreamTransferFinalizerConfigurator createConfigurator() throws DBException {
        if (configuratorType == null) {
            return null;
        }
        configuratorType.checkObjectClass(IStreamTransferFinalizerConfigurator.class);
        try {
            return configuratorType
                .getObjectClass(IStreamTransferFinalizerConfigurator.class)
                .getDeclaredConstructor()
                .newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create finalizer configurator", e);
        }
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @NotNull
    public String getDescription() {
        return description;
    }
}
