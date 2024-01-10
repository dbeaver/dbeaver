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
package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.utils.CommonUtils;

public class TipOfTheDayInitializer implements IWorkbenchWindowInitializer {
    private static final String PROP_NOT_FIRST_RUN = "tipOfTheDayInitializer.notFirstRun";

    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        if (!isTipsEnabled() || window.getWorkbench().getWorkbenchWindowCount() > 1) {
            return;
        }
        // Show tips with delay to let UI initialize properly
        new UIJob(window.getShell().getDisplay(), "Show tip of the day") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                ShowTipOfTheDayHandler.showTipOfTheDay(window);
                return Status.OK_STATUS;
            }
        }.schedule(3000);

    }

    private static boolean isTipsEnabled() {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PROP_NOT_FIRST_RUN)) {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_NOT_FIRST_RUN, true);
            return false;
        }
        String tipsEnabledStr = DBWorkbench.getPlatform().getPreferenceStore().getString(ShowTipOfTheDayHandler.UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP);
        if (CommonUtils.isEmpty(tipsEnabledStr)) {
            return true;
        }
        return CommonUtils.toBoolean(tipsEnabledStr);
    }
}
