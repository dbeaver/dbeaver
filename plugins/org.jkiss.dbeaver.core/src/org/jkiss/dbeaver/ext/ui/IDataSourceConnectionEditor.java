/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.jface.dialogs.IDialogPage;

/**
 * IDataSourceConnectionEditor
 */
public interface IDataSourceConnectionEditor extends IDialogPage
{
    void setSite(IDataSourceConnectionEditorSite site);

    boolean isComplete();

    void loadSettings();

    void saveSettings();

}
