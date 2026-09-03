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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.preference.IPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * ConnectionPageDescriptor
 */
public class ConnectionPageDescriptor extends AbstractDescriptor {

    private final String id;
    private final String parentId;
    private final String afterPageId;
    private final String title;
    private final String description;
    private final ObjectType pageClass;
    private final Expression enablementExpression;

    public ConnectionPageDescriptor(@NotNull IConfigurationElement config) {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.parentId = config.getAttribute(RegistryConstants.ATTR_PARENT);
        this.afterPageId = config.getAttribute("after");
        this.title = config.getAttribute("title");
        this.description = config.getAttribute("description");
        this.pageClass = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.enablementExpression = getEnablementExpression(config);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getParentId() {
        return parentId;
    }

    @Nullable
    public String getAfterPageId() {
        return afterPageId;
    }

    @NotNull
    public String getTitle() {
        return title == null ? id : title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NotNull
    public IPreferencePage createPage() {
        try {
            return pageClass.createInstance(IPreferencePage.class);
        } catch (Throwable ex) {
            throw new IllegalStateException("Can't create preferences page '" + id + "'", ex);
        }
    }

    public boolean appliesTo(@NotNull DBPDataSourceContainer dataSource) {
        return isExpressionTrue(enablementExpression, dataSource);
    }

    @Override
    public String toString() {
        return id;
    }

}
