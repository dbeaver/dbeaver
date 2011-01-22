/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.registry.ProjectRegistry;

/**
 * GlobalPropertyTester
 */
public class GlobalPropertyTester extends PropertyTester
{
    //static final Log log = LogFactory.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.global";
    public static final String PROP_HAS_MULTI_PROJECTS = "hasMultipleProjects";

    public GlobalPropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (property.equals(PROP_HAS_MULTI_PROJECTS)) {
            return DBeaverCore.getInstance().getLiveProjects().size() > 1;
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}
