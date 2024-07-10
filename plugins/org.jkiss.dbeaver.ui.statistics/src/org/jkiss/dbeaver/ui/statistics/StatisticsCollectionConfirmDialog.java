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
package org.jkiss.dbeaver.ui.statistics;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.statistics.StatisticCollectionMessages;

/**
 * StatisticsCollectionConfirmDialog
 */
public class StatisticsCollectionConfirmDialog extends BaseDialog {

    private Button doNotShareDataButton;

    public StatisticsCollectionConfirmDialog(Shell parentShell) {
        super(parentShell, StatisticCollectionMessages.statistic_collection_dialog_title, DBIcon.STATUS_INFO);
    }

    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        Composite composite = UIUtils.createComposite(dialogArea, 1);

        Label label = UIUtils.createLabel(composite, "Data share");
        label.setFont(JFaceResources.getFont(JFaceResources.HEADER_FONT));
        UIUtils.createHorizontalLine(composite);

        PrefPageUsageStatistics.createDataShareComposite(composite);

        if (!DBWorkbench.getPlatform().getApplication().isStatisticsCollectionRequired()) {
            UIUtils.createEmptyLabel(composite, 1, 1);
            UIUtils.createLink(
                composite,
                StatisticCollectionMessages.statistic_collection_pref_link,
                SelectionListener.widgetSelectedAdapter(selectionEvent -> {
                    Shell parentShell = getParentShell();
                    close();
                    UIUtils.showPreferencesFor(parentShell, null, PrefPageUsageStatistics.PAGE_ID);
                })
            );
        }

        UIUtils.createEmptyLabel(composite, 1, 1);
        doNotShareDataButton = UIUtils.createCheckbox(composite, StatisticCollectionMessages.statistic_collection_dont_share_lbl, false);

        if (DBWorkbench.getPlatform().getApplication().isStatisticsCollectionRequired()) {
            doNotShareDataButton.setEnabled(false);
            UIUtils.createInfoLabel(composite, "You cannot opt-out from data sharing in this version of DBeaver.");
        }

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.YES_ID, StatisticCollectionMessages.statistic_collection_confirm_lbl, true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (doNotShareDataButton.getSelection()) {
            // DO NOT SEND
            UIStatisticsActivator.setTrackingEnabled(buttonId == IDialogConstants.NO_ID);
        } else {
            // SEND
            UIStatisticsActivator.setTrackingEnabled(buttonId == IDialogConstants.YES_ID);
        }
        UIStatisticsActivator.setSkipDataShareConfirmation(true);
        close();

        super.buttonPressed(buttonId);
    }
}
