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
package org.jkiss.dbeaver.model.navigator.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBRuntimeException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNModelExtender;
import org.jkiss.utils.CommonUtils;

import java.util.Objects;

/**
 * Model extender descriptor
 */
public class DBNModelExtenderDescriptor extends AbstractDescriptor {

    private final IConfigurationElement config;
    private final ObjectType implType;
    private DBNModelExtender instance;

    DBNModelExtenderDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        this.config = config;
        this.implType = new ObjectType(config, "class");
    }

    @NotNull
    public String getId() {
        return Objects.requireNonNull(config.getAttribute("id"), "id not specified");
    }

    @NotNull
    public String getNodeType() {
        return Objects.requireNonNull(config.getAttribute("nodeType"), "nodeType not specified");
    }

    @NotNull
    public String getNodeDisplayName() {
        return Objects.requireNonNull(config.getAttribute("nodeDisplayName"), "displayName not specified");
    }

    @Nullable
    public String getNodeDescription() {
        return config.getAttribute("nodeDescription");
    }

    @Nullable
    public DBPImage getNodeIcon() {
        return iconToImage(config.getAttribute("nodeIcon"));
    }

    public boolean isRoot() {
        return CommonUtils.toBoolean(config.getAttribute("root"));
    }

    public ObjectType getImplType() {
        return implType;
    }

    @NotNull
    public DBNModelExtender getInstance() {
        if (instance == null) {
            try {
                instance = implType.createInstance(DBNModelExtender.class);
            } catch (DBException e) {
                throw new DBRuntimeException(e);
            }
        }
        return instance;
    }

    @Override
    public String toString() {
        return getId();
    }
}
