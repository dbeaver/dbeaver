/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.ui.eula;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;

public class EULAInitializer implements IWorkbenchWindowInitializer {
    private static final String EULA_ALREADY_CONFIRMED = "eula.confirmed";

    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        if (!DBWorkbench.getPlatform().getApplication().isStandalone() || !isEulaDialogNeeded() || window.getWorkbench().getWorkbenchWindowCount() > 1) {
            return;
        }
        EULAHandler.showEula(window, true);
    }

    private static boolean isEulaDialogNeeded() {
        return !DBWorkbench.getPlatform().getPreferenceStore().getBoolean(EULA_ALREADY_CONFIRMED);
    }

}

