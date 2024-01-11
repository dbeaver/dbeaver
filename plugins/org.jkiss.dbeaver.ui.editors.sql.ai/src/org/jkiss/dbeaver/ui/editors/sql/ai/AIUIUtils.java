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
package org.jkiss.dbeaver.ui.editors.sql.ai;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.ai.internal.AIUIMessages;

public class AIUIUtils {
    private AIUIUtils() {
        // prevents instantiation
    }

    public static boolean confirmMetaTransfer(@NotNull DAICompletionSettings settings, @NotNull DBPDataSourceContainer container) {
        if (settings.isMetaTransferConfirmed()) {
            return true;
        }

        if (UIUtils.confirmAction(UIUtils.getActiveWorkbenchShell(),
            AIUIMessages.confirm_meta_transfer_usage_title,
            NLS.bind(AIUIMessages.confirm_meta_transfer_usage_message, container.getName()),
            DBIcon.AI
        )) {
            settings.setMetaTransferConfirmed(true);
            settings.saveSettings();
            return true;
        }

        return false;
    }
}
