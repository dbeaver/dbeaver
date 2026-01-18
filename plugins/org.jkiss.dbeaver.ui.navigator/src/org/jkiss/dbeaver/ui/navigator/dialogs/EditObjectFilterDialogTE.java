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
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.UIUtils;

public class EditObjectFilterDialogTE extends EditObjectFilterDialog {
    public static final int DELETE_USER_FILTER = 1001;

    private Button customUserFilterCheckbox;

    protected EditObjectFilterDialogTE(
        Shell shell,
        DBPDataSourceRegistry dsRegistry,
        String objectTitle,
        DBSObjectFilter filter,
        boolean globalFilter
    ) {
        super(shell, dsRegistry, objectTitle, filter, globalFilter);
    }

    @NotNull
    @Override
    protected Composite createSfGroup(Composite composite) {
        Composite sfGroup = super.createSfGroup(composite);
        customUserFilterCheckbox = UIUtils.createCheckbox(sfGroup, "My custom checkbox", filter.isUserFilter());
        customUserFilterCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (customUserFilterCheckbox.getSelection()) {
                    filter.setUserFilter(true);
                } else {
                    currentUserFilterUnselected();
                }
            }
        });
        return sfGroup;
    }

    private void currentUserFilterUnselected() {
        if (UIUtils.confirmAction("sure bout that?", "it will remove your custom stuff")) {
            setReturnCode(DELETE_USER_FILTER);
            close();
        } else {
            customUserFilterCheckbox.setSelection(true);
        }
    }
}
