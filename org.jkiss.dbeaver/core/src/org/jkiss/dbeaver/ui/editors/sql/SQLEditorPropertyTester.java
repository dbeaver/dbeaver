/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;

/**
 * DatabaseEditorPropertyTester
 */
public class SQLEditorPropertyTester extends PropertyTester
{
    static final Log log = LogFactory.getLog(SQLEditorPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.sql";
    public static final String PROP_CAN_EXECUTE = "canExecute";
    public static final String PROP_CAN_EXPLAIN = "canExplain";

    public SQLEditorPropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof SQLEditor)) {
            return false;
        }
        SQLEditor editor = (SQLEditor)receiver;
        if (property.equals(PROP_CAN_EXECUTE)) {
            return editor.getDataSourceContainer().isConnected();
        } if (property.equals(PROP_CAN_EXPLAIN)) {
            return editor.getDataSource() instanceof DBCQueryPlanner;
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}