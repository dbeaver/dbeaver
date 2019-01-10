/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Status line contribution
 */
public class StatusLineContributionItemEx extends ContributionItem {

    private CLabel label;
    private String text = "";
    private String toolTip = "";

    public StatusLineContributionItemEx(String id) {
        super(id);
        setVisible(false); // no text to start with
    }

    @Override
    public void fill(Composite parent) {
        Composite statusLine = parent;

        Label sep = new Label(parent, SWT.SEPARATOR);
        label = new CLabel(statusLine, SWT.SHADOW_NONE);
        label.setText(text);
        if (toolTip != null) {
            label.setToolTipText(toolTip);
        }

        // compute the size of the label to get the width hint for the contribution
        Point preferredSize = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int widthHint = preferredSize.x;
        int heightHint = preferredSize.y;

        StatusLineLayoutData data = new StatusLineLayoutData();
        data.widthHint = widthHint;
        label.setLayoutData(data);

        data = new StatusLineLayoutData();
        data.heightHint = heightHint;
        sep.setLayoutData(data);
    }

    public void setText(String text) {
        Assert.isNotNull(text);

        this.text = LegacyActionTools.escapeMnemonics(text);

        if (label != null && !label.isDisposed()) {
            label.setText(this.text);
        }
        updateUI();
    }

    private void updateUI() {
        setVisible(true);
        IContributionManager contributionManager = getParent();

        if (contributionManager != null) {
            contributionManager.update(true);
        }
    }

    public void setToolTip(String text) {
        this.toolTip = text;

        if (label != null && !label.isDisposed()) {
            label.setToolTipText(toolTip);
        }
        updateUI();
    }

}
