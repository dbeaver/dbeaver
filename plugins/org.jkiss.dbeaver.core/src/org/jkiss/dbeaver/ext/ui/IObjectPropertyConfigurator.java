/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.swt.widgets.Composite;

/**
 * IDataSourceConnectionEditor
 */
public interface IObjectPropertyConfigurator<T extends IObjectPropertyConfiguration>
{
    void createControl(Composite parent);

    void loadSettings(T configuration);

    void saveSettings(T configuration);

    boolean isComplete();

}
