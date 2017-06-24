/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

public class DataTransferWizard extends Wizard implements IExportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "DataTransfer";//$NON-NLS-1$

    private DataTransferSettings settings;

    public DataTransferWizard(@Nullable IDataTransferProducer[] producers, @Nullable IDataTransferConsumer[] consumers) {
        this.settings = new DataTransferSettings(producers, consumers);
        loadSettings();
    }

    private void loadSettings()
    {
        IDialogSettings section = UIUtils.getDialogSettings(RS_EXPORT_WIZARD_DIALOG_SETTINGS);
        setDialogSettings(section);

        settings.loadFrom(DBeaverUI.getActiveWorkbenchWindow(), section);
    }

    public DataTransferSettings getSettings()
    {
        return settings;
    }

    public <T extends IDataTransferSettings> T getPageSettings(IWizardPage page, Class<T> type)
    {
        return type.cast(settings.getNodeSettings(page));
    }

    @Override
    public void addPages() {
        super.addPages();
        if (settings.isConsumerOptional()) {
            addPage(new DataTransferPagePipes());
        }
        settings.addWizardPages(this);
        addPage(new DataTransferPageFinal());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle(CoreMessages.data_transfer_wizard_name);
        setNeedsProgressMonitor(true);
    }

    @Nullable
    @Override
    public IWizardPage getNextPage(IWizardPage page)
    {
        IWizardPage[] pages = getPages();
        int curIndex = -1;
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] == page) {
                curIndex = i;
                break;
            }
        }
        if (curIndex == pages.length - 1) {
            return null;
        }
        if (curIndex != -1) {
            // Return first node config page
            for (int i = curIndex + 1; i < pages.length; i++) {
                if (settings.isPageValid(pages[i])) {
                    return pages[i];
                }
            }
        }
        // Final page
        return pages[pages.length - 1];
    }

    @Nullable
    @Override
    public IWizardPage getPreviousPage(IWizardPage page)
    {
        IWizardPage[] pages = getPages();
        int curIndex = -1;
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] == page) {
                curIndex = i;
                break;
            }
        }
        if (curIndex == 0) {
            return null;
        }
        if (curIndex != -1) {
            for (int i = curIndex - 1; i > 0; i--) {
                if (settings.isPageValid(pages[i])) {
                    return pages[i];
                }
            }
        }
        // First page
        return pages[0];
    }

    @Override
    public boolean canFinish()
    {
        for (IWizardPage page : getPages()) {
            if (settings.isPageValid(page) && !page.isPageComplete()) {
                return false;
            }
            if (page instanceof DataTransferPageFinal && !((DataTransferPageFinal) page).isActivated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean performCancel() {
        // Save settings anyway
        getSettings().saveTo(getDialogSettings());

        return super.performCancel();
    }

    @Override
    public boolean performFinish() {
        // Save settings
        getSettings().saveTo(getDialogSettings());

        // Start consumers
        try {
            DBeaverUI.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        for (DataTransferPipe pipe : settings.getDataPipes()) {
                            pipe.getConsumer().startTransfer(monitor);
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError("Transfer init failed", "Can't start data transfer", e.getTargetException());
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        // Run export jobs
        executeJobs();

        // Done
        return true;
    }

    private void executeJobs() {
        // Schedule jobs for data providers
        int totalJobs = settings.getDataPipes().size();
        if (totalJobs > settings.getMaxJobCount()) {
            totalJobs = settings.getMaxJobCount();
        }
        for (int i = 0; i < totalJobs; i++) {
            new DataTransferJob(settings).schedule();
        }
    }

}