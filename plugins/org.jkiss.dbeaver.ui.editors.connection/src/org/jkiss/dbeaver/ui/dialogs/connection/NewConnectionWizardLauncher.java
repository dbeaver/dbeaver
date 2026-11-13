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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Launches the connection wizard in its own dialog, which provides the Test Connection button.
 */
public class NewConnectionWizardLauncher extends Wizard implements INewWizard {
    private IWorkbenchWindow window;
    private IStructuredSelection selection = StructuredSelection.EMPTY;

    @Override
    public void init(@NotNull IWorkbench workbench, @NotNull IStructuredSelection selection) {
        this.window = workbench.getActiveWorkbenchWindow();
        this.selection = selection;
    }

    @Override
    public boolean performFinish() {
        var window = this.window;
        var selection = this.selection;
        if (window != null) {
            UIUtils.asyncExec(() -> NewConnectionDialog.openNewConnectionDialog(window, selection));
            return true;
        }
        return false;
    }
}
