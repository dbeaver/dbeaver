/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;

/**
 * DBRProcessPropertyTester
 */
public class DBRProcessPropertyTester extends PropertyTester
{

    public static final String NAMESPACE = "org.jkiss.dbeaver.runtime.process";
    public static final String PROP_RUNNING = "running";

    public DBRProcessPropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBRProcessController)) {
            return false;
        }
        DBRProcessController controller = (DBRProcessController)receiver;
        if (property.equals(PROP_RUNNING)) {
            return controller.getProcessDescriptor() != null && controller.getProcessDescriptor().isRunning();
        }

        return false;
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}