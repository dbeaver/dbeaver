/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

/**
 * This page hosts other settings pages
 */
class DataTransferPageSettings extends ActiveWizardPage<DataTransferWizard> {

    private IWizardPage producerSettingsPage;
    private IWizardPage consumerSettingsPage;

    DataTransferPageSettings() {
        super(DTMessages.data_transfer_wizard_settings_name);
        setTitle(DTMessages.data_transfer_wizard_settings_title);
        setDescription(DTMessages.data_transfer_wizard_settings_description);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        setControl(composite);
    }

    private void createSettingsPages(Composite composite) {
        DataTransferSettings dtSettings = getWizard().getSettings();

        {
            DataTransferPipe dataPipe = dtSettings.getDataPipes().get(0);

            StringBuilder title = new StringBuilder();
            title.append(DTMessages.data_transfer_wizard_settings_title).append(" (").append(dtSettings.getProducer().getName()).append(" to ").append(dtSettings.getConsumer().getName());
            if (dtSettings.getProcessor() != null) {
                title.append(", ").append(dtSettings.getProcessor().getName());
            }
            title.append(")");
            setTitle(title.toString());


            producerSettingsPage = getSettingsPage(dataPipe.getProducer());
            consumerSettingsPage = getSettingsPage(dataPipe.getConsumer());
        }
        Composite settingsComposite = composite;

        if (producerSettingsPage != null && consumerSettingsPage != null) {
            SashForm sash = new SashForm(composite, SWT.HORIZONTAL);
            sash.setLayoutData(new GridData(GridData.FILL_BOTH));

            settingsComposite = sash;
        }

        if (producerSettingsPage != null) {
            producerSettingsPage.setWizard(getWizard());
            Composite producerGroup = UIUtils.createPlaceholder(settingsComposite, 1);
            UIUtils.createInfoLabel(producerGroup, producerSettingsPage.getTitle());
            producerSettingsPage.createControl(producerGroup);
            if (producerSettingsPage instanceof ActiveWizardPage) {
                ((ActiveWizardPage) producerSettingsPage).activatePage();
            }
        }

        if (consumerSettingsPage != null) {
            consumerSettingsPage.setWizard(getWizard());
            Composite consumerGroup = UIUtils.createPlaceholder(settingsComposite, 1);
            UIUtils.createInfoLabel(consumerGroup, consumerSettingsPage.getTitle());
            consumerSettingsPage.createControl(consumerGroup);
            if (consumerSettingsPage instanceof ActiveWizardPage) {
                ((ActiveWizardPage) consumerSettingsPage).activatePage();
            }
        }
    }

    private IWizardPage getSettingsPage(IDataTransferNode node) {
        DataTransferWizard.NodePageSettings producerInfo = getWizard().getNodeInfo(node);
        return producerInfo == null ? null : producerInfo.settingsPage;
    }

    @Override
    public void activatePage()
    {
        Composite composite = (Composite) getControl();
        UIUtils.disposeChildControls(composite);

        createSettingsPages(composite);

        composite.layout(true, true);

        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
        if (producerSettingsPage instanceof ActiveWizardPage) {
            ((ActiveWizardPage) producerSettingsPage).deactivatePage();
        }
        if (consumerSettingsPage instanceof ActiveWizardPage) {
            ((ActiveWizardPage) consumerSettingsPage).deactivatePage();
        }
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return (producerSettingsPage == null || producerSettingsPage.isPageComplete()) &&
            (consumerSettingsPage == null || consumerSettingsPage.isPageComplete());
    }

}