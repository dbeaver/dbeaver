/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * DatabaseEditorPropertyTester
 */
public class DataSourcePropertyTester extends PropertyTester
{
    public static final String PROP_CONNECTED = "connected";

    public DataSourcePropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IDataSourceContainerProvider)) {
            return false;
        }
        IDataSourceContainerProvider containerProvider = (IDataSourceContainerProvider) receiver;
        DBSDataSourceContainer dataSourceContainer = containerProvider.getDataSourceContainer();
        if (property.equals(PROP_CONNECTED)) {
            return dataSourceContainer.isConnected() == Boolean.valueOf(String.valueOf(expectedValue));
        } else {
            return false;
        }
    }

}