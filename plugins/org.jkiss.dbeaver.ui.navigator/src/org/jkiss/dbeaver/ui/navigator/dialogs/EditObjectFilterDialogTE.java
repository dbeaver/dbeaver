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

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.UIUtils;

public class EditObjectFilterDialogTE extends EditObjectFilterDialog {
    public static final int DELETE_USER_FILTER = 1001;

    private Button customUserFilterCheckbox;

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
    protected void setSfGroup(@NotNull Composite composite) {
        super.setSfGroup(composite);
        customUserFilterCheckbox = UIUtils.createCheckbox(sfGroup, "My custom checkbox", filter.isUserFilter());
        customUserFilterCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (customUserFilterCheckbox.getSelection()) {
                    currentUserFilterSelected();
                } else {
                    currentUserFilterUnselected();
                }
            }
        });
        updateTemplatesEnabledState();
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
        } else if (UIUtils.confirmAction("sure bout that?", "it will remove your custom stuff")) {
            setReturnCode(DELETE_USER_FILTER);
            close();
        } else {
            customUserFilterCheckbox.setSelection(true);
        }
    }

    private boolean isCustomUserFilter() {
        return filter.isUserFilter();
    }
}
