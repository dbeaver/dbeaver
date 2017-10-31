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
package org.jkiss.dbeaver.model.impl.app;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * DBeaver application descriptor.
 */
public class ApplicationDescriptor extends AbstractDescriptor {

    private String id;
    private String name;
    private String description;
    private String parentId;
    private ApplicationDescriptor parent;
    private boolean finalApplication = true;

    ApplicationDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.name = config.getAttribute("name");
        this.description = config.getAttribute("description");
        this.parentId = config.getAttribute("parent");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ApplicationDescriptor getParent() {
        return parent;
    }

    public void setParent(ApplicationDescriptor parent) {
        this.parent = parent;
        this.parent.finalApplication = false;
    }

    boolean isFinalApplication() {
        return finalApplication;
    }

    String getParentId() {
        return parentId;
    }

}
