/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * SQLFormatterDescriptor
 */
public class SQLGeneratorDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sqlGenerator"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final int order;
    private final boolean multiObject;
    private final ObjectType generatorImplClass;

    SQLGeneratorDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.generatorImplClass = new ObjectType(config.getAttribute("class"));
        this.order = CommonUtils.toInt(config.getAttribute("order"));
        this.multiObject = CommonUtils.toBoolean(config.getAttribute("multiObject"));
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    public boolean isMultiObject() {
        return multiObject;
    }

    @NotNull
    public <T> SQLGenerator<T> createGenerator(List<T> objects)
        throws DBException
    {
        @SuppressWarnings("unchecked")
        SQLGenerator<T> instance = generatorImplClass.createInstance(SQLGenerator.class);
        instance.initGenerator(objects);
        return instance;
    }

    @Override
    public String toString() {
        return id;
    }
}
