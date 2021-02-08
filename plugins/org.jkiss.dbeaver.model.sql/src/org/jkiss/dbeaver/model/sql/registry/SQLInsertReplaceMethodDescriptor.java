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
package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

public class SQLInsertReplaceMethodDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sqlInsertMethod"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final AbstractDescriptor.ObjectType implClass;

    public SQLInsertReplaceMethodDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.implClass = new AbstractDescriptor.ObjectType(config.getAttribute("class"));
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
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

    public DBDInsertReplaceMethod createInsertMethod() throws DBException {
        return implClass.createInstance(DBDInsertReplaceMethod.class);
    }
}
