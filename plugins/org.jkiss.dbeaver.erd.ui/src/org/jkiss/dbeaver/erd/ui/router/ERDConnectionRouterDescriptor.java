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
package org.jkiss.dbeaver.erd.ui.router;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.draw2d.AbstractRouter;
import org.eclipse.draw2d.PolylineConnection;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

public class ERDConnectionRouterDescriptor extends AbstractDescriptor {
    private String id;
    private String name;
    private String description;
    private boolean isDefault = false;
    private boolean isEnableAttributeAssociation;
    private AbstractRouter router;
    private PolylineConnection connection;
    private ObjectType lazyRouter;
    private ObjectType lazyConnection;

    private Log log = Log.getLog(ERDConnectionRouterDescriptor.class.getName());

    protected ERDConnectionRouterDescriptor(IConfigurationElement cf) throws CoreException {
        super(cf);
        this.id = cf.getAttribute(RegistryConstants.ATTR_ID);
        this.name = cf.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = cf.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.isDefault = Boolean.valueOf(cf.getAttribute(RegistryConstants.ATTR_IS_DEFAULT));
        this.isEnableAttributeAssociation = CommonUtils.toBoolean(cf.getAttribute(RegistryConstants.ATTR_SUPPORT_ATTRIBUTES_ASSOCIATION));
        this.lazyRouter = new ObjectType(cf.getAttribute(RegistryConstants.ATTR_ROUTER)); //$NON-NLS-1$
        this.lazyConnection = new ObjectType(cf.getAttribute(RegistryConstants.ATTR_CONNECTION)); //$NON-NLS-1$
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
     * Get contributed router type
     */
    public AbstractRouter getRouter() {
        if (router == null) {
            try {
                router = lazyRouter.createInstance(AbstractRouter.class);
            } catch (DBException e) {
                log.error(e.getMessage());
            }
        }
        return router;
    }

    /**
     * Create contributed connection type
     */
    public PolylineConnection createRouterConnectionInstance() {
        try {
            connection = lazyConnection.createInstance(PolylineConnection.class);
        } catch (DBException e) {
            log.error(e.getMessage());
        }
        return connection;
    }

    /**
     * Is descriptor has default flag
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Get flag of supported attribute association
     *
     * @return - true if support attributes enabled
     */
    public boolean supportedAttributeAssociation() {
        return isEnableAttributeAssociation;
    }

}
