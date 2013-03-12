package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;

/**
 * Transfer settings
 */
public interface IDataTransferSettings {

    void loadSettings(IRunnableContext runnableContext, IDialogSettings dialogSettings);

    void saveSettings(IDialogSettings dialogSettings);

}
