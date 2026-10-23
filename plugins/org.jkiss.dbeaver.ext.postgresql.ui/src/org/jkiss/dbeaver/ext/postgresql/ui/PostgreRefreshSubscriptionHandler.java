/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.ArrayList;
import java.util.List;

public class PostgreRefreshSubscriptionHandler extends AbstractHandler {
    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection == null) {
            return null;
        }
        List<PostgreSubscription> subscriptions = new ArrayList<>();
        for (DBSObject object : NavigatorUtils.getSelectedObjects(selection)) {
            if (object instanceof PostgreSubscription subscription && subscription.isPersisted()) {
                subscriptions.add(subscription);
            }
        }
        if (subscriptions.isEmpty()) {
            return null;
        }
        MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(
            HandlerUtil.getActiveShell(event), PostgreMessages.dialog_refresh_subscription_title,
            PostgreMessages.dialog_refresh_subscription_description, PostgreMessages.dialog_refresh_subscription_copy_data,
            true, null, null);
        if (dialog.getReturnCode() != IDialogConstants.YES_ID) {
            return null;
        }
        boolean copyData = dialog.getToggleState();
        try {
            UIUtils.runWithDialog(monitor -> {
                for (PostgreSubscription subscription : subscriptions) {
                    if (monitor.isCanceled()) {
                        return false;
                    }
                    subscription.refreshPublications(monitor, copyData);
                }
                return true;
            });
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(PostgreMessages.dialog_refresh_subscription_title, null, e);
        }
        return null;
    }
}
