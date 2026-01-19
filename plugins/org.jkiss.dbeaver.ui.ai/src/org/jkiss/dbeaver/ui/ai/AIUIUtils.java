/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIContextSettings;
import org.jkiss.dbeaver.model.ai.AIIcons;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.preferences.AIPreferencePageMain;


public class AIUIUtils {
    private static final Log log = Log.getLog(AIUIUtils.class);

    private AIUIUtils() {
        // prevents instantiation
    }

    public static boolean confirmMetaTransfer(@NotNull AIContextSettings settings) {
        if (settings.isMetaTransferConfirmed()) {
            return true;
        }

        if (UIUtils.confirmAction(UIUtils.getActiveWorkbenchShell(),
            AIUIMessages.confirm_meta_transfer_usage_title,
            NLS.bind(AIUIMessages.confirm_meta_transfer_usage_message, settings.getDataSourceContainer().getName()),
            AIIcons.AI
        )) {
            settings.setMetaTransferConfirmed(true);
            try {
                settings.saveSettings();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(AIUIMessages.confirm_meta_transfer_usage_title, null, e);
            }
            return true;
        }

        return false;
    }

    public static void showPreferences(@NotNull Shell shell) {
        UIUtils.showPreferencesFor(
            shell,
            AISettingsManager.getInstance().getSettings(),
            AIPreferencePageMain.PAGE_ID
        );
    }
}
