package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * Transfer settings
 */
public interface IDataTransferSettings {

    void loadSettings(IDialogSettings dialogSettings);

    void saveSettings(IDialogSettings dialogSettings);

}
