package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.jface.wizard.IWizardPage;

/**
 * Data transfer node (producer or consumer)
 */
public interface IDataTransferNode<SETTINGS extends IDataTransferSettings> {

    /**
     * Creates shared settings.
     * @return settings
     */
    SETTINGS createSettings();

    IWizardPage[] createWizardPages();

}
