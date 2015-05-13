/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * ResourcePropertyTester
 */
public class ResourcePropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.resource";
    public static final String PROP_CAN_OPEN = "canOpen";
    public static final String PROP_CAN_CREATE_FOLDER = "canCreateFolder";
    public static final String PROP_CAN_CREATE_LINK = "canCreateLink";
    public static final String PROP_CAN_SET_ACTIVE = "canSetActive";
    public static final String PROP_CAN_DELETE = "canDelete";
    public static final String PROP_TYPE = "type";

    public ResourcePropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IResource)) {
            return false;
        }
        IResource resource = (IResource)receiver;
        final ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
        DBPResourceHandler handler = projectRegistry.getResourceHandler(resource);
        if (handler == null) {
            return false;
        }

        if (property.equals(PROP_CAN_OPEN)) {
            return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_OPEN) != 0;
        } else if (property.equals(PROP_CAN_DELETE)) {
            return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_DELETE) != 0;
        } else if (property.equals(PROP_CAN_CREATE_FOLDER)) {
            return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_CREATE_FOLDER) != 0;
        } else if (property.equals(PROP_CAN_CREATE_LINK)) {
            return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_CREATE_FOLDER) != 0 && !resource.isLinked(IResource.CHECK_ANCESTORS);
        } else if (property.equals(PROP_CAN_SET_ACTIVE)) {
            return resource instanceof IProject && resource != projectRegistry.getActiveProject();
        } else if (property.equals(PROP_TYPE)) {
            final DBPResourceHandler resourceHandler = DBeaverCore.getInstance().getProjectRegistry().getResourceHandler(resource);
            return resourceHandler != null && expectedValue.equals(resourceHandler.getTypeName(resource));
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}
