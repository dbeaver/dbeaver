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
package org.jkiss.dbeaver.erd.ui.router;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

/**
 * The class descriptor of representing visual connection between figures
 */
public class ERDConnectionRouterDescriptor extends AbstractDescriptor {
    private final static Log log = Log.getLog(ERDConnectionRouterDescriptor.class.getName());

    private final String id;
    private final String name;
    private final String description;
    private final boolean isEnableAttributeAssociation;
    private final ObjectType lazyRouter;
    private final boolean isDefault;

    protected ERDConnectionRouterDescriptor(IConfigurationElement cf) throws CoreException {
        super(cf);
        this.id = cf.getAttribute(RegistryConstants.ATTR_ID);
        this.name = cf.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = cf.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.isEnableAttributeAssociation = CommonUtils.toBoolean(cf.getAttribute(ERDUIConstants.ATTR_ERD_SUPPORT_ATTRIBUTES_ASSOCIATION));
        this.lazyRouter = new ObjectType(cf.getAttribute(ERDUIConstants.ATTR_ERD_ROUTER));
        this.isDefault = CommonUtils.toBoolean(cf.getAttribute(RegistryConstants.ATTR_DEFAULT));
    }

    /**
     * Id
     */
    public String getId() {
        return id;
    }

    /**
     * Name
     */
    public String getName() {
        return name;
    }

    /**
     * Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Create contributed router type
     */
    public ERDConnectionRouter createRouter() {
        try {
            return lazyRouter.createInstance(ERDConnectionRouter.class);
        } catch (DBException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Get flag of supported attribute association
     *
     * @return - true if support attributes enabled
     */
    public boolean supportedAttributeAssociation() {
        return isEnableAttributeAssociation;
    }

    public boolean isDefaultRouter() {
        return isDefault;
    }

}
