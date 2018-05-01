/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.ui.DatabaseLaunchShortcut;
import org.jkiss.dbeaver.debug.ui.DatabaseScriptDialog;
import org.jkiss.dbeaver.ext.postgresql.debug.core.PostgreSqlDebugCore;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

public class PgSqlLocalLaunchShortcut extends DatabaseLaunchShortcut {

    public PgSqlLocalLaunchShortcut() {
        super(PostgreSqlDebugCore.CONFIGURATION_TYPE, PostgreDebugUIMessages.PgSqlLaunchShortcut_name);
    }

    @Override
    protected ILaunchConfiguration createConfiguration(DBSObject launchable) throws CoreException {
        ILaunchConfigurationWorkingCopy workingCopy = PostgreSqlDebugCore.createConfiguration(launchable);
        workingCopy.setAttribute(DBGConstants.ATTR_ATTACH_KIND, DBGController.ATTACH_KIND_LOCAL);
        IWorkbenchPartSite site = getWorkbenchPartSite();
        String script = workingCopy.getAttribute(DBGConstants.ATTR_SCRIPT_TEXT, "");
        String inputName = "Script";
        DatabaseScriptDialog dialog = new DatabaseScriptDialog(getShell(), site, inputName, script, launchable);
        dialog.create();
        
        dialog.setTitle("Specify script to be executed");
        dialog.setMessage("Specify script to be executed to start debug.");
        int open = dialog.open();
        if (IDialogConstants.CANCEL_ID == open) {
            return null;
        }
        String modified = dialog.getScriptTextValue();
        workingCopy.setAttribute(DBGConstants.ATTR_SCRIPT_TEXT, modified);
        return workingCopy.doSave();
    }

    @Override
    protected boolean isCandidate(ILaunchConfiguration config, DBSObject launchable, Map<String, Object> databaseContext) {
        try {
            String kind = config.getAttribute(DBGConstants.ATTR_ATTACH_KIND, (String)null);
            if (!DBGController.ATTACH_KIND_LOCAL.equals(kind)) {
                return false;
            }
        } catch (CoreException e) {
            return false;
        }
        return super.isCandidate(config, launchable, databaseContext);
    }

}
