/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tools.fdw;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;


public class PostgreFDWConfigToolCommandHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) {
        for (DBSObject object : NavigatorUtils.getSelectedObjects(HandlerUtil.getCurrentSelection(event))) {
            PostgreDatabase database;
            if (object instanceof PostgreObject) {
                database = ((PostgreObject) object).getDatabase();
            } else {
                continue;
            }
            ActiveWizardDialog dialog = new ActiveWizardDialog(
                HandlerUtil.getActiveWorkbenchWindow(event),
                new PostgreFDWConfigWizard(database));
            dialog.setFinishButtonLabel("Install");
            dialog.open();
        }
        return null;
    }
}
