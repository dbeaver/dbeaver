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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.expressions.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.preference.IPreferencePage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * DataSourcePageDescriptor
 */
public class DataSourcePageDescriptor extends AbstractDescriptor {
    private static final Log log = Log.getLog(DataSourcePageDescriptor.class);

    private final String id;
    private final String parentId;
    private final String title;
    private final String description;
    private final ObjectType pageClass;
    private Expression enablementExpression;

    public DataSourcePageDescriptor(IConfigurationElement config) {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.parentId = config.getAttribute(RegistryConstants.ATTR_PARENT);
        this.title = config.getAttribute("title");
        this.description = config.getAttribute("description");
        this.pageClass = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));

        {
            IConfigurationElement[] elements = config.getChildren("enabledWhen");
            if (elements.length > 0) {
                try {
                    IConfigurationElement[] enablement = elements[0].getChildren();
                    if (enablement.length > 0) {
                        enablementExpression = ExpressionConverter.getDefault().perform(enablement[0]);
                    }
                } catch (Exception e) {
                    log.debug(e);
                }
            }
        }

    }

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public String getTitle() {
        return title == null ? id : title;
    }

    public String getDescription() {
        return description;
    }

    public ObjectType getPageClass() {
        return pageClass;
    }

    public IPreferencePage createPage() {
        try {
            return pageClass.createInstance(IPreferencePage.class);
        } catch (Throwable ex) {
            throw new IllegalStateException("Can't create preferences page '" + id + "'", ex);
        }
    }

    public boolean appliesTo(DBPDataSourceContainer dataSource) {
        if (enablementExpression != null) {
            try {
                IEvaluationContext context = new EvaluationContext(null, dataSource);
                EvaluationResult result = enablementExpression.evaluate(context);
                if (result != EvaluationResult.TRUE) {
                    return false;
                }
            } catch (CoreException e) {
                log.debug(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return id;
    }

}
