/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.registry.sql;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

/**
 * SQLFormatterDescriptor
 */
public class SQLFormatterDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sqlFormatter"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final AbstractDescriptor.ObjectType formatterImplClass;
    private final AbstractDescriptor.ObjectType configurerImplClass;


    public SQLFormatterDescriptor(IConfigurationElement config)
    {
        super(config);
        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.formatterImplClass = new AbstractDescriptor.ObjectType(config.getAttribute("class"));
        if (!CommonUtils.isEmpty(config.getAttribute("configurerClass"))) {
            this.configurerImplClass = new AbstractDescriptor.ObjectType(config.getAttribute("configurerClass"));
        } else {
            this.configurerImplClass = null;
        }
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

    @NotNull
    public SQLFormatter createFormatter()
        throws DBException
    {
        return formatterImplClass.createInstance(SQLFormatter.class);
    }

    @Nullable
    public SQLFormatterConfigurer createConfigurer()
        throws DBException
    {
        if (configurerImplClass == null) {
            return null;
        }
        return configurerImplClass.createInstance(SQLFormatterConfigurer.class);
    }

}
