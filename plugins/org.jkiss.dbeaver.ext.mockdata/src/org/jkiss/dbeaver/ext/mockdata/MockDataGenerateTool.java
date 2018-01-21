/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.ToolWizardDialog;

import java.util.ArrayList;
import java.util.Collection;

public class MockDataGenerateTool implements IExternalTool {

    private MockDataSettings mockDataSettings = new MockDataSettings();

    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException {

        ArrayList<DBSDataManipulator> dbObjects = new ArrayList<>();
        for (DBSObject obj : objects) {
            dbObjects.add((DBSDataManipulator) obj);
        }

        MockDataExecuteWizard wizard = new MockDataExecuteWizard(
                mockDataSettings, dbObjects, MockDataMessages.tools_mockdata_wizard_page_name);
        ToolWizardDialog dialog = new ToolWizardDialog(
                window,
                wizard) {

            private boolean removeOldDataConfirmed = false;

            @Override
            protected void finishPressed() {
                if (doRemoveDataConfirmation()) {
                    return;
                }
                super.finishPressed();
            }

            @Override
            protected void nextPressed() {
                IWizardPage currentPage = getCurrentPage();
                if (currentPage instanceof MockDataWizardPageSettings) {
                    if (doRemoveDataConfirmation()) {
                        return;
                    }
                }
                super.nextPressed();
            }

            private boolean doRemoveDataConfirmation() {
                if (mockDataSettings.isRemoveOldData() && !removeOldDataConfirmed) {
                    if (UIUtils.confirmAction(getShell(), MockDataMessages.tools_mockdata_wizard_title, MockDataMessages.tools_mockdata_confirm_delete_old_data_message)) {
                        removeOldDataConfirmed = true;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        };
        dialog.open();
    }
}
