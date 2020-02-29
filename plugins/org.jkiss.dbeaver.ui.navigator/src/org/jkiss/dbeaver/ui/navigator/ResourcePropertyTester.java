/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceCreator;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * ResourcePropertyTester
 */
public class ResourcePropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.resource";
    public static final String PROP_CAN_OPEN = "canOpen";
    public static final String PROP_CAN_CREATE_FILE = "canCreateFile";
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
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        DBPResourceHandler handler = workspace.getResourceHandler(resource);
        if (handler == null) {
            return false;
        }

        switch (property) {
            case PROP_CAN_OPEN:
                return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_OPEN) != 0;
            case PROP_CAN_DELETE:
                return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_DELETE) != 0;
            case PROP_CAN_CREATE_FILE:
                return handler instanceof DBPResourceCreator && (handler.getFeatures(resource) & DBPResourceCreator.FEATURE_CREATE_FILE) != 0;
            case PROP_CAN_CREATE_FOLDER:
                return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_CREATE_FOLDER) != 0;
            case PROP_CAN_CREATE_LINK:
                return (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_CREATE_FOLDER) != 0 && !resource.isLinked(IResource.CHECK_ANCESTORS);
            case PROP_CAN_SET_ACTIVE: {
                DBPProject activeProject = workspace.getActiveProject();
                return resource instanceof IProject && (activeProject == null || resource != activeProject.getEclipseProject());
            }
            case PROP_TYPE:
                final DBPResourceHandler resourceHandler = DBWorkbench.getPlatform().getWorkspace().getResourceHandler(resource);
                return resourceHandler != null && expectedValue.equals(resourceHandler.getTypeName(resource));
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}
