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
package org.jkiss.dbeaver.ui.statistics;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * StatisticsCollectionConfirmDialog
 */
public class StatisticsCollectionConfirmDialog extends BaseDialog {

    public StatisticsCollectionConfirmDialog(Shell parentShell) {
        super(parentShell, "Statistics collection", DBIcon.STATUS_INFO);
    }

    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        Composite composite = UIUtils.createComposite(dialogArea, 1);

        Label label = UIUtils.createLabel(composite, "Data share");
        label.setFont(JFaceResources.getFont(JFaceResources.HEADER_FONT));
        UIUtils.createHorizontalLine(composite);

        PrefPageUsageStatistics.createDataShareComposite(composite);

        UIUtils.createEmptyLabel(composite, 1, 1);
        UIUtils.createLink(composite,
            "You can always change this behavior in <a>preferences</a>",
            SelectionListener.widgetSelectedAdapter(selectionEvent -> {
                Shell parentShell = getParentShell();
                close();
                UIUtils.showPreferencesFor(
                    parentShell,
                        null,
                        PrefPageUsageStatistics.PAGE_ID);
                }));

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.YES_ID, "Send anonymous statistics", true);
        createButton(parent, IDialogConstants.NO_ID, "Do not send", false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        UIStatisticsActivator.setSkipDataShareConfirmation(true);
        UIStatisticsActivator.setTrackingEnabled(buttonId == IDialogConstants.YES_ID);
        close();

        super.buttonPressed(buttonId);
    }
}
