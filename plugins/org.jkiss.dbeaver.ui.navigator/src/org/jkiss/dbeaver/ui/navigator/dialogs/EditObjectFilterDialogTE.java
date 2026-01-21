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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

public class EditObjectFilterDialogTE extends EditObjectFilterDialog {
    public static final int DELETE_USER_FILTER = 1001;

    private Combo customUserFilterCombo;

    private boolean isUserFilterUnsaved;

    protected EditObjectFilterDialogTE(
        @NotNull Shell shell,
        @NotNull DBPDataSourceRegistry dsRegistry,
        @NotNull String objectTitle,
        @Nullable DBSObjectFilter filter,
        boolean globalFilter
    ) {
        super(shell, dsRegistry, objectTitle, filter, globalFilter);
    }

    @NotNull
    @Override
    protected Composite setTopPanel(@NotNull Composite composite) {
        Composite topPanel = getTopPanelPlaceholder(composite);
        setEnableCheckbox(topPanel);
        setCustomUserFilterCombo(topPanel);
        if (!globalFilter) {
            setGlobalFilterLink(topPanel);
        }
        return topPanel;
    }


    @NotNull
    @Override
    protected Composite getTopPanelPlaceholder(@NotNull Composite composite) {
        Composite topPanel = UIUtils.createPlaceholder(composite, globalFilter ? 3 : 4, 5);
        topPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return topPanel;
    }

    private void setCustomUserFilterCombo(@NotNull Composite parent) {
        customUserFilterCombo = UIUtils.createLabelCombo(
            parent,
            "Filter for",
            "Set this filter for all DBeaver users, or only for current one",
            SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER
        );
        customUserFilterCombo.add("All users");
        customUserFilterCombo.add("Current user");
        customUserFilterCombo.select(0);
        GridData comboGD = new GridData(SWT.FILL, SWT.CENTER, false, false);
        customUserFilterCombo.setLayoutData(comboGD);
    }

    @Override
    protected boolean shouldSaveFilterInRegistry() {
        return !isCustomUserFilter() && super.shouldSaveFilterInRegistry();
    }

    private void updateTemplatesEnabledState() {
        boolean isTemplatesEnabled = !isCustomUserFilter();
        saveButton.setEnabled(isTemplatesEnabled);
        removeButton.setEnabled(isTemplatesEnabled);
        namesCombo.setEnabled(isTemplatesEnabled);
    }

    private void currentUserFilterSelected() {
        filter.setUserFilter(true);
        isUserFilterUnsaved = true;
        updateTemplatesEnabledState();
    }

    private void currentUserFilterUnselected() {
        if (isUserFilterUnsaved) {
            filter.setUserFilter(false);
            updateTemplatesEnabledState();
        } else if (UIUtils.confirmAction(
            UINavigatorMessages.dialog_filter_remove_custom_user_filter_title,
            UINavigatorMessages.dialog_filter_remove_custom_user_filter_question
        )) {
            setReturnCode(DELETE_USER_FILTER);
            close();
        } else {
            // customUserFilterCombo.setSelection(true);
        }
    }

    private boolean isCustomUserFilter() {
        return filter.isUserFilter();
    }
}
