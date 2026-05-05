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
package org.jkiss.dbeaver.ui.editors.sql.addins;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * Configuration descriptor of the SQL Editor add-in
 */
public class SQLEditorAddInDescriptor extends AbstractDescriptor {
    private final String id;
    private final int priority;
    private final ObjectType implClass;

    SQLEditorAddInDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.priority = CommonUtils.toInt(config.getAttribute("priority"));
        this.implClass = new ObjectType(config.getAttribute("class"));
    }

    /**
     * Symbolic identifier of the add-in
     */
    public String getId() {
        return id;
    }
    
    /**
     * Initialization priority of the add-in. Affects the order in which add-ins would be instantiated and initialized for each editor. 
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Create a new instance of an editor add-in
     */
    public SQLEditorAddIn createInstance() throws DBException {
        return implClass.createInstance(SQLEditorAddIn.class);
    }

    @Override
    public String toString() {
        return "SQLEditorAddInDescriptor[id: " + id + ", class: " + implClass.getImplName() + ", priority: " + priority + "]"; //$NON-NLS-1$
    }
}
