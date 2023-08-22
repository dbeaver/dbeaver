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
package org.jkiss.dbeaver.model.navigator.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNModelExtender;
import org.jkiss.utils.CommonUtils;

/**
 * Model extender descriptor
 */
public class DBNModelExtenderDescriptor extends AbstractDescriptor {

    private String id;
    private ObjectType implType;
    private final boolean isRoot;
    private DBNModelExtender instance;

    DBNModelExtenderDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.implType = new ObjectType(config, "class");
        this.isRoot = CommonUtils.toBoolean(config.getAttribute("root"));
    }

    public String getId() {
        return id;
    }

    public ObjectType getImplType() {
        return implType;
    }

    public DBNModelExtender getInstance() throws DBException {
        if (instance == null) {
            instance = implType.createInstance(DBNModelExtender.class);
        }
        return instance;
    }

    public boolean isRoot() {
        return isRoot;
    }
}
