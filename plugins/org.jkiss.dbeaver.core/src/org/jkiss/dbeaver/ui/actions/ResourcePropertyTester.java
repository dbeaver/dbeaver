/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.ScriptsHandlerImpl;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.registry.ProjectRegistry;

/**
 * ResourcePropertyTester
 */
public class ResourcePropertyTester extends PropertyTester
{
    //static final Log log = LogFactory.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.resource";
    public static final String PROP_CAN_OPEN = "canOpen";
    public static final String PROP_CAN_CREATE_FOLDER = "canCreateFolder";
    public static final String PROP_CAN_CREATE_SCRIPT = "canCreateScript";
    public static final String PROP_CAN_SET_ACTIVE = "canSetActive";
    public static final String PROP_CAN_DELETE = "canDelete";

    public ResourcePropertyTester() {
        super();
    }

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
        } else if (property.equals(PROP_CAN_SET_ACTIVE)) {
            return resource instanceof IProject && resource != projectRegistry.getActiveProject();
        } else if (property.equals(PROP_CAN_CREATE_SCRIPT)) {
            return resource instanceof IFolder && handler instanceof ScriptsHandlerImpl;
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}
