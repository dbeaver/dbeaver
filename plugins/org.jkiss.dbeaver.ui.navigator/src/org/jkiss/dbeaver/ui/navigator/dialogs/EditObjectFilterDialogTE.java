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

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
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

    private Button allUsersRadioButton;
    private Button currentUserRadioButton;

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

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialog = super.createDialogArea(parent);
        updateTemplatesEnabledState();
        return dialog;
    }

    @NotNull
    @Override
    protected Composite setTopPanel(@NotNull Composite composite) {
        Composite topPanel = getTopPanelPlaceholder(composite);
        setEnableCheckbox(topPanel);
        if (!globalFilter) {
            setGlobalFilterLink(topPanel);
        }
        setCustomUserFilterButtons(composite);
        return topPanel;
    }


    @NotNull
    @Override
    protected Composite getTopPanelPlaceholder(@NotNull Composite parent) {
        Composite topPanel = UIUtils.createPlaceholder(parent, globalFilter ? 3 : 4, 5);
        topPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return topPanel;
    }

    private void setCustomUserFilterButtons(@NotNull Composite parent) {
        Composite buttonsPlaceholder = UIUtils.createPlaceholder(
            parent,
            3,
            10
        );
        buttonsPlaceholder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createLabel(buttonsPlaceholder, "Filter for: ")
            .setToolTipText("My custom tooltip text");

        SelectionListener allUsersSelected = SelectionListener.widgetSelectedAdapter(e -> {
            if (allUsersRadioButton.getSelection()) {
                allUsersFilterSelected();
            }
        });
        allUsersRadioButton = UIUtils.createRadioButton(
            buttonsPlaceholder,
            "All users",
            !isCustomUserFilter(),
            allUsersSelected
        );
        allUsersRadioButton.setSelection(!isCustomUserFilter());


        SelectionListener currentUserSelected = SelectionListener.widgetSelectedAdapter(e -> {
            if (currentUserRadioButton.getSelection()) {
                currentUserFilterSelected();
            }
        });
        currentUserRadioButton = UIUtils.createRadioButton(
            buttonsPlaceholder,
            "Current user",
            isCustomUserFilter(),
            currentUserSelected
        );
        currentUserRadioButton.setSelection(isCustomUserFilter());

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

    private void allUsersFilterSelected() {
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
            allUsersRadioButton.setSelection(false);
            currentUserRadioButton.setSelection(true);
        }
    }

    private boolean isCustomUserFilter() {
        return filter.isUserFilter();
    }
}
