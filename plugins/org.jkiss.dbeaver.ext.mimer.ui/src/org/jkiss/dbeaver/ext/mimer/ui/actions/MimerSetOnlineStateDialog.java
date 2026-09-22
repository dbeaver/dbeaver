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
package org.jkiss.dbeaver.ext.mimer.ui.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.List;

/**
 * Asks which online/offline state to set the selected Mimer SQL databank(s) or shadow(s) to.
 * The offered {@code states} are already filtered to what's valid for the current state (see
 * {@code MimerUtils.onlineStatesFor}); OK is disabled until one is picked (it defaults to the
 * first, so it's effectively always enabled).
 *
 * @author Mimer Information Technology
 */
public class MimerSetOnlineStateDialog extends BaseDialog {

    private final String targetLabel;
    private final List<String> names;
    private final String[] states;
    private Combo stateCombo;
    private String selectedState;

    public MimerSetOnlineStateDialog(
        @NotNull Shell parentShell,
        @NotNull String keyword,
        @NotNull List<String> names,
        @NotNull String[] states
    ) {
        super(parentShell, "Set " + capitalize(keyword) + " Online State", null);
        this.targetLabel = capitalize(keyword) + (names.size() > 1 ? "s" : "");
        this.names = names;
        this.states = states;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite area = super.createDialogArea(parent);
        Composite group = new Composite(area, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, targetLabel, String.join(", ", names), SWT.BORDER | SWT.READ_ONLY)
            .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        stateCombo = UIUtils.createLabelCombo(group, "New state", SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String state : states) {
            stateCombo.add(state);
        }
        stateCombo.select(0);

        return area;
    }

    @Override
    protected void okPressed() {
        selectedState = stateCombo.getText();
        super.okPressed();
    }

    @NotNull
    public String getSelectedState() {
        return selectedState;
    }

    @NotNull
    private static String capitalize(@NotNull String keyword) {
        return keyword.charAt(0) + keyword.substring(1).toLowerCase();
    }
}
