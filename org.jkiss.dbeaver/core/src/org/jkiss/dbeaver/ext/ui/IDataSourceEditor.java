package org.jkiss.dbeaver.ext.ui;

import org.eclipse.jface.dialogs.IDialogPage;

/**
 * IDataSourceEditor
 */
public interface IDataSourceEditor extends IDialogPage
{
    void setSite(IDataSourceEditorSite site);

    boolean isComplete();

    void loadSettings();

    void saveSettings();

}
