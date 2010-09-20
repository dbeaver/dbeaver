/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * DatabaseEditorPropertyTester
 */
public class DataSourcePropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.datasource";
    public static final String PROP_CONNECTED = "connected";

    public DataSourcePropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBSDataSourceContainer)) {
            return false;
        }
        DBSDataSourceContainer dataSourceContainer = (DBSDataSourceContainer)receiver;
        if (property.equals(PROP_CONNECTED)) {
            return dataSourceContainer.isConnected() == Boolean.valueOf(String.valueOf(expectedValue));
        }
        return false;
    }

    public static void fireProperty—hange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}