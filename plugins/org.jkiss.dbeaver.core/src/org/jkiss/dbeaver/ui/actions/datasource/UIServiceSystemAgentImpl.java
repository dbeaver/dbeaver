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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.ui.UIServiceSystemAgent;
import org.jkiss.dbeaver.ui.TrayIconHandler;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * UIServiceDriversImpl
 */
public class UIServiceSystemAgentImpl implements UIServiceSystemAgent {

    private TrayIconHandler trayItem;

    public UIServiceSystemAgentImpl() {
        this.trayItem = new TrayIconHandler();
    }

    @Override
    public long getLongOperationTimeout() {
        return DBWorkbench.getPlatform().getPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT);
    }

    @Override
    public void notifyAgent(String message, int status) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY)) {
            // Notifications disabled
            return;
        }
        if (TrayIconHandler.isSupported()) {
            UIUtils.syncExec(() -> Display.getCurrent().beep());
            trayItem.notify(message, status);
        } else {
            DBeaverNotifications.showNotification(
                "agent.notify",
                "Agent Notification",
                message,
                status == IStatus.INFO ? DBPMessageType.INFORMATION :
                    (status == IStatus.ERROR ? DBPMessageType.ERROR : DBPMessageType.WARNING),
                null);
        }
    }


}
