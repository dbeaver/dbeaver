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
package org.jkiss.dbeaver.team.git.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.egit.ui.internal.clone.GitImportWizard;
import org.eclipse.egit.ui.internal.clone.GitSelectWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;

public class ProjectCloneHandler extends AbstractHandler {

    private static final Log log = Log.getLog(ProjectCloneHandler.class);

    @Override
	public Object execute(ExecutionEvent event) {
        try {
            // FIXME: this is a EGIT hack
            // Set new project default option (Create general project. As Create New Project is broken)
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=324145
            org.eclipse.egit.ui.Activator.getDefault().getDialogSettings().put(
                    "org.eclipse.egit.ui.internal.clone.GitSelectWizardPageWizardSel",
                    GitSelectWizardPage.GENERAL_WIZARD);
        } catch (Throwable e) {
            log.debug(e);
        }

        IWorkbench workbench = HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench();
        GitImportWizard wizard = new GitImportWizard();
        wizard.init(workbench, HandlerUtil.getCurrentStructuredSelection(event));

        WizardDialog wizardDialog = new WizardDialog(
            HandlerUtil.getActiveShell(event), wizard);
        wizardDialog.setHelpAvailable(false);
        wizardDialog.open();

	    return null;
		
	}
}
