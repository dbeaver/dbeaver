/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;

import java.util.Map;
import java.util.Optional;

public class DataTransferConfigurationWizardDialog extends TaskConfigurationWizardDialog {

    public DataTransferConfigurationWizardDialog(
        @NotNull IWorkbenchWindow window,
        @NotNull TaskConfigurationWizard<?> wizard,
        @NotNull IStructuredSelection selection
    ) {
        super(window, wizard, selection, Map.of());
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID && !isLastPage()) {
            cycleToLastPage();
            return;
        }
        super.buttonPressed(buttonId);
    }

    public void cycleToLastPage() {
        while (canShowNext()) {
            nextPressed();
        }
    }

    private boolean canShowNext() {
        IWizard wizard = getWizard();
        IWizardPage currentPage = getCurrentPage();
        return currentPage != null
            && currentPage.isPageComplete()
            && wizard.getNextPage(currentPage) != null;
    }


    private boolean isLastPage() {
        IWizardPage lastPage = getLastPage();
        return lastPage != null && lastPage.equals(getCurrentPage());
    }

    @Nullable
    private IWizardPage getLastPage() {
        Optional<Object> data = Optional
            .ofNullable(getPagesTree())
            .map(t -> t.getItem(t.getItemCount() - 1))
            .map(Widget::getData);
        return data.isPresent() && data.get() instanceof IWizardPage
            ? (IWizardPage) data.get()
            : null;
    }
}
