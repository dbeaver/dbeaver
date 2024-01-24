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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitution;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

public class DriverSubstitutionDescriptor extends AbstractDescriptor implements DBPDriverSubstitutionDescriptor {
    private final String id;
    private final String name;
    private final String providerId;
    private final String driverId;
    private final ObjectType type;

    private volatile DBPDriverSubstitution instance;

    public DriverSubstitutionDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.providerId = config.getAttribute(RegistryConstants.ATTR_PROVIDER);
        this.driverId = config.getAttribute(RegistryConstants.ATTR_DRIVER);
        this.type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getProviderId() {
        return providerId;
    }

    @NotNull
    @Override
    public String getDriverId() {
        return driverId;
    }

    @NotNull
    @Override
    public DBPDriverSubstitution getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    try {
                        instance = type.createInstance(DBPDriverSubstitution.class);
                    } catch (Throwable ex) {
                        throw new IllegalStateException("Unable to initialize driver substitution " + type.getImplName() + "'", ex);
                    }
                }
            }
        }

        return instance;
    }
}
