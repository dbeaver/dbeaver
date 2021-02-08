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
package org.jkiss.dbeaver.registry.task;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.task.DBTScheduler;
import org.jkiss.dbeaver.model.task.DBTSchedulerDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * TaskTypeDescriptor
 */
public class SchedulerDescriptor extends AbstractContextDescriptor implements DBTSchedulerDescriptor {

    private final String name;
    private final String description;
    private final ObjectType implType;
    private DBTScheduler instance;

    SchedulerDescriptor(IConfigurationElement config) {
        super(config);

        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.implType = new ObjectType(config, "class");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public synchronized DBTScheduler getInstance() throws DBException {
        if (instance == null) {
            instance = implType.createInstance(DBTScheduler.class);
        }
        return instance;
    }

    @Override
    public String toString() {
        return getName();
    }
}
