/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.eula;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;

import java.util.prefs.Preferences;

public class EULAInitializer implements IWorkbenchWindowInitializer {
    public static final String DBEAVER_EULA = "DBeaver.eula";

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindow window) {
        if (!DBWorkbench.getPlatform().getApplication().isStandalone() || !isEulaDialogNeeded() || window.getWorkbench().getWorkbenchWindowCount() > 1) {
            return;
        }
        EULAUtils.showEula(window.getShell(), true);
    }

    private boolean isEulaDialogNeeded() {
        Preferences preferences = Preferences.userNodeForPackage(DBWorkbench.getPlatform().getApplication().getClass());
        if (preferences.get(DBEAVER_EULA, null) == null || !preferences.get(DBEAVER_EULA, null).equals(EULAUtils.getEulaVersion())) {
            //Used didn't accept EULA before or dialog was shown on different eula version
            preferences.remove(DBEAVER_EULA);
            return true;
        }
        return false;
    }

}

