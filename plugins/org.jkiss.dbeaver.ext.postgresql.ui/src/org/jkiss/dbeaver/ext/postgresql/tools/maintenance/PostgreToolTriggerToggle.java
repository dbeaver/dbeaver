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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

import java.util.Collection;


public abstract class PostgreToolTriggerToggle implements IUserInterfaceTool {

    private boolean isEnable;

    PostgreToolTriggerToggle(boolean enable) {
        this.isEnable = enable;
    }

    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) {
        if (isEnable) {
            TaskConfigurationWizardDialog.openNewTaskDialog(
                    window,
                    NavigatorUtils.getSelectedProject(),
                    "pgToolTriggerEnable",
                    new StructuredSelection(objects.toArray()));
        } else {
            TaskConfigurationWizardDialog.openNewTaskDialog(
                    window,
                    NavigatorUtils.getSelectedProject(),
                    "pgToolTriggerDisable",
                    new StructuredSelection(objects.toArray()));
            }
    }


    /*        @Override
        protected boolean needsRefreshOnFinish() {
            return true;
        }
    }*/

}
